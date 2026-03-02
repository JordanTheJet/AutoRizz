import { APP_VERSION } from "@/lib/constants";

export default function DownloadPage() {
  const apkUrl = `${process.env.NEXT_PUBLIC_SUPABASE_URL}/storage/v1/object/public/apk-releases/autorizz-v${APP_VERSION}.apk`;

  return (
    <div>
      <h1 className="text-2xl font-extrabold text-ink">Download AutoRizz</h1>
      <p className="mt-1 text-sm text-ink-light">
        Get the latest version of AutoRizz for Android.
      </p>

      <div className="mt-8 max-w-lg rounded-2xl bg-surface p-8 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-accent-blue/30">
          <svg
            className="h-8 w-8 text-ink"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
            />
          </svg>
        </div>

        <h2 className="text-lg font-bold text-ink">
          AutoRizz v{APP_VERSION}
        </h2>

        <a
          href={apkUrl}
          className="mt-4 inline-flex items-center gap-2 rounded-full bg-ink px-6 py-3 text-sm font-semibold text-white hover:bg-ink/80"
        >
          <svg
            className="h-4 w-4"
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
          Download APK
        </a>

        <div className="mt-6 space-y-1 text-sm text-ink-muted">
          <p>Android 8.0+ (API 26) required</p>
          <p>No root access needed</p>
        </div>
      </div>

      <div className="mt-8 max-w-lg">
        <h2 className="mb-3 text-sm font-bold uppercase tracking-wide text-ink-muted">
          Installation Instructions
        </h2>
        <ol className="space-y-2 text-sm text-ink-light">
          <li className="flex gap-3">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-ink text-xs font-bold text-white">1</span>
            Download the APK file to your Android device
          </li>
          <li className="flex gap-3">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-ink text-xs font-bold text-white">2</span>
            Open the file &mdash; you may need to allow installs from unknown sources
          </li>
          <li className="flex gap-3">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-ink text-xs font-bold text-white">3</span>
            Tap &quot;Install&quot; and wait for the installation to complete
          </li>
          <li className="flex gap-3">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-ink text-xs font-bold text-white">4</span>
            Open AutoRizz and grant the required permissions
          </li>
          <li className="flex gap-3">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-ink text-xs font-bold text-white">5</span>
            Sign in with the same account you created on this website
          </li>
        </ol>
      </div>
    </div>
  );
}
