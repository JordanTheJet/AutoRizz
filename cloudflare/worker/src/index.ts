/**
 * AutoRizz API — Cloudflare Worker
 * Entry point: routes requests to auth, proxy, credits, profile, and sync handlers.
 */

import {
  type Env,
  handleSignUp,
  handleSignIn,
  handleRefresh,
  authenticateRequest,
} from "./auth";
import { handleProxy } from "./proxy";

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, POST, PATCH, DELETE, OPTIONS",
          "Access-Control-Allow-Headers": "Authorization, Content-Type",
        },
      });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    try {
      // --- Public routes (no auth required) ---
      if (path === "/auth/signup" && request.method === "POST") {
        return handleSignUp(request, env);
      }
      if (path === "/auth/signin" && request.method === "POST") {
        return handleSignIn(request, env);
      }
      if (path === "/auth/refresh" && request.method === "POST") {
        return handleRefresh(request, env);
      }

      // --- Protected routes (auth required) ---
      const user = await authenticateRequest(request, env);
      if (!user) {
        return jsonResponse({ error: "Unauthorized" }, 401);
      }

      // LLM Proxy
      if (path === "/llm-proxy" && request.method === "POST") {
        return handleProxy(request, env, user.sub);
      }

      // Credits balance
      if (path === "/credits/balance" && request.method === "GET") {
        const result = await env.DB.prepare(
          "SELECT credit_balance FROM users WHERE id = ?"
        )
          .bind(user.sub)
          .first<{ credit_balance: number }>();
        return jsonResponse({ balance: result?.credit_balance || 0 });
      }

      // Profile
      if (path === "/profile" && request.method === "GET") {
        const profile = await env.DB.prepare(
          `SELECT id, email, display_name, credit_balance, referral_code, created_at
           FROM users WHERE id = ?`
        )
          .bind(user.sub)
          .first();
        if (!profile) return jsonResponse({ error: "Not found" }, 404);
        return jsonResponse(profile);
      }

      if (path === "/profile" && request.method === "PATCH") {
        const updates = (await request.json()) as Record<string, any>;
        const allowed = [
          "display_name",
          "auto_refill_enabled",
          "auto_refill_pack",
          "auto_refill_threshold",
        ];
        const sets: string[] = [];
        const values: any[] = [];

        for (const key of allowed) {
          if (key in updates) {
            sets.push(`${key} = ?`);
            values.push(updates[key]);
          }
        }

        if (sets.length > 0) {
          sets.push("updated_at = datetime('now')");
          values.push(user.sub);
          await env.DB.prepare(
            `UPDATE users SET ${sets.join(", ")} WHERE id = ?`
          )
            .bind(...values)
            .run();
        }

        return jsonResponse({ ok: true });
      }

      // Sync push/pull (stubs)
      if (path === "/sync/push" && request.method === "POST") {
        // TODO: Accept and store entities
        return jsonResponse({ ok: true });
      }

      if (path === "/sync/pull" && request.method === "GET") {
        const since = url.searchParams.get("since") || "0";
        // TODO: Return entities changed since timestamp
        return jsonResponse({ conversations: [], messages: [] });
      }

      return jsonResponse({ error: "Not found" }, 404);
    } catch (e: any) {
      console.error("Worker error:", e);
      return jsonResponse({ error: "Internal server error" }, 500);
    }
  },
};

function jsonResponse(data: any, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    },
  });
}
