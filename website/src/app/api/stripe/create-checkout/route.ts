import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { getStripe } from "@/lib/stripe";
import { SUBSCRIPTION_PLANS, SITE_URL } from "@/lib/constants";

export async function POST(request: Request) {
  try {
    const supabase = await createClient();
    const {
      data: { user },
      error: authError,
    } = await supabase.auth.getUser();

    if (authError || !user) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { planId } = await request.json();
    const plan = SUBSCRIPTION_PLANS.find((p) => p.id === planId);

    if (!plan || plan.priceUsd === 0) {
      return NextResponse.json({ error: "Invalid plan" }, { status: 400 });
    }

    // Look up Stripe price from subscription_plans table
    const { data: dbPlan } = await supabase
      .from("subscription_plans")
      .select("stripe_price_id")
      .eq("id", planId)
      .single();

    if (!dbPlan?.stripe_price_id) {
      return NextResponse.json(
        { error: "Plan not configured for payments" },
        { status: 400 }
      );
    }

    const session = await getStripe().checkout.sessions.create({
      mode: "subscription",
      line_items: [
        {
          price: dbPlan.stripe_price_id,
          quantity: 1,
        },
      ],
      metadata: {
        user_id: user.id,
        plan_id: plan.id,
      },
      subscription_data: {
        metadata: {
          user_id: user.id,
          plan_id: plan.id,
        },
      },
      customer_email: user.email,
      success_url: `${SITE_URL}/dashboard/credits?success=true`,
      cancel_url: `${SITE_URL}/dashboard/credits?canceled=true`,
    });

    return NextResponse.json({ url: session.url });
  } catch (err) {
    console.error("Stripe checkout error:", err);
    return NextResponse.json(
      { error: "Failed to create checkout session" },
      { status: 500 }
    );
  }
}
