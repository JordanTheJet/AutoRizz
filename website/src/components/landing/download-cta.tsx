import { APP_VERSION } from "@/lib/constants";

export function DownloadCta() {
  const apkUrl = `${process.env.NEXT_PUBLIC_SUPABASE_URL}/storage/v1/object/public/apk-releases/autorizz-v${APP_VERSION}.apk`;

  return (
    <section id="download" className="bg-white py-20">
      <div className="mx-auto max-w-6xl px-6">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-bold text-gray-900">Get AutoRizz</h2>
          <p className="mt-3 text-gray-500">
            Download the APK and install it on your Android device. No Play
            Store required.
          </p>

          <a
            href={apkUrl}
            className="mt-8 inline-flex items-center gap-2 rounded-lg bg-brand-500 px-8 py-3.5 text-sm font-semibold text-white shadow-lg hover:bg-brand-600"
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

          <div className="mt-4 flex items-center justify-center gap-6 text-xs text-gray-400">
            <span>Android 8.0+ (API 26)</span>
            <span>~25 MB</span>
            <span>No root required</span>
          </div>

          <p className="mt-6 text-xs text-gray-400">
            Coming soon to Google Play Store.
          </p>
        </div>
      </div>
    </section>
  );
}
