/**
 * LLM proxy for Cloudflare Workers.
 * Port of supabase/functions/llm-proxy/index.ts to Workers format.
 * Uses D1 for credit checks/deduction instead of Supabase RPC.
 * Single provider: Vertex AI (Google) with 2 modes: fast + thinking.
 */

import { type Env, type authenticateRequest } from "./auth";

// AI Mode → Model mapping (server-side only)
const MODE_MODELS: Record<string, string> = {
  fast: "gemini-2.5-flash-lite",
  thinking: "gemini-3-flash-preview",
};

const MODE_MULTIPLIERS: Record<string, number> = {
  fast: 1,
  thinking: 5,
};

interface ProxyRequest {
  ai_mode?: string;  // "fast" | "thinking"
  model?: string;    // Legacy (ignored)
  system_prompt?: string;
  messages: Array<{ role: string; content: any }>;
  tools?: Array<{ name: string; description: string; input_schema: any }>;
  max_tokens: number;
  stream: boolean;
}

function buildVertexRequest(req: ProxyRequest) {
  return {
    contents: req.messages.map((m) => ({
      role: m.role === "user" ? "user" : "model",
      parts: [
        {
          text:
            typeof m.content === "string"
              ? m.content
              : JSON.stringify(m.content),
        },
      ],
    })),
    systemInstruction: req.system_prompt
      ? { parts: [{ text: req.system_prompt }] }
      : undefined,
    generationConfig: { maxOutputTokens: req.max_tokens },
  };
}

function transformVertexResponse(data: any) {
  const candidate = data.candidates?.[0];
  const content =
    candidate?.content?.parts?.map((p: any) => ({
      type: "text",
      text: p.text || "",
    })) || [];
  return {
    content,
    stop_reason: "end_turn",
    usage: data.usageMetadata
      ? {
          input_tokens: data.usageMetadata.promptTokenCount || 0,
          output_tokens: data.usageMetadata.candidatesTokenCount || 0,
        }
      : null,
  };
}

export async function handleProxy(
  request: Request,
  env: Env,
  userId: string
): Promise<Response> {
  // 1. Check credit balance
  const user = await env.DB.prepare(
    "SELECT credit_balance FROM users WHERE id = ?"
  )
    .bind(userId)
    .first<{ credit_balance: number }>();

  if (!user || user.credit_balance <= 0) {
    return new Response(
      JSON.stringify({
        error: "Insufficient credits",
        balance: user?.credit_balance || 0,
      }),
      { status: 402, headers: { "Content-Type": "application/json" } }
    );
  }

  // 2. Parse request and resolve model from ai_mode
  const proxyReq: ProxyRequest = await request.json();
  const mode = (proxyReq.ai_mode || "fast").toLowerCase();
  const model = MODE_MODELS[mode] || MODE_MODELS["fast"];
  const multiplier = MODE_MULTIPLIERS[mode] || 1;

  const googleKey = (env as any).GOOGLE_API_KEY;
  if (!googleKey) {
    return new Response(
      JSON.stringify({ error: "No API key configured" }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }

  // 3. Forward to Vertex AI (using AI Studio endpoint for CF Workers — no service account needed)
  const vertexBody = buildVertexRequest(proxyReq);
  const providerUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${googleKey}`;

  const providerResp = await fetch(providerUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(vertexBody),
  });

  if (!providerResp.ok) {
    const errorText = await providerResp.text();
    console.error(`Provider error (${providerResp.status}):`, errorText);
    return new Response(
      JSON.stringify({ error: `Provider error: ${providerResp.status}` }),
      { status: 502, headers: { "Content-Type": "application/json" } }
    );
  }

  // 4. Transform response and deduct credits
  const providerData = await providerResp.json();
  const transformedResponse = transformVertexResponse(providerData);

  if (transformedResponse.usage) {
    const rawCost =
      transformedResponse.usage.input_tokens * multiplier +
      transformedResponse.usage.output_tokens * 3 * multiplier;
    const totalCost = Math.max(1, Math.ceil(rawCost / 1000));

    try {
      const result = await env.DB.prepare(
        `UPDATE users SET credit_balance = credit_balance - ?, updated_at = datetime('now')
         WHERE id = ? AND credit_balance >= ?
         RETURNING credit_balance`
      )
        .bind(totalCost, userId, totalCost)
        .first<{ credit_balance: number }>();

      if (result) {
        transformedResponse.remaining_credits = result.credit_balance;

        await env.DB.prepare(
          `INSERT INTO credit_transactions (user_id, type, amount, balance_after, description)
           VALUES (?, 'usage', ?, ?, ?)`
        )
          .bind(
            userId,
            -totalCost,
            result.credit_balance,
            `${mode} mode conversation`
          )
          .run();

        await env.DB.prepare(
          `INSERT INTO usage_logs (user_id, provider, model, input_tokens, output_tokens)
           VALUES (?, ?, ?, ?, ?)`
        )
          .bind(
            userId,
            "google",
            model,
            transformedResponse.usage.input_tokens,
            transformedResponse.usage.output_tokens
          )
          .run();
      }
    } catch (e) {
      console.error("Credit deduction failed:", e);
    }
  }

  return new Response(JSON.stringify(transformedResponse), {
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    },
  });
}
