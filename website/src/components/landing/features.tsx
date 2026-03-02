const FEATURES = [
  {
    title: "Smart Swiping",
    description:
      "AI learns your type and auto-swipes on Hinge, Tinder, and Bumble. Per-app strategies that respect daily limits.",
    color: "bg-accent-coral",
  },
  {
    title: "AI Conversations",
    description:
      "Natural, witty replies that match your personality. Keeps conversations going while you focus on your day.",
    color: "bg-accent-blue",
  },
  {
    title: "Date Scheduling",
    description:
      "Automatically suggests dates, picks venues, and handles the logistics. From match to meetup, hands-free.",
    color: "bg-accent-green",
  },
  {
    title: "4 AI Providers",
    description:
      "Claude, GPT, Gemini, and OpenRouter. Switch models mid-conversation for the best results.",
    color: "bg-accent-lavender",
  },
  {
    title: "BYOK Mode",
    description:
      "Bring your own API keys for completely free, fully private usage. No account required.",
    color: "bg-accent-green",
  },
  {
    title: "Full Device Control",
    description:
      "Screen reading, taps, swipes, typing, notifications. 42 tools for complete Android autonomy.",
    color: "bg-accent-blue",
  },
];

export function Features() {
  return (
    <section id="features" className="px-4 py-6 md:px-6">
      <div className="mx-auto max-w-6xl">
        <div className="rounded-3xl bg-surface px-8 py-16 md:px-16">
          <div className="mb-12">
            <h2 className="text-4xl font-extrabold tracking-tight text-ink md:text-5xl">
              Everything you need
              <br />
              to automate dating
            </h2>
            <p className="mt-4 max-w-md text-lg text-ink-light">
              A fully autonomous agent with native Android integration.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map((feature) => (
              <div
                key={feature.title}
                className="group rounded-2xl bg-white/60 p-6 transition-colors hover:bg-white"
              >
                <div
                  className={`mb-4 inline-block rounded-xl ${feature.color} px-3 py-1.5`}
                >
                  <span className="text-xs font-bold uppercase tracking-wide text-ink">
                    {feature.title}
                  </span>
                </div>
                <p className="text-sm leading-relaxed text-ink-light">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
