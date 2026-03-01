import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { getPolar } from "@/lib/polar";
import { SUBSCRIPTION_PLANS, SITE_URL } from "@/lib/constants";
import { createClient } from "@/lib/supabase/server";

export async function GET(request: NextRequest) {
  try {
    const supabase = await createClient();
    const {
      data: { user },
      error: authError,
    } = await supabase.auth.getUser();

    if (authError || !user) {
      return NextResponse.redirect(`${SITE_URL}/sign-in`);
    }

    const params = request.nextUrl.searchParams;
    const productId = params.get("products");
    const metadataStr = params.get("metadata");
    const customerEmail = params.get("customerEmail");

    if (!productId) {
      return NextResponse.redirect(
        `${SITE_URL}/dashboard/credits?error=missing_product`
      );
    }

    const metadata = metadataStr ? JSON.parse(metadataStr) : {};

    const polar = getPolar();
    const checkout = await polar.checkouts.create({
      products: [productId],
      successUrl: `${SITE_URL}/dashboard/credits?success=true`,
      metadata: {
        user_id: user.id,
        plan_id: metadata.plan_id || "",
        ...metadata,
      },
      customerEmail: customerEmail || user.email || undefined,
    });

    if (!checkout.url) {
      return NextResponse.redirect(
        `${SITE_URL}/dashboard/credits?error=checkout_failed`
      );
    }

    return NextResponse.redirect(checkout.url);
  } catch (err) {
    console.error("Polar checkout error:", err);
    return NextResponse.redirect(
      `${SITE_URL}/dashboard/credits?error=checkout_failed`
    );
  }
}
