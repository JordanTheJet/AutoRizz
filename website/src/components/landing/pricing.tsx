import Link from "next/link";
import { SUBSCRIPTION_PLANS, AI_MODES } from "@/lib/constants";

export function Pricing() {
  return (
    <section id="pricing" className="bg-gray-50 py-20">
      <div className="mx-auto max-w-6xl px-6">
        <div className="mb-12 text-center">
          <h2 className="text-3xl font-bold text-gray-900">
            Simple, Transparent Pricing
          </h2>
          <p className="mt-3 text-gray-500">
            Monthly subscriptions. Credits reset each billing cycle.
          </p>
        </div>

        <div className="mx-auto grid max-w-5xl gap-4 sm:gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {SUBSCRIPTION_PLANS.map((plan) => (
            <div
              key={plan.id}
              className={`relative rounded-xl border bg-white p-6 shadow-sm ${
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
              <h3 className="text-lg font-semibold text-gray-900">
                {plan.name}
              </h3>
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
              <Link
                href="/sign-up"
                className={`mt-6 block rounded-lg px-4 py-2.5 text-center text-sm font-medium ${
                  "recommended" in plan
                    ? "bg-brand-500 text-white hover:bg-brand-600"
                    : "bg-gray-100 text-gray-900 hover:bg-gray-200"
                }`}
              >
                {plan.priceUsd === 0 ? "Get Started" : "Subscribe"}
              </Link>
            </div>
          ))}
        </div>

        {/* AI modes table */}
        <div className="mx-auto mt-12 max-w-2xl">
          <h3 className="mb-4 text-center text-sm font-semibold text-gray-700">
            AI Modes & Credit Usage
          </h3>
          <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
            <table className="w-full text-sm">
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
          <p className="mt-2 text-center text-xs text-gray-400">
            Output tokens cost 3x input tokens. Paid plans roll over unused
            credits up to 2 months.
          </p>
        </div>
      </div>
    </section>
  );
}
