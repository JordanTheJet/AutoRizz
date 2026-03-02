import Link from "next/link";

export function Hero() {
  return (
    <section className="px-4 pt-24 pb-6 md:px-6 md:pt-28">
      <div className="mx-auto max-w-6xl rounded-3xl bg-surface px-8 py-16 md:px-16 md:py-24">
        <div className="max-w-3xl">
          <h1 className="text-5xl font-extrabold leading-[1.05] tracking-tight text-ink md:text-7xl">
            Swipe Smart.{" "}
            <br className="hidden md:block" />
            Chat Better.{" "}
            <br className="hidden md:block" />
            Date More.
          </h1>
          <p className="mt-6 max-w-lg text-lg leading-relaxed text-ink-light">
            Your autonomous AI agent for dating apps. AutoRizz handles swiping,
            conversations, and scheduling &mdash; so you can focus on the dates
            that matter.
          </p>
          <div className="mt-10 flex flex-col gap-3 sm:flex-row">
            <a
              href="#download"
              className="inline-flex items-center justify-center rounded-full bg-ink px-7 py-3.5 text-sm font-semibold text-white hover:bg-ink/80"
            >
              Download APK
            </a>
            <Link
              href="/sign-up"
              className="inline-flex items-center justify-center rounded-full border-2 border-ink/15 px-7 py-3.5 text-sm font-semibold text-ink hover:bg-ink/5"
            >
              Create Free Account
            </Link>
          </div>
        </div>

        {/* Stats row */}
        <div className="mt-16 grid grid-cols-2 gap-4 md:grid-cols-4 md:gap-6">
          <div className="rounded-2xl bg-accent-blue px-5 py-6">
            <div className="text-3xl font-extrabold text-ink">42+</div>
            <div className="mt-1 text-sm font-medium text-ink/70">
              AI Tools
            </div>
          </div>
          <div className="rounded-2xl bg-accent-green px-5 py-6">
            <div className="text-3xl font-extrabold text-ink">4</div>
            <div className="mt-1 text-sm font-medium text-ink/70">
              AI Providers
            </div>
          </div>
          <div className="rounded-2xl bg-accent-coral px-5 py-6">
            <div className="text-3xl font-extrabold text-ink">3</div>
            <div className="mt-1 text-sm font-medium text-ink/70">
              Dating Apps
            </div>
          </div>
          <div className="rounded-2xl bg-accent-lavender px-5 py-6">
            <div className="text-3xl font-extrabold text-ink">24/7</div>
            <div className="mt-1 text-sm font-medium text-ink/70">
              Autonomous
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
