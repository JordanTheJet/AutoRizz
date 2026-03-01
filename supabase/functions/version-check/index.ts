import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const { data, error } = await supabase
      .from("app_config")
      .select("value")
      .eq("key", "app_version")
      .single();

    if (error) throw error;

    const config = data.value;

    // Parse optional query param for client version
    const url = new URL(req.url);
    const clientVersion = parseInt(url.searchParams.get("v") || "0");

    const response: Record<string, unknown> = {
      min_version_code: config.min_version_code,
      latest_version_code: config.latest_version_code,
      latest_version_name: config.latest_version_name,
      update_url: config.update_url,
    };

    if (clientVersion > 0) {
      response.force_update = clientVersion < config.min_version_code;
      response.update_available = clientVersion < config.latest_version_code;
      if (config.update_message) {
        response.update_message = config.update_message;
      }
    }

    return new Response(JSON.stringify(response), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
