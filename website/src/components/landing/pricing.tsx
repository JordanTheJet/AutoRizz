import Link from "next/link";
import { SUBSCRIPTION_PLANS, AI_MODES } from "@/lib/constants";

const PLAN_COLORS = ["bg-white/60", "bg-accent-blue/30", "bg-accent-green/30", "bg-accent-coral/30"];

export function Pricing() {
  return (
    <section id="pricing" className="px-4 py-6 md:px-6">
      <div className="mx-auto max-w-6xl">
        <div className="rounded-3xl bg-surface px-8 py-16 md:px-16">
          <div className="mb-12">
            <h2 className="text-4xl font-extrabold tracking-tight text-ink md:text-5xl">
              Simple pricing,
              <br />
              no surprises
            </h2>
            <p className="mt-4 max-w-md text-lg text-ink-light">
              Monthly credits. Cancel anytime. BYOK mode is always free.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
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
                <h3 className="text-lg font-bold text-ink">
                  {plan.name}
                </h3>
                <div className="mt-3">
                  <span className="text-3xl font-extrabold text-ink">
                    {plan.priceDisplay}
                  </span>
                </div>
                <p className="mt-1 text-sm text-ink-light">
                  {plan.monthlyCredits.toLocaleString()} credits/mo
                </p>
                <p className="mt-0.5 text-xs text-ink-muted">
                  {plan.aiModes
                    .map((m) => m.charAt(0).toUpperCase() + m.slice(1))
                    .join(" + ")}{" "}
                  modes
                </p>
                <Link
                  href="/sign-up"
                  className={`mt-6 block rounded-full px-4 py-2.5 text-center text-sm font-semibold transition-colors ${
                    "recommended" in plan
                      ? "bg-ink text-white hover:bg-ink/80"
                      : "bg-ink/10 text-ink hover:bg-ink/20"
                  }`}
                >
                  {plan.priceUsd === 0 ? "Get Started" : "Subscribe"}
                </Link>
              </div>
            ))}
          </div>

          {/* AI modes */}
          <div className="mt-12 max-w-2xl">
            <h3 className="mb-4 text-sm font-bold uppercase tracking-wide text-ink-muted">
              AI Modes & Credit Usage
            </h3>
            <div className="overflow-hidden rounded-2xl bg-white/60">
              <table className="w-full text-sm">
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
              Output tokens cost 3x input tokens. Paid plans roll over unused
              credits up to 2 months.
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
