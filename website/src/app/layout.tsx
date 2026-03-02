import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "AutoRizz - Autonomous AI Agent for Android",
  description:
    "Control your Android with AI. Bring your own key free, or use credits for managed AI. 33 tools, 4 providers, zero friction.",
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_SITE_URL || "http://localhost:3000"
  ),
  openGraph: {
    title: "AutoRizz",
    description:
      "Autonomous AI agent for Android. No other hardware required.",
    url: "https://autorizz.lol",
    siteName: "AutoRizz",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <head>
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="min-h-screen bg-page text-ink antialiased">
        {children}
      </body>
    </html>
  );
}
