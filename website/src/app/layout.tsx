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
      <body className="min-h-screen bg-white text-gray-900 antialiased">
        {children}
      </body>
    </html>
  );
}
