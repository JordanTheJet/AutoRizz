import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import Stripe from "https://esm.sh/stripe@17?target=deno";

const stripe = new Stripe(Deno.env.get("STRIPE_SECRET_KEY")!, {
  apiVersion: "2024-12-18.acacia",
});

const endpointSecret = Deno.env.get("STRIPE_WEBHOOK_SECRET");

const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
);

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  try {
    const body = await req.text();
    let event: Stripe.Event;

    if (endpointSecret) {
      const sig = req.headers.get("stripe-signature");
      if (!sig) {
        return new Response("Missing signature", { status: 400 });
      }
      event = stripe.webhooks.constructEvent(body, sig, endpointSecret);
    } else {
      event = JSON.parse(body) as Stripe.Event;
    }

    switch (event.type) {
      // New subscription created via Checkout
      case "checkout.session.completed": {
        const session = event.data.object as Stripe.Checkout.Session;
        if (session.mode !== "subscription") break;

        const userId = session.metadata?.user_id;
        const planId = session.metadata?.plan_id;
        if (!userId || !planId) {
          console.error("Missing metadata in checkout session:", session.id);
          break;
        }

        await supabase
          .from("user_profiles")
          .update({
            stripe_customer_id: session.customer as string,
            stripe_subscription_id: session.subscription as string,
            subscription_plan: planId,
            subscription_status: "active",
          })
          .eq("id", userId);

        console.log(`Subscription created: user=${userId}, plan=${planId}`);
        break;
      }

      // Recurring payment succeeded — reset credits
      case "invoice.paid": {
        const invoice = event.data.object as Stripe.Invoice;
        if (!invoice.subscription) break;

        const subscription = await stripe.subscriptions.retrieve(
          invoice.subscription as string
        );
        const userId = subscription.metadata?.user_id;
        const planId = subscription.metadata?.plan_id;
        if (!userId || !planId) {
          console.error("Missing metadata on subscription:", subscription.id);
          break;
        }

        // Idempotency check
        const { data: existing } = await supabase
          .from("credit_transactions")
          .select("id")
          .eq("stripe_payment_id", invoice.id)
          .limit(1);

        if (existing && existing.length > 0) {
          console.log("Invoice already processed:", invoice.id);
          break;
        }

        const periodStart = new Date(
          subscription.current_period_start * 1000
        ).toISOString();
        const periodEnd = new Date(
          subscription.current_period_end * 1000
        ).toISOString();

        // Reset credits with rollover
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

        // Mark transaction with invoice ID for idempotency
        await supabase
          .from("credit_transactions")
          .update({ stripe_payment_id: invoice.id })
          .eq("user_id", userId)
          .eq("type", "subscription")
          .order("created_at", { ascending: false })
          .limit(1);

        console.log(
          `Credits reset: user=${userId}, plan=${planId}, balance=${newBalance}`
        );
        break;
      }

      // Subscription updated (upgrade/downgrade/cancel-at-period-end)
      case "customer.subscription.updated": {
        const subscription = event.data.object as Stripe.Subscription;
        const userId = subscription.metadata?.user_id;
        const planId = subscription.metadata?.plan_id;
        if (!userId) break;

        // cancel_at_period_end = user canceled but keeps access until period ends
        // Credits stay intact; they expire only when subscription.deleted fires
        let subscriptionStatus: string;
        if (subscription.cancel_at_period_end) {
          subscriptionStatus = "canceling";
        } else if (subscription.status === "active") {
          subscriptionStatus = "active";
        } else if (subscription.status === "past_due") {
          subscriptionStatus = "past_due";
        } else {
          subscriptionStatus = "canceled";
        }

        await supabase
          .from("user_profiles")
          .update({
            subscription_plan: planId || "free",
            subscription_status: subscriptionStatus,
            current_period_start: new Date(
              subscription.current_period_start * 1000
            ).toISOString(),
            current_period_end: new Date(
              subscription.current_period_end * 1000
            ).toISOString(),
          })
          .eq("id", userId);

        console.log(
          `Subscription updated: user=${userId}, plan=${planId}, status=${subscriptionStatus}`
        );
        break;
      }

      // Subscription canceled — expire paid credits, downgrade to free
      case "customer.subscription.deleted": {
        const subscription = event.data.object as Stripe.Subscription;
        const userId = subscription.metadata?.user_id;
        if (!userId) break;

        // Look up free plan allocation
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
            stripe_subscription_id: null,
            credit_balance: freeCredits,
            rollover_credits: 0,
          })
          .eq("id", userId);

        // Log the credit expiration
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
