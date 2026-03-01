/**
 * Self-rolled JWT auth for Cloudflare Workers.
 * Uses Web Crypto API (built into Workers runtime).
 */

export interface Env {
  DB: D1Database;
  SESSIONS: KVNamespace;
  JWT_SECRET: string;
}

interface TokenPayload {
  sub: string; // user id
  email: string;
  exp: number;
  iat: number;
}

// Simple base64url encoding/decoding
function base64urlEncode(data: Uint8Array): string {
  return btoa(String.fromCharCode(...data))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function base64urlDecode(str: string): Uint8Array {
  str = str.replace(/-/g, "+").replace(/_/g, "/");
  while (str.length % 4) str += "=";
  const binary = atob(str);
  return Uint8Array.from(binary, (c) => c.charCodeAt(0));
}

async function getSigningKey(secret: string): Promise<CryptoKey> {
  const encoder = new TextEncoder();
  return crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"]
  );
}

export async function signJwt(
  payload: TokenPayload,
  secret: string
): Promise<string> {
  const header = { alg: "HS256", typ: "JWT" };
  const encoder = new TextEncoder();

  const headerB64 = base64urlEncode(encoder.encode(JSON.stringify(header)));
  const payloadB64 = base64urlEncode(encoder.encode(JSON.stringify(payload)));
  const data = `${headerB64}.${payloadB64}`;

  const key = await getSigningKey(secret);
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    encoder.encode(data)
  );

  return `${data}.${base64urlEncode(new Uint8Array(signature))}`;
}

export async function verifyJwt(
  token: string,
  secret: string
): Promise<TokenPayload | null> {
  const parts = token.split(".");
  if (parts.length !== 3) return null;

  const [headerB64, payloadB64, signatureB64] = parts;
  const data = `${headerB64}.${payloadB64}`;
  const encoder = new TextEncoder();

  const key = await getSigningKey(secret);
  const signature = base64urlDecode(signatureB64);

  const valid = await crypto.subtle.verify(
    "HMAC",
    key,
    signature,
    encoder.encode(data)
  );

  if (!valid) return null;

  const payload: TokenPayload = JSON.parse(
    new TextDecoder().decode(base64urlDecode(payloadB64))
  );

  // Check expiration
  if (payload.exp < Math.floor(Date.now() / 1000)) return null;

  return payload;
}

// Password hashing using PBKDF2 (available in Workers runtime)
async function hashPassword(password: string): Promise<string> {
  const encoder = new TextEncoder();
  const salt = crypto.getRandomValues(new Uint8Array(16));

  const keyMaterial = await crypto.subtle.importKey(
    "raw",
    encoder.encode(password),
    "PBKDF2",
    false,
    ["deriveBits"]
  );

  const derivedBits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations: 100000, hash: "SHA-256" },
    keyMaterial,
    256
  );

  const hash = new Uint8Array(derivedBits);
  return `${base64urlEncode(salt)}.${base64urlEncode(hash)}`;
}

async function verifyPassword(
  password: string,
  stored: string
): Promise<boolean> {
  const [saltB64, hashB64] = stored.split(".");
  const salt = base64urlDecode(saltB64);
  const storedHash = base64urlDecode(hashB64);

  const encoder = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    "raw",
    encoder.encode(password),
    "PBKDF2",
    false,
    ["deriveBits"]
  );

  const derivedBits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations: 100000, hash: "SHA-256" },
    keyMaterial,
    256
  );

  const computed = new Uint8Array(derivedBits);
  if (computed.length !== storedHash.length) return false;
  let equal = true;
  for (let i = 0; i < computed.length; i++) {
    if (computed[i] !== storedHash[i]) equal = false;
  }
  return equal;
}

function generateId(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  const hex = Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function generateReferralCode(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(6));
  return Array.from(bytes)
    .map((b) => b.toString(36))
    .join("")
    .toUpperCase()
    .slice(0, 8);
}

async function createTokens(
  userId: string,
  email: string,
  secret: string
): Promise<{ access_token: string; refresh_token: string }> {
  const now = Math.floor(Date.now() / 1000);

  const accessToken = await signJwt(
    { sub: userId, email, iat: now, exp: now + 3600 }, // 1 hour
    secret
  );

  const refreshToken = await signJwt(
    { sub: userId, email, iat: now, exp: now + 604800 }, // 7 days
    secret
  );

  return { access_token: accessToken, refresh_token: refreshToken };
}

export async function handleSignUp(
  request: Request,
  env: Env
): Promise<Response> {
  const { email, password } = (await request.json()) as {
    email: string;
    password: string;
  };

  if (!email || !password || password.length < 6) {
    return jsonResponse(
      { error: "Email and password (6+ chars) required" },
      400
    );
  }

  // Check if user exists
  const existing = await env.DB.prepare(
    "SELECT id FROM users WHERE email = ?"
  )
    .bind(email.toLowerCase())
    .first();

  if (existing) {
    return jsonResponse({ error: "User already exists" }, 409);
  }

  const userId = generateId();
  const passwordHash = await hashPassword(password);
  const referralCode = generateReferralCode();

  await env.DB.prepare(
    `INSERT INTO users (id, email, password_hash, referral_code, credit_balance)
     VALUES (?, ?, ?, ?, 50)`
  )
    .bind(userId, email.toLowerCase(), passwordHash, referralCode)
    .run();

  // Log welcome bonus
  await env.DB.prepare(
    `INSERT INTO credit_transactions (user_id, type, amount, balance_after, description)
     VALUES (?, 'bonus', 50, 50, 'Welcome bonus')`
  )
    .bind(userId)
    .run();

  const tokens = await createTokens(userId, email, env.JWT_SECRET);

  return jsonResponse({
    ...tokens,
    user: { id: userId, email: email.toLowerCase(), display_name: null },
  });
}

export async function handleSignIn(
  request: Request,
  env: Env
): Promise<Response> {
  const { email, password } = (await request.json()) as {
    email: string;
    password: string;
  };

  if (!email || !password) {
    return jsonResponse({ error: "Email and password required" }, 400);
  }

  const user = await env.DB.prepare(
    "SELECT id, email, password_hash, display_name FROM users WHERE email = ?"
  )
    .bind(email.toLowerCase())
    .first<{
      id: string;
      email: string;
      password_hash: string;
      display_name: string | null;
    }>();

  if (!user) {
    return jsonResponse({ error: "Invalid credentials" }, 401);
  }

  const valid = await verifyPassword(password, user.password_hash);
  if (!valid) {
    return jsonResponse({ error: "Invalid credentials" }, 401);
  }

  const tokens = await createTokens(user.id, user.email, env.JWT_SECRET);

  return jsonResponse({
    ...tokens,
    user: {
      id: user.id,
      email: user.email,
      display_name: user.display_name,
    },
  });
}

export async function handleRefresh(
  request: Request,
  env: Env
): Promise<Response> {
  const { refresh_token } = (await request.json()) as {
    refresh_token: string;
  };

  const payload = await verifyJwt(refresh_token, env.JWT_SECRET);
  if (!payload) {
    return jsonResponse({ error: "Invalid or expired refresh token" }, 401);
  }

  const user = await env.DB.prepare(
    "SELECT id, email, display_name FROM users WHERE id = ?"
  )
    .bind(payload.sub)
    .first<{ id: string; email: string; display_name: string | null }>();

  if (!user) {
    return jsonResponse({ error: "User not found" }, 404);
  }

  const tokens = await createTokens(user.id, user.email, env.JWT_SECRET);

  return jsonResponse({
    ...tokens,
    user: {
      id: user.id,
      email: user.email,
      display_name: user.display_name,
    },
  });
}

export async function authenticateRequest(
  request: Request,
  env: Env
): Promise<TokenPayload | null> {
  const authHeader = request.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) return null;
  const token = authHeader.replace("Bearer ", "");
  return verifyJwt(token, env.JWT_SECRET);
}

function jsonResponse(data: any, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    },
  });
}
