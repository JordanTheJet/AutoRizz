"use client";

import { useState, useEffect } from "react";
import { createClient } from "@/lib/supabase/client";
import { useRouter } from "next/navigation";

export default function SettingsPage() {
  const [displayName, setDisplayName] = useState("");
  const [referralCode, setReferralCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    async function loadProfile() {
      const supabase = createClient();
      const {
        data: { user },
      } = await supabase.auth.getUser();
      if (!user) return;

      const { data } = await supabase
        .from("user_profiles")
        .select("display_name, referral_code")
        .eq("id", user.id)
        .single();

      if (data) {
        setDisplayName(data.display_name || "");
        setReferralCode(data.referral_code || "");
      }
    }
    loadProfile();
  }, []);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSaved(false);

    const supabase = createClient();
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) return;

    const { error: updateError } = await supabase
      .from("user_profiles")
      .update({ display_name: displayName })
      .eq("id", user.id);

    if (updateError) {
      setError(updateError.message);
    } else {
      setSaved(true);
    }
    setLoading(false);
  }

  async function handleSignOut() {
    const supabase = createClient();
    await supabase.auth.signOut();
    router.push("/");
    router.refresh();
  }

  return (
    <div>
      <h1 className="text-2xl font-extrabold text-ink">Account Settings</h1>
      <p className="mt-1 text-sm text-ink-light">Manage your profile.</p>

      <form
        onSubmit={handleSave}
        className="mt-6 max-w-lg space-y-4 rounded-2xl bg-surface p-6"
      >
        {saved && (
          <div className="rounded-xl bg-accent-green/20 p-3 text-sm font-medium text-ink">
            Settings saved.
          </div>
        )}
        {error && (
          <div className="rounded-xl bg-accent-coral/20 p-3 text-sm text-ink">
            {error}
          </div>
        )}

        <div>
          <label
            htmlFor="displayName"
            className="mb-1.5 block text-sm font-medium text-ink"
          >
            Display Name
          </label>
          <input
            id="displayName"
            type="text"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="w-full rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-sm text-ink focus:border-ink/30 focus:outline-none focus:ring-2 focus:ring-ink/10"
            placeholder="Your name"
          />
        </div>

        {referralCode && (
          <div>
            <label className="mb-1.5 block text-sm font-medium text-ink">
              Referral Code
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={referralCode}
                readOnly
                className="w-full rounded-xl border border-ink/10 bg-white/60 px-4 py-2.5 text-sm text-ink-muted"
              />
              <button
                type="button"
                onClick={() => navigator.clipboard.writeText(referralCode)}
                className="shrink-0 rounded-xl bg-ink/5 px-4 py-2.5 text-sm font-medium text-ink hover:bg-ink/10"
              >
                Copy
              </button>
            </div>
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="rounded-full bg-ink px-5 py-2.5 text-sm font-semibold text-white hover:bg-ink/80 disabled:opacity-50"
        >
          {loading ? "Saving..." : "Save Changes"}
        </button>
      </form>

      <div className="mt-8 max-w-lg rounded-2xl bg-accent-coral/10 p-6">
        <h2 className="text-sm font-bold text-ink">Danger Zone</h2>
        <p className="mt-1 text-sm text-ink-light">
          Sign out of your account on this device.
        </p>
        <button
          onClick={handleSignOut}
          className="mt-4 rounded-full bg-accent-coral/30 px-5 py-2.5 text-sm font-semibold text-ink hover:bg-accent-coral/50"
        >
          Sign Out
        </button>
      </div>
    </div>
  );
}
