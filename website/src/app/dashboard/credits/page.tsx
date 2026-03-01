"use client";

import { Suspense, useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { createBrowserClient } from "@supabase/ssr";
import { SUBSCRIPTION_PLANS, AI_MODES } from "@/lib/constants";

function CreditsContent() {
  const [loadingPlan, setLoadingPlan] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [user, setUser] = useState<{ id: string; email?: string } | null>(null);
  const searchParams = useSearchParams();
  const success = searchParams.get("success");
  const canceled = searchParams.get("canceled");

  useEffect(() => {
    const supabase = createBrowserClient(
      process.env.NEXT_PUBLIC_SUPABASE_URL!,
      process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
    );
    supabase.auth.getUser().then(({ data }) => setUser(data.user));
  }, []);

  function handleSubscribe(planId: string) {
    const plan = SUBSCRIPTION_PLANS.find((p) => p.id === planId);
    if (!plan || !("polarProductId" in plan) || !user) {
      setError("Unable to start checkout");
      return;
    }

    setLoadingPlan(planId);
    setError(null);

    const metadata = JSON.stringify({
      user_id: user.id,
      plan_id: plan.id,
    });

    const polarProductId = "polarProductId" in plan ? plan.polarProductId : null;
    if (!polarProductId) {
      setError("Unable to start checkout");
      return;
    }

    const params = new URLSearchParams({
      products: polarProductId,
      metadata,
      ...(user.email ? { customerEmail: user.email } : {}),
    });

    window.location.href = `/api/polar/checkout?${params.toString()}`;
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900">Subscription Plans</h1>
      <p className="mt-1 text-sm text-gray-500">
        Credits reset monthly. Paid plans roll over unused credits up to 2
        months. Free plan credits do not roll over.
      </p>

      {success && (
        <div className="mt-4 rounded-lg bg-green-50 p-4 text-sm text-green-700">
          Subscription activated! Credits have been added to your account.
        </div>
      )}

      {canceled && (
        <div className="mt-4 rounded-lg bg-yellow-50 p-4 text-sm text-yellow-700">
          Checkout was canceled. No changes were made.
        </div>
      )}

      {error && (
        <div className="mt-4 rounded-lg bg-red-50 p-4 text-sm text-red-600">
          {error}
        </div>
      )}

      <div className="mt-8 grid gap-4 sm:gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {SUBSCRIPTION_PLANS.map((plan) => (
          <div
            key={plan.id}
            className={`relative rounded-xl border p-6 ${
              "recommended" in plan
                ? "border-brand-500 ring-2 ring-brand-500/20"
                : "border-gray-200"
            }`}
          >
            {"recommended" in plan && (
              <div className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-brand-500 px-3 py-0.5 text-xs font-medium text-white">
                Recommended
              </div>
            )}
            <h3 className="text-lg font-semibold text-gray-900">{plan.name}</h3>
            <div className="mt-2">
              <span className="text-3xl font-bold text-gray-900">
                {plan.priceDisplay}
              </span>
            </div>
            <p className="mt-1 text-sm text-gray-500">
              {plan.monthlyCredits.toLocaleString()} credits/mo
            </p>
            <p className="text-xs text-gray-400">
              {plan.aiModes
                .map((m) => m.charAt(0).toUpperCase() + m.slice(1))
                .join(", ")}
            </p>
            {plan.priceUsd > 0 ? (
              <button
                onClick={() => handleSubscribe(plan.id)}
                disabled={loadingPlan !== null}
                className={`mt-6 w-full rounded-lg px-4 py-2.5 text-sm font-medium disabled:opacity-50 ${
                  "recommended" in plan
                    ? "bg-brand-500 text-white hover:bg-brand-600"
                    : "bg-gray-100 text-gray-900 hover:bg-gray-200"
                }`}
              >
                {loadingPlan === plan.id ? "Redirecting..." : "Subscribe"}
              </button>
            ) : (
              <div className="mt-6 w-full rounded-lg bg-gray-50 px-4 py-2.5 text-center text-sm font-medium text-gray-500">
                Current Plan
              </div>
            )}
          </div>
        ))}
      </div>

      {/* AI mode reference */}
      <div className="mt-12">
        <h2 className="mb-4 text-sm font-semibold text-gray-700">
          AI Modes & Credit Usage
        </h2>
        <div className="-mx-4 overflow-x-auto sm:mx-0 sm:rounded-lg sm:border sm:border-gray-200">
          <table className="w-full min-w-[400px] text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-4 py-2 text-left font-medium text-gray-600">
                  Mode
                </th>
                <th className="px-4 py-2 text-left font-medium text-gray-600">
                  Multiplier
                </th>
                <th className="px-4 py-2 text-left font-medium text-gray-600">
                  Best For
                </th>
              </tr>
            </thead>
            <tbody>
              {AI_MODES.map((mode) => (
                <tr key={mode.id} className="border-b border-gray-50">
                  <td className="px-4 py-2 font-medium text-gray-900">
                    {mode.name}
                  </td>
                  <td className="px-4 py-2 text-gray-600">
                    {mode.multiplier}x
                  </td>
                  <td className="px-4 py-2 text-gray-500">
                    {mode.description}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="mt-2 text-xs text-gray-400">
          Output tokens cost 3x input tokens.
        </p>
      </div>
    </div>
  );
}

export default function CreditsPage() {
  return (
    <Suspense>
      <CreditsContent />
    </Suspense>
  );
}
