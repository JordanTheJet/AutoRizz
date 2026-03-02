"use client";

import { useState } from "react";
import { createClient } from "@/lib/supabase/client";
import Link from "next/link";

export default function SignUpPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    setLoading(true);

    const supabase = createClient();
    const { error } = await supabase.auth.signUp({
      email,
      password,
      options: {
        emailRedirectTo: `${window.location.origin}/callback`,
      },
    });

    if (error) {
      setError(error.message);
      setLoading(false);
      return;
    }

    setSuccess(true);
    setLoading(false);
  }

  if (success) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-page px-4">
        <div className="w-full max-w-md text-center">
          <div className="rounded-2xl bg-surface p-8">
            <div className="mb-4 text-4xl">&#9993;</div>
            <h1 className="mb-2 text-xl font-bold text-ink">
              Check your email
            </h1>
            <p className="mb-4 text-sm text-ink-light">
              We sent a confirmation link to <strong>{email}</strong>. Click it
              to activate your account.
            </p>
            <p className="text-sm font-semibold text-accent-green">
              You&apos;ll get 100 welcome credits on your first sign-in!
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-page px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <Link href="/" className="text-2xl font-extrabold text-ink">
            AutoRizz
          </Link>
          <h1 className="mt-4 text-2xl font-bold text-ink">
            Create your account
          </h1>
          <p className="mt-1 text-sm text-ink-light">
            Get 100 free credits when you sign up
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="rounded-2xl bg-surface p-8"
        >
          {error && (
            <div className="mb-4 rounded-xl bg-accent-coral/20 p-3 text-sm text-ink">
              {error}
            </div>
          )}

          <div className="mb-4">
            <label
              htmlFor="email"
              className="mb-1.5 block text-sm font-medium text-ink"
            >
              Email
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-sm text-ink focus:border-ink/30 focus:outline-none focus:ring-2 focus:ring-ink/10"
              placeholder="you@example.com"
            />
          </div>

          <div className="mb-4">
            <label
              htmlFor="password"
              className="mb-1.5 block text-sm font-medium text-ink"
            >
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
              className="w-full rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-sm text-ink focus:border-ink/30 focus:outline-none focus:ring-2 focus:ring-ink/10"
              placeholder="Min 6 characters"
            />
          </div>

          <div className="mb-6">
            <label
              htmlFor="confirmPassword"
              className="mb-1.5 block text-sm font-medium text-ink"
            >
              Confirm Password
            </label>
            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={6}
              className="w-full rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-sm text-ink focus:border-ink/30 focus:outline-none focus:ring-2 focus:ring-ink/10"
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-full bg-ink px-4 py-3 text-sm font-semibold text-white hover:bg-ink/80 disabled:opacity-50"
          >
            {loading ? "Creating account..." : "Create Account"}
          </button>

          <p className="mt-4 text-center text-sm text-ink-light">
            Already have an account?{" "}
            <Link
              href="/sign-in"
              className="font-semibold text-ink hover:underline"
            >
              Sign In
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
