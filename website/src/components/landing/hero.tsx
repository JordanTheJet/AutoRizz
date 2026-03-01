import Link from "next/link";

export function Hero() {
  return (
    <section className="relative overflow-hidden bg-gradient-to-b from-brand-900 via-brand-800 to-brand-600 pt-32 pb-20">
      <div className="mx-auto max-w-6xl px-6">
        <div className="mx-auto max-w-3xl text-center">
          <h1 className="text-4xl font-bold tracking-tight text-white sm:text-5xl lg:text-6xl">
            Your Phone. Your AI.{" "}
            <span className="text-brand-200">Your Rules.</span>
          </h1>
          <p className="mt-6 text-lg leading-relaxed text-brand-100/80">
            AutoRizz gives you an autonomous AI agent that controls your
            Android &mdash; reading screens, sending messages, making calls, and
            more. Bring your own API key or let us handle it.
          </p>
          <div className="mt-8 flex flex-col items-center justify-center gap-4 sm:flex-row">
            <a
              href="#download"
              className="rounded-lg bg-white px-6 py-3 text-sm font-semibold text-brand-600 shadow-lg hover:bg-gray-50"
            >
              Download APK
            </a>
            <Link
              href="/sign-up"
              className="rounded-lg border border-white/30 px-6 py-3 text-sm font-semibold text-white hover:bg-white/10"
            >
              Create Free Account
            </Link>
          </div>
          <p className="mt-4 text-xs text-brand-200/60">
            Android 8.0+ required. No root needed.
          </p>
        </div>
      </div>

      {/* Background decoration */}
      <div className="absolute inset-0 -z-10">
        <div className="absolute top-1/4 left-1/2 h-96 w-96 -translate-x-1/2 rounded-full bg-brand-400/20 blur-3xl" />
      </div>
    </section>
  );
}
