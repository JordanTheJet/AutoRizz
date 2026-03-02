"use client";

import { useState } from "react";
import Link from "next/link";

export function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <nav className="fixed top-0 z-50 w-full px-4 pt-4 md:px-6 md:pt-6">
      <div className="mx-auto max-w-6xl rounded-2xl bg-surface px-6 py-3 shadow-sm">
        <div className="flex items-center justify-between">
          <Link href="/" className="text-xl font-extrabold tracking-tight text-ink">
            AutoRizz
          </Link>

          {/* Desktop nav */}
          <div className="hidden items-center gap-8 md:flex">
            <a
              href="#features"
              className="text-sm font-medium text-ink-light hover:text-ink"
            >
              Features
            </a>
            <a
              href="#pricing"
              className="text-sm font-medium text-ink-light hover:text-ink"
            >
              Pricing
            </a>
            <a
              href="#download"
              className="text-sm font-medium text-ink-light hover:text-ink"
            >
              Download
            </a>
            <Link
              href="/sign-in"
              className="text-sm font-medium text-ink-light hover:text-ink"
            >
              Sign In
            </Link>
          </div>

          <Link
            href="/sign-up"
            className="hidden rounded-full bg-ink px-5 py-2 text-sm font-semibold text-white hover:bg-ink/80 md:inline-flex"
          >
            Get Started
          </Link>

          {/* Mobile menu toggle */}
          <button
            onClick={() => setMobileOpen(!mobileOpen)}
            className="md:hidden"
            aria-label="Toggle menu"
          >
            <svg
              className="h-6 w-6 text-ink"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              {mobileOpen ? (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              ) : (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 6h16M4 12h16M4 18h16"
                />
              )}
            </svg>
          </button>
        </div>

        {/* Mobile menu */}
        {mobileOpen && (
          <div className="mt-4 border-t border-surface-alt pt-4 md:hidden">
            <div className="flex flex-col gap-3">
              <a
                href="#features"
                className="text-sm font-medium text-ink-light"
                onClick={() => setMobileOpen(false)}
              >
                Features
              </a>
              <a
                href="#pricing"
                className="text-sm font-medium text-ink-light"
                onClick={() => setMobileOpen(false)}
              >
                Pricing
              </a>
              <a
                href="#download"
                className="text-sm font-medium text-ink-light"
                onClick={() => setMobileOpen(false)}
              >
                Download
              </a>
              <Link
                href="/sign-in"
                className="text-sm font-medium text-ink-light"
              >
                Sign In
              </Link>
              <Link
                href="/sign-up"
                className="mt-1 rounded-full bg-ink px-5 py-2.5 text-center text-sm font-semibold text-white"
              >
                Get Started
              </Link>
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}
