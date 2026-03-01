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
      <h1 className="text-2xl font-bold text-gray-900">Account Settings</h1>
      <p className="mt-1 text-sm text-gray-500">Manage your profile.</p>

      <form
        onSubmit={handleSave}
        className="mt-6 max-w-lg space-y-4 rounded-xl border border-gray-200 bg-white p-6"
      >
        {saved && (
          <div className="rounded-lg bg-green-50 p-3 text-sm text-green-700">
            Settings saved.
          </div>
        )}
        {error && (
          <div className="rounded-lg bg-red-50 p-3 text-sm text-red-600">
            {error}
          </div>
        )}

        <div>
          <label
            htmlFor="displayName"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Display Name
          </label>
          <input
            id="displayName"
            type="text"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            placeholder="Your name"
          />
        </div>

        {referralCode && (
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Referral Code
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={referralCode}
                readOnly
                className="w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-500"
              />
              <button
                type="button"
                onClick={() => navigator.clipboard.writeText(referralCode)}
                className="shrink-0 rounded-lg bg-gray-100 px-3 py-2 text-sm text-gray-600 hover:bg-gray-200"
              >
                Copy
              </button>
            </div>
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
        >
          {loading ? "Saving..." : "Save Changes"}
        </button>
      </form>

      <div className="mt-8 max-w-lg rounded-xl border border-red-200 bg-red-50/50 p-6">
        <h2 className="text-sm font-semibold text-red-700">Danger Zone</h2>
        <p className="mt-1 text-sm text-red-600/70">
          Sign out of your account on this device.
        </p>
        <button
          onClick={handleSignOut}
          className="mt-4 rounded-lg bg-red-100 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-200"
        >
          Sign Out
        </button>
      </div>
    </div>
  );
}
