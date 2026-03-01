import { NextResponse } from "next/server";
import {
  validateEvent,
  WebhookVerificationError,
} from "@polar-sh/sdk/webhooks";
import { createServiceClient } from "@/lib/supabase/server";

export async function POST(request: Request) {
  const body = await request.text();
  const webhookSecret = process.env.POLAR_WEBHOOK_SECRET!;

  let event: ReturnType<typeof validateEvent>;

  try {
    event = validateEvent(body, Object.fromEntries(request.headers), webhookSecret);
  } catch (err) {
    if (err instanceof WebhookVerificationError) {
      console.error("Webhook signature verification failed:", err.message);
      return NextResponse.json({ error: "Invalid signature" }, { status: 403 });
    }
    throw err;
  }

  const supabase = createServiceClient();

  switch (event.type) {
    // Subscription activated (new checkout completed)
    case "subscription.created":
    case "subscription.active": {
      const sub = event.data;
      const userId = sub.metadata?.user_id as string | undefined;
      const planId = sub.metadata?.plan_id as string | undefined;
      if (!userId || !planId) {
        console.error("Missing metadata in subscription:", sub.id);
        break;
      }

      await supabase
        .from("user_profiles")
        .update({
          polar_customer_id: sub.customerId,
          polar_subscription_id: sub.id,
          subscription_plan: planId,
          subscription_status: "active",
        })
        .eq("id", userId);

      console.log(`Subscription created: user=${userId}, plan=${planId}`);
      break;
    }

    // Order paid — reset credits for the billing period
    case "order.paid": {
      const order = event.data;
      const userId = order.metadata?.user_id as string | undefined;
      const planId = order.metadata?.plan_id as string | undefined;
      if (!userId || !planId) {
        console.error("Missing metadata in order:", order.id);
        break;
      }

      // Idempotency check
      const { data: existing } = await supabase
        .from("credit_transactions")
        .select("id")
        .eq("polar_order_id", order.id)
        .maybeSingle();

      if (existing) {
        console.log("Order already processed:", order.id);
        break;
      }

      // Calculate period from subscription or use current month
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

      // Mark transaction for idempotency
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

    // Subscription updated (plan change, status change)
    case "subscription.updated": {
      const sub = event.data;
      const userId = sub.metadata?.user_id as string | undefined;
      const planId = sub.metadata?.plan_id as string | undefined;
      if (!userId) break;

      let subscriptionStatus: string;
      if (sub.cancelAtPeriodEnd) {
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
      const userId = sub.metadata?.user_id as string | undefined;
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

  return NextResponse.json({ received: true });
}
