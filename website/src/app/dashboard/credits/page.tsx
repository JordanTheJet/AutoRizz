"use client";

import { Suspense, useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { createBrowserClient } from "@supabase/ssr";
import { SUBSCRIPTION_PLANS, AI_MODES } from "@/lib/constants";

const PLAN_COLORS = ["bg-white/60", "bg-accent-blue/20", "bg-accent-green/20", "bg-accent-coral/20"];

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
      <h1 className="text-2xl font-extrabold text-ink">Subscription Plans</h1>
      <p className="mt-1 text-sm text-ink-light">
        Credits reset monthly. Paid plans roll over unused credits up to 2
        months. Free plan credits do not roll over.
      </p>

      {success && (
        <div className="mt-4 rounded-xl bg-accent-green/20 p-4 text-sm font-medium text-ink">
          Subscription activated! Credits have been added to your account.
        </div>
      )}

      {canceled && (
        <div className="mt-4 rounded-xl bg-accent-coral/20 p-4 text-sm font-medium text-ink">
          Checkout was canceled. No changes were made.
        </div>
      )}

      {error && (
        <div className="mt-4 rounded-xl bg-accent-coral/20 p-4 text-sm text-ink">
          {error}
        </div>
      )}

      <div className="mt-8 grid gap-4 sm:gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {SUBSCRIPTION_PLANS.map((plan, i) => (
          <div
            key={plan.id}
            className={`relative rounded-2xl ${PLAN_COLORS[i]} p-6 ${
              "recommended" in plan
                ? "ring-2 ring-ink"
                : ""
            }`}
          >
            {"recommended" in plan && (
              <div className="absolute -top-3 left-4 rounded-full bg-ink px-3 py-0.5 text-xs font-semibold text-white">
                Popular
              </div>
            )}
            <h3 className="text-lg font-bold text-ink">{plan.name}</h3>
            <div className="mt-2">
              <span className="text-3xl font-extrabold text-ink">
                {plan.priceDisplay}
              </span>
            </div>
            <p className="mt-1 text-sm text-ink-light">
              {plan.monthlyCredits.toLocaleString()} credits/mo
            </p>
            <p className="text-xs text-ink-muted">
              {plan.aiModes
                .map((m) => m.charAt(0).toUpperCase() + m.slice(1))
                .join(" + ")}{" "}
              modes
            </p>
            {plan.priceUsd > 0 ? (
              <button
                onClick={() => handleSubscribe(plan.id)}
                disabled={loadingPlan !== null}
                className={`mt-6 w-full rounded-full px-4 py-2.5 text-sm font-semibold disabled:opacity-50 ${
                  "recommended" in plan
                    ? "bg-ink text-white hover:bg-ink/80"
                    : "bg-ink/10 text-ink hover:bg-ink/20"
                }`}
              >
                {loadingPlan === plan.id ? "Redirecting..." : "Subscribe"}
              </button>
            ) : (
              <div className="mt-6 w-full rounded-full bg-ink/5 px-4 py-2.5 text-center text-sm font-semibold text-ink-muted">
                Current Plan
              </div>
            )}
          </div>
        ))}
      </div>

      {/* AI mode reference */}
      <div className="mt-12">
        <h2 className="mb-4 text-sm font-bold uppercase tracking-wide text-ink-muted">
          AI Modes & Credit Usage
        </h2>
        <div className="-mx-4 overflow-x-auto sm:mx-0 sm:rounded-2xl sm:bg-surface">
          <table className="w-full min-w-[400px] text-sm">
            <thead>
              <tr className="border-b border-ink/5">
                <th className="px-5 py-3 text-left font-semibold text-ink">
                  Mode
                </th>
                <th className="px-5 py-3 text-left font-semibold text-ink">
                  Multiplier
                </th>
                <th className="px-5 py-3 text-left font-semibold text-ink">
                  Best For
                </th>
              </tr>
            </thead>
            <tbody>
              {AI_MODES.map((mode) => (
                <tr key={mode.id} className="border-b border-ink/5 last:border-0">
                  <td className="px-5 py-3 font-medium text-ink">
                    {mode.name}
                  </td>
                  <td className="px-5 py-3 text-ink-light">
                    {mode.multiplier}x
                  </td>
                  <td className="px-5 py-3 text-ink-light">
                    {mode.description}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="mt-3 text-xs text-ink-muted">
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
