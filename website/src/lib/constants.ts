export const BRAND = {
  name: "AutoRizz",
  tagline: "Autonomous AI Agent for Android",
  primary: "#1565C0",
  primaryDark: "#0D47A1",
  primaryLight: "#42A5F5",
} as const;

export const SUBSCRIPTION_PLANS = [
  {
    id: "free",
    name: "Free",
    monthlyCredits: 100,
    priceUsd: 0,
    priceDisplay: "Free",
    aiModes: ["fast", "thinking"],
  },
  {
    id: "starter",
    name: "Starter",
    monthlyCredits: 1_000,
    priceUsd: 4.99,
    priceDisplay: "$4.99/mo",
    aiModes: ["fast", "thinking"],
  },
  {
    id: "pro",
    name: "Pro",
    monthlyCredits: 10_000,
    priceUsd: 19.99,
    priceDisplay: "$19.99/mo",
    aiModes: ["fast", "thinking"],
    recommended: true,
  },
  {
    id: "ultra",
    name: "Ultra",
    monthlyCredits: 100_000,
    priceUsd: 99.0,
    priceDisplay: "$99/mo",
    aiModes: ["fast", "thinking"],
  },
] as const;

export const AI_MODES = [
  {
    id: "fast",
    name: "Fast",
    description: "Quick responses, simple tasks",
    tier: "Standard",
    multiplier: 1,
  },
  {
    id: "thinking",
    name: "Thinking",
    description: "Deep reasoning, complex tasks",
    tier: "Thinking",
    multiplier: 5,
  },
] as const;

export const WELCOME_BONUS_CREDITS = 100;

export const APP_VERSION = process.env.NEXT_PUBLIC_APP_VERSION || "0.1.0";
export const SITE_URL =
  process.env.NEXT_PUBLIC_SITE_URL || "http://localhost:3000";
