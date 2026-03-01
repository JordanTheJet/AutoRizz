import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import Stripe from "https://esm.sh/stripe@17?target=deno";

const stripe = new Stripe(Deno.env.get("STRIPE_SECRET_KEY")!, {
  apiVersion: "2024-12-18.acacia",
});

const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
);

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    // 1. Authenticate user
    const authHeader = req.headers.get("Authorization");
    if (!authHeader?.startsWith("Bearer ")) {
      return new Response("Missing authorization", { status: 401 });
    }
    const jwt = authHeader.replace("Bearer ", "");
    const {
      data: { user },
      error: authError,
    } = await supabase.auth.getUser(jwt);

    if (authError || !user) {
      return new Response("Invalid token", { status: 401 });
    }

    // 2. Get requested subscription plan
    const { plan_id } = await req.json();
    if (!plan_id) {
      return new Response(
        JSON.stringify({ error: "plan_id required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const { data: plan, error: planError } = await supabase
      .from("subscription_plans")
      .select("*")
      .eq("id", plan_id)
      .eq("active", true)
      .single();

    if (planError || !plan) {
      return new Response(
        JSON.stringify({ error: "Invalid plan" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!plan.stripe_price_id) {
      return new Response(
        JSON.stringify({ error: "Plan has no Stripe price configured" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Find or create Stripe customer
    const { data: profile } = await supabase
      .from("user_profiles")
      .select("stripe_customer_id")
      .eq("id", user.id)
      .single();

    let customerId: string;
    if (profile?.stripe_customer_id) {
      customerId = profile.stripe_customer_id;
    } else {
      const customers = await stripe.customers.list({
        email: user.email,
        limit: 1,
      });

      if (customers.data.length > 0) {
        customerId = customers.data[0].id;
      } else {
        const customer = await stripe.customers.create({
          email: user.email!,
          metadata: { supabase_user_id: user.id },
        });
        customerId = customer.id;
      }

      // Save Stripe customer ID
      await supabase
        .from("user_profiles")
        .update({ stripe_customer_id: customerId })
        .eq("id", user.id);
    }

    // 4. Check if user already has an active subscription — upgrade/downgrade
    if (profile?.stripe_customer_id) {
      const existingSubs = await stripe.subscriptions.list({
        customer: customerId,
        status: "active",
        limit: 1,
      });

      if (existingSubs.data.length > 0) {
        const sub = existingSubs.data[0];
        const updatedSub = await stripe.subscriptions.update(sub.id, {
          items: [{ id: sub.items.data[0].id, price: plan.stripe_price_id }],
          proration_behavior: "create_prorations",
          metadata: { user_id: user.id, plan_id: plan.id },
        });

        return new Response(
          JSON.stringify({
            action: "updated",
            subscription_id: updatedSub.id,
            plan_id: plan.id,
          }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    }

    // 5. Create new subscription Checkout Session
    const session = await stripe.checkout.sessions.create({
      customer: customerId,
      mode: "subscription",
      line_items: [
        {
          price: plan.stripe_price_id,
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
      success_url: "autorizz://purchase-success?session_id={CHECKOUT_SESSION_ID}",
      cancel_url: "autorizz://purchase-cancelled",
    });

    return new Response(
      JSON.stringify({
        action: "checkout",
        checkout_url: session.url,
        session_id: session.id,
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  } catch (err) {
    console.error("Checkout error:", err);
    return new Response(
      JSON.stringify({ error: err.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
