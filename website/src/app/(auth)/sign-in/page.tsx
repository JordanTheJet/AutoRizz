"use client";

import { Suspense, useState } from "react";
import { createClient } from "@/lib/supabase/client";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";

function SignInForm() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirect = searchParams.get("redirect") || "/dashboard";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const supabase = createClient();
    const { error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });

    if (error) {
      setError(error.message);
      setLoading(false);
      return;
    }

    router.push(redirect);
    router.refresh();
  }

  return (
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

      <div className="mb-6">
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
          placeholder="••••••••"
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full rounded-full bg-ink px-4 py-3 text-sm font-semibold text-white hover:bg-ink/80 disabled:opacity-50"
      >
        {loading ? "Signing in..." : "Sign In"}
      </button>

      <p className="mt-4 text-center text-sm text-ink-light">
        Don&apos;t have an account?{" "}
        <Link
          href="/sign-up"
          className="font-semibold text-ink hover:underline"
        >
          Sign Up
        </Link>
      </p>
    </form>
  );
}

export default function SignInPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-page px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <Link href="/" className="text-2xl font-extrabold text-ink">
            AutoRizz
          </Link>
          <h1 className="mt-4 text-2xl font-bold text-ink">
            Sign in to your account
          </h1>
        </div>
        <Suspense>
          <SignInForm />
        </Suspense>
      </div>
    </div>
  );
}
