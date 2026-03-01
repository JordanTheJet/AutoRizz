import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const VERTEX_API_KEY = Deno.env.get("VERTEX_API_KEY")!;
const GOOGLE_API_KEY = Deno.env.get("GOOGLE_API_KEY"); // AI Studio fallback

// AI Mode → Model mapping (server-side only, not exposed to clients)
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

// --- Request/Response transformation for Vertex AI Gemini ---

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

// --- Main handler ---

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST",
        "Access-Control-Allow-Headers": "Authorization, Content-Type",
      },
    });
  }

  try {
    // 1. Validate JWT
    const authHeader = req.headers.get("Authorization");
    if (!authHeader?.startsWith("Bearer ")) {
      return new Response("Missing authorization", { status: 401 });
    }
    const jwt = authHeader.replace("Bearer ", "");

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);
    const {
      data: { user },
      error: authError,
    } = await supabase.auth.getUser(jwt);
    if (authError || !user) {
      return new Response("Invalid token", { status: 401 });
    }

    // 2. Get user profile
    const { data: profile } = await supabase
      .from("user_profiles")
      .select("credit_balance")
      .eq("id", user.id)
      .single();

    if (!profile || profile.credit_balance <= 0) {
      return new Response(
        JSON.stringify({
          error: "Insufficient credits",
          balance: profile?.credit_balance || 0,
        }),
        { status: 402, headers: { "Content-Type": "application/json" } }
      );
    }

    // 3. Parse request and resolve model from ai_mode
    const proxyReq: ProxyRequest = await req.json();
    const mode = (proxyReq.ai_mode || "fast").toLowerCase();
    const model = MODE_MODELS[mode] || MODE_MODELS["fast"];
    const multiplier = MODE_MULTIPLIERS[mode] || 1;

    // 4. Call Vertex AI Express Mode, fall back to AI Studio on error
    const vertexBody = buildVertexRequest(proxyReq);
    const requestBody = JSON.stringify(vertexBody);

    // Try Vertex AI Express first
    const vertexUrl = `https://aiplatform.googleapis.com/v1/publishers/google/models/${model}:generateContent?key=${VERTEX_API_KEY}`;
    let providerResp = await fetch(vertexUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: requestBody,
    });

    // Fall back to AI Studio if Vertex fails and we have a key
    if (!providerResp.ok && GOOGLE_API_KEY) {
      console.warn(`Vertex AI failed (${providerResp.status}), falling back to AI Studio`);
      const studioUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GOOGLE_API_KEY}`;
      providerResp = await fetch(studioUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: requestBody,
      });
    }

    if (!providerResp.ok) {
      const errorText = await providerResp.text();
      console.error(`Provider error (${providerResp.status}):`, errorText);
      return new Response(
        JSON.stringify({ error: `Provider error: ${providerResp.status}` }),
        { status: 502, headers: { "Content-Type": "application/json" } }
      );
    }

    // 5. Transform response and deduct credits
    const providerData = await providerResp.json();
    const transformedResponse = transformVertexResponse(providerData);

    if (transformedResponse.usage) {
      try {
        const { data: newBalance } = await supabase.rpc("deduct_credits", {
          p_user_id: user.id,
          p_input_tokens: transformedResponse.usage.input_tokens,
          p_output_tokens: transformedResponse.usage.output_tokens,
          p_model: model,
          p_multiplier: multiplier,
          p_ai_mode: mode,
        });
        transformedResponse.remaining_credits = newBalance;
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
  } catch (e) {
    console.error("Proxy error:", e);
    return new Response(
      JSON.stringify({ error: "Internal proxy error" }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
});
