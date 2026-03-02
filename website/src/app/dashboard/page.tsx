import { createClient } from "@/lib/supabase/server";
import { formatCredits, formatDate, friendlyDescription } from "@/lib/utils";
import Link from "next/link";

export default async function DashboardPage() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  const { data: profile } = await supabase
    .from("user_profiles")
    .select("display_name, credit_balance")
    .eq("id", user!.id)
    .single();

  const { data: transactions } = await supabase
    .from("credit_transactions")
    .select("*")
    .eq("user_id", user!.id)
    .order("created_at", { ascending: false })
    .limit(5);

  const balance = profile?.credit_balance ?? 0;
  const displayName = profile?.display_name || user!.email;

  return (
    <div>
      <h1 className="text-2xl font-extrabold text-ink">
        Welcome back, {displayName}
      </h1>

      <div className="mt-6 grid gap-4 sm:gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {/* Balance card */}
        <div className="rounded-2xl bg-ink p-6 text-white">
          <p className="text-sm font-medium opacity-60">Credit Balance</p>
          <p className="mt-1 text-3xl font-extrabold">{formatCredits(balance)}</p>
          <Link
            href="/dashboard/credits"
            className="mt-4 inline-block rounded-full bg-white/20 px-4 py-2 text-sm font-semibold hover:bg-white/30"
          >
            Buy More
          </Link>
        </div>

        {/* Quick actions */}
        <Link
          href="/dashboard/download"
          className="flex items-center gap-3 rounded-2xl bg-surface p-6 hover:bg-surface-alt"
        >
          <div className="rounded-xl bg-accent-blue/30 p-2">
            <svg
              className="h-6 w-6 text-ink"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
              />
            </svg>
          </div>
          <div>
            <p className="font-semibold text-ink">Download APK</p>
            <p className="text-sm text-ink-light">Get the latest version</p>
          </div>
        </Link>

        <Link
          href="/dashboard/settings"
          className="flex items-center gap-3 rounded-2xl bg-surface p-6 hover:bg-surface-alt"
        >
          <div className="rounded-xl bg-accent-green/30 p-2">
            <svg
              className="h-6 w-6 text-ink"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065zM15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
          </div>
          <div>
            <p className="font-semibold text-ink">Account Settings</p>
            <p className="text-sm text-ink-light">Manage your profile</p>
          </div>
        </Link>
      </div>

      {/* Recent transactions */}
      <div className="mt-8">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-ink">
            Recent Activity
          </h2>
          <Link
            href="/dashboard/transactions"
            className="text-sm font-semibold text-ink-light hover:text-ink"
          >
            View all
          </Link>
        </div>

        {transactions && transactions.length > 0 ? (
          <div className="mt-4 space-y-2">
            {transactions.map(
              (tx: {
                id: number;
                created_at: string;
                type: string;
                description: string;
                amount: number;
              }) => (
                <div
                  key={tx.id}
                  className="flex items-center justify-between rounded-xl bg-surface px-4 py-3"
                >
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ink">
                      {friendlyDescription(tx.description)}
                    </p>
                    <p className="text-xs text-ink-muted">
                      {formatDate(tx.created_at)}
                    </p>
                  </div>
                  <p
                    className={`ml-4 shrink-0 text-sm font-bold ${tx.amount > 0 ? "text-accent-green" : "text-ink-light"}`}
                  >
                    {tx.amount > 0 ? "+" : ""}
                    {formatCredits(Math.abs(tx.amount))}
                  </p>
                </div>
              )
            )}
          </div>
        ) : (
          <p className="mt-4 text-sm text-ink-muted">No transactions yet.</p>
        )}
      </div>
    </div>
  );
}
