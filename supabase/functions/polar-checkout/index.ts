import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const POLAR_API = "https://api.polar.sh/v1";
const POLAR_ACCESS_TOKEN = Deno.env.get("POLAR_ACCESS_TOKEN")!;

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
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
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
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    if (!plan.polar_product_id) {
      return new Response(
        JSON.stringify({ error: "Plan has no Polar product configured" }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // 3. Create Polar checkout session
    const checkoutRes = await fetch(`${POLAR_API}/checkouts/`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${POLAR_ACCESS_TOKEN}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        products: [plan.polar_product_id],
        successUrl:
          "autorizz://purchase-success?checkout_id={CHECKOUT_ID}",
        metadata: {
          user_id: user.id,
          plan_id: plan.id,
        },
        customerEmail: user.email,
      }),
    });

    if (!checkoutRes.ok) {
      const errBody = await checkoutRes.text();
      console.error("Polar checkout API error:", errBody);
      return new Response(
        JSON.stringify({ error: "Failed to create checkout" }),
        {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    const checkout = await checkoutRes.json();

    return new Response(
      JSON.stringify({
        checkout_url: checkout.url,
        session_id: checkout.id,
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  } catch (err) {
    console.error("Checkout error:", err);
    return new Response(
      JSON.stringify({ error: err.message }),
      {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  }
});
