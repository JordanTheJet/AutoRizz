"use client";

import { useState } from "react";
import Link from "next/link";

export function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <nav className="fixed top-0 z-50 w-full border-b border-gray-200/50 bg-white/80 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link href="/" className="text-xl font-bold text-brand-500">
          AutoRizz
        </Link>

        {/* Desktop nav */}
        <div className="hidden items-center gap-8 md:flex">
          <a
            href="#features"
            className="text-sm text-gray-600 hover:text-gray-900"
          >
            Features
          </a>
          <a
            href="#pricing"
            className="text-sm text-gray-600 hover:text-gray-900"
          >
            Pricing
          </a>
          <a
            href="#download"
            className="text-sm text-gray-600 hover:text-gray-900"
          >
            Download
          </a>
          <Link
            href="/sign-in"
            className="text-sm text-gray-600 hover:text-gray-900"
          >
            Sign In
          </Link>
          <Link
            href="/sign-up"
            className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600"
          >
            Get Started
          </Link>
        </div>

        {/* Mobile menu toggle */}
        <button
          onClick={() => setMobileOpen(!mobileOpen)}
          className="md:hidden"
          aria-label="Toggle menu"
        >
          <svg
            className="h-6 w-6 text-gray-600"
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
        <div className="border-t border-gray-200 bg-white px-6 py-4 md:hidden">
          <div className="flex flex-col gap-4">
            <a
              href="#features"
              className="text-sm text-gray-600"
              onClick={() => setMobileOpen(false)}
            >
              Features
            </a>
            <a
              href="#pricing"
              className="text-sm text-gray-600"
              onClick={() => setMobileOpen(false)}
            >
              Pricing
            </a>
            <a
              href="#download"
              className="text-sm text-gray-600"
              onClick={() => setMobileOpen(false)}
            >
              Download
            </a>
            <Link href="/sign-in" className="text-sm text-gray-600">
              Sign In
            </Link>
            <Link
              href="/sign-up"
              className="rounded-lg bg-brand-500 px-4 py-2 text-center text-sm font-medium text-white"
            >
              Get Started
            </Link>
          </div>
        </div>
      )}
    </nav>
  );
}
