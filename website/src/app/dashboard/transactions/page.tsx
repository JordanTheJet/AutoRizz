import { createClient } from "@/lib/supabase/server";
import { formatCredits, formatDate, friendlyDescription } from "@/lib/utils";

export default async function TransactionsPage() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  const { data: transactions } = await supabase
    .from("credit_transactions")
    .select("*")
    .eq("user_id", user!.id)
    .order("created_at", { ascending: false })
    .limit(50);

  return (
    <div>
      <h1 className="text-2xl font-extrabold text-ink">Transaction History</h1>
      <p className="mt-1 text-sm text-ink-light">
        Credit purchases, usage, and bonuses.
      </p>

      {transactions && transactions.length > 0 ? (
        <div className="mt-6 space-y-2">
          {transactions.map(
            (tx: {
              id: number;
              created_at: string;
              type: string;
              description: string;
              amount: number;
              balance_after: number;
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
                <div className="ml-4 shrink-0 text-right">
                  <p
                    className={`text-sm font-bold ${tx.amount > 0 ? "text-accent-green" : "text-ink-light"}`}
                  >
                    {tx.amount > 0 ? "+" : ""}
                    {formatCredits(Math.abs(tx.amount))}
                  </p>
                  <p className="text-xs text-ink-muted">
                    bal {formatCredits(tx.balance_after)}
                  </p>
                </div>
              </div>
            )
          )}
        </div>
      ) : (
        <div className="mt-6 rounded-2xl bg-surface p-8 text-center">
          <p className="text-sm text-ink-muted">No transactions yet.</p>
        </div>
      )}
    </div>
  );
}
