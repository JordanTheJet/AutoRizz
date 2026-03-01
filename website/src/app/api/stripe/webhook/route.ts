import { NextResponse } from "next/server";
import { headers } from "next/headers";
import { getStripe } from "@/lib/stripe";
import { createServiceClient } from "@/lib/supabase/server";
import type Stripe from "stripe";

export async function POST(request: Request) {
  const body = await request.text();
  const headersList = await headers();
  const signature = headersList.get("stripe-signature");

  if (!signature) {
    return NextResponse.json({ error: "Missing signature" }, { status: 400 });
  }

  let event: Stripe.Event;

  try {
    event = getStripe().webhooks.constructEvent(
      body,
      signature,
      process.env.STRIPE_WEBHOOK_SECRET!
    );
  } catch (err) {
    console.error("Webhook signature verification failed:", err);
    return NextResponse.json({ error: "Invalid signature" }, { status: 400 });
  }

  const supabase = createServiceClient();

  switch (event.type) {
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

    case "invoice.paid": {
      const invoice = event.data.object as Stripe.Invoice;
      const subDetails = invoice.parent?.subscription_details;
      if (!subDetails?.subscription) break;

      // Get metadata from the subscription snapshot on the invoice
      const subMeta = subDetails.metadata;
      const userId = subMeta?.user_id;
      const planId = subMeta?.plan_id;
      if (!userId || !planId) {
        // Fallback: fetch subscription directly for metadata
        const stripe = getStripe();
        const subscriptionId =
          typeof subDetails.subscription === "string"
            ? subDetails.subscription
            : subDetails.subscription.id;
        const sub = await stripe.subscriptions.retrieve(subscriptionId);
        const fallbackUserId = sub.metadata?.user_id;
        const fallbackPlanId = sub.metadata?.plan_id;
        if (!fallbackUserId || !fallbackPlanId) {
          console.error(
            "Missing metadata on subscription:",
            subscriptionId
          );
          break;
        }
        // Use invoice period (available on Invoice type in Stripe v20)
        await processInvoicePaid(
          supabase,
          invoice,
          fallbackUserId,
          fallbackPlanId
        );
        break;
      }

      await processInvoicePaid(supabase, invoice, userId, planId);
      break;
    }

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
        })
        .eq("id", userId);

      console.log(
        `Subscription updated: user=${userId}, plan=${planId}, status=${subscriptionStatus}`
      );
      break;
    }

    case "customer.subscription.deleted": {
      const subscription = event.data.object as Stripe.Subscription;
      const userId = subscription.metadata?.user_id;
      if (!userId) break;

      // Downgrade to free: expire all paid credits, reset to free allocation
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

  return NextResponse.json({ received: true });
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function processInvoicePaid(
  supabase: ReturnType<typeof createServiceClient>,
  invoice: Stripe.Invoice,
  userId: string,
  planId: string
) {
  // Idempotency check
  const { data: existing } = await supabase
    .from("credit_transactions")
    .select("id")
    .eq("stripe_payment_id", invoice.id)
    .maybeSingle();

  if (existing) {
    console.log("Invoice already processed:", invoice.id);
    return;
  }

  // Use invoice period_start/period_end (Stripe v20 API)
  const periodStart = new Date(invoice.period_start * 1000).toISOString();
  const periodEnd = new Date(invoice.period_end * 1000).toISOString();

  const { data, error } = await supabase.rpc("reset_subscription_credits", {
    p_user_id: userId,
    p_plan_id: planId,
    p_period_start: periodStart,
    p_period_end: periodEnd,
  });

  if (error) {
    console.error("Failed to reset credits:", error);
    return;
  }

  // Mark transaction for idempotency
  await supabase
    .from("credit_transactions")
    .update({ stripe_payment_id: invoice.id })
    .eq("user_id", userId)
    .eq("type", "subscription")
    .order("created_at", { ascending: false })
    .limit(1);

  console.log(
    `Credits reset: user=${userId}, plan=${planId}, balance=${data}`
  );
}
