import { APP_VERSION } from "@/lib/constants";

export function DownloadCta() {
  const apkUrl = `${process.env.NEXT_PUBLIC_SUPABASE_URL}/storage/v1/object/public/apk-releases/autorizz-v${APP_VERSION}.apk`;

  return (
    <section id="download" className="px-4 py-6 md:px-6">
      <div className="mx-auto max-w-6xl">
        <div className="rounded-3xl bg-ink px-8 py-16 text-center md:px-16 md:py-20">
          <h2 className="text-4xl font-extrabold tracking-tight text-white md:text-5xl">
            Ready to rizz?
          </h2>
          <p className="mx-auto mt-4 max-w-md text-lg text-white/60">
            Download the APK and install on your Android. No Play Store required.
          </p>

          <a
            href={apkUrl}
            className="mt-10 inline-flex items-center gap-2 rounded-full bg-white px-8 py-4 text-sm font-bold text-ink hover:bg-white/90"
          >
            <svg
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
              />
            </svg>
            Download APK v{APP_VERSION}
          </a>

          <div className="mt-6 flex items-center justify-center gap-6 text-xs text-white/40">
            <span>Android 8.0+</span>
            <span className="h-1 w-1 rounded-full bg-white/20" />
            <span>~25 MB</span>
            <span className="h-1 w-1 rounded-full bg-white/20" />
            <span>No root required</span>
          </div>
        </div>
      </div>
    </section>
  );
}
