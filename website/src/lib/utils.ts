export function formatCredits(credits: number): string {
  return credits.toLocaleString();
}

export function formatCurrency(amount: number): string {
  return `$${amount.toFixed(2)}`;
}

export function formatDate(date: string | Date): string {
  return new Date(date).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function cn(...classes: (string | false | undefined | null)[]): string {
  return classes.filter(Boolean).join(" ");
}

/** Clean up transaction descriptions for display */
export function friendlyDescription(description: string): string {
  // Map old model-name descriptions to mode names
  return description
    .replace(/gemini-2\.5-flash-lite conversation/gi, "Fast mode")
    .replace(/gemini-3-flash-preview conversation/gi, "Thinking mode")
    .replace(/gemini-\S+ conversation/gi, "AI usage")
    .replace(/claude-\S+ conversation/gi, "AI usage");
}
