import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { createHmac } from "https://deno.land/std@0.224.0/crypto/mod.ts";

const webhookSecret = Deno.env.get("POLAR_WEBHOOK_SECRET")!;

const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
);

async function verifyWebhookSignature(
  body: string,
  headers: Headers
): Promise<boolean> {
  // Polar uses standard webhook signature verification
  const signature = headers.get("webhook-signature");
  if (!signature) return false;

  // Polar webhook signatures use the standard format: v1,<signature>
  // Parse timestamp and signature from header
  const webhookId = headers.get("webhook-id");
  const webhookTimestamp = headers.get("webhook-timestamp");
  if (!webhookId || !webhookTimestamp) return false;

  const toSign = `${webhookId}.${webhookTimestamp}.${body}`;
  const secretBytes = Uint8Array.from(
    atob(webhookSecret.replace("polar_whs_", "").replace(/whsec_/, "")),
    (c) => c.charCodeAt(0)
  );

  const key = await crypto.subtle.importKey(
    "raw",
    secretBytes,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signatureBytes = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(toSign));
  const computedSignature = btoa(
    String.fromCharCode(...new Uint8Array(signatureBytes))
  );

  // Compare against all provided signatures (v1,<sig> format)
  const signatures = signature.split(" ");
  for (const sig of signatures) {
    const [version, value] = sig.split(",");
    if (version === "v1" && value === computedSignature) {
      return true;
    }
  }

  return false;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  try {
    const body = await req.text();

    const verified = await verifyWebhookSignature(body, req.headers);
    if (!verified) {
      console.error("Webhook signature verification failed");
      return new Response("Invalid signature", { status: 403 });
    }

    const event = JSON.parse(body);

    switch (event.type) {
      // Subscription created or activated
      case "subscription.created":
      case "subscription.active": {
        const sub = event.data;
        const userId = sub.metadata?.user_id;
        const planId = sub.metadata?.plan_id;
        if (!userId || !planId) {
          console.error("Missing metadata in subscription:", sub.id);
          break;
        }

        await supabase
          .from("user_profiles")
          .update({
            polar_customer_id: sub.customer_id,
            polar_subscription_id: sub.id,
            subscription_plan: planId,
            subscription_status: "active",
          })
          .eq("id", userId);

        console.log(`Subscription created: user=${userId}, plan=${planId}`);
        break;
      }

      // Order paid — reset credits
      case "order.paid": {
        const order = event.data;
        const userId = order.metadata?.user_id;
        const planId = order.metadata?.plan_id;
        if (!userId || !planId) {
          console.error("Missing metadata in order:", order.id);
          break;
        }

        // Idempotency check
        const { data: existing } = await supabase
          .from("credit_transactions")
          .select("id")
          .eq("polar_order_id", order.id)
          .limit(1);

        if (existing && existing.length > 0) {
          console.log("Order already processed:", order.id);
          break;
        }

        const now = new Date();
        const periodStart = now.toISOString();
        const periodEnd = new Date(
          now.getFullYear(),
          now.getMonth() + 1,
          now.getDate()
        ).toISOString();

        const { data: newBalance, error } = await supabase.rpc(
          "reset_subscription_credits",
          {
            p_user_id: userId,
            p_plan_id: planId,
            p_period_start: periodStart,
            p_period_end: periodEnd,
          }
        );

        if (error) {
          console.error("Failed to reset credits:", error);
          break;
        }

        // Mark for idempotency
        await supabase
          .from("credit_transactions")
          .update({ polar_order_id: order.id })
          .eq("user_id", userId)
          .eq("type", "subscription")
          .order("created_at", { ascending: false })
          .limit(1);

        console.log(
          `Credits reset: user=${userId}, plan=${planId}, balance=${newBalance}`
        );
        break;
      }

      // Subscription updated
      case "subscription.updated": {
        const sub = event.data;
        const userId = sub.metadata?.user_id;
        const planId = sub.metadata?.plan_id;
        if (!userId) break;

        let subscriptionStatus: string;
        if (sub.cancel_at_period_end) {
          subscriptionStatus = "canceling";
        } else if (sub.status === "active") {
          subscriptionStatus = "active";
        } else {
          subscriptionStatus = sub.status || "active";
        }

        await supabase
          .from("user_profiles")
          .update({
            subscription_plan: planId || "free",
            subscription_status: subscriptionStatus,
          })
          .eq("id", userId);

        console.log(
          `Subscription updated: user=${userId}, plan=${planId}, status=${subscriptionStatus}`
        );
        break;
      }

      // Subscription canceled or revoked
      case "subscription.canceled":
      case "subscription.revoked": {
        const sub = event.data;
        const userId = sub.metadata?.user_id;
        if (!userId) break;

        const { data: freePlan } = await supabase
          .from("subscription_plans")
          .select("monthly_credits")
          .eq("id", "free")
          .single();

        const freeCredits = freePlan?.monthly_credits ?? 100;

        await supabase
          .from("user_profiles")
          .update({
            subscription_plan: "free",
            subscription_status: "canceled",
            polar_subscription_id: null,
            credit_balance: freeCredits,
            rollover_credits: 0,
          })
          .eq("id", userId);

        await supabase.from("credit_transactions").insert({
          user_id: userId,
          type: "expiration",
          amount: 0,
          balance_after: freeCredits,
          description: "Subscription canceled — paid credits expired",
        });

        console.log(
          `Subscription canceled: user=${userId}, credits reset to ${freeCredits}`
        );
        break;
      }
    }

    return new Response("OK", { status: 200 });
  } catch (err) {
    console.error("Webhook error:", err);
    return new Response(`Webhook error: ${err.message}`, { status: 400 });
  }
});
