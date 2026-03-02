"use client";

import { useState, useEffect, useCallback } from "react";

const PROFILES = [
  {
    name: "Sophie",
    age: 24,
    bio: "Coffee addict. Dog mom. Let's go on an adventure",
    gradient: "from-rose-300 to-pink-400",
    emoji: "🎨",
    image: "/stock3.jpg",
    distance: "2 miles away",
  },

  {
    name: "Ary",
    age: 16,
    bio: "Film nerd. Will judge you by your letterboxd",
    gradient: "from-amber-200 to-orange-300",
    emoji: "🎬",

    distance: "3 miles away",
    image: "/ary1.jpg",
  },
  {
    name: "Mia",
    age: 25,
    bio: "Startup founder by day, salsa dancer by night",
    gradient: "from-teal-200 to-emerald-300",
    emoji: "💃",
    image: "/stock2.jpg",
    distance: "1 mile away",
  },
  {
    name: "Jordan",
    age: 26,
    bio: "Yoga instructor who loves tacos more than yoga",
    gradient: "from-violet-300 to-purple-400",
    emoji: "🧘",
    image: "/jordan.jpeg",
    distance: "0 miles away",
  },
  {
    name: "Ava",
    age: 27,
    bio: "Looking for someone to split appetizers with",
    gradient: "from-sky-200 to-blue-300",
    emoji: "✈️",
    image: "/stock1.jpg",
    distance: "4 miles away",
  },
];

type SwipeDir = "right" | "left";

export function SwipeCards() {
  const [stack, setStack] = useState(PROFILES.map((_, i) => i));
  const [swiping, setSwiping] = useState<{
    index: number;
    dir: SwipeDir;
  } | null>(null);
  const [showBadge, setShowBadge] = useState(false);

  const doSwipe = useCallback(() => {
    if (swiping || stack.length === 0) return;

    const topIndex = stack[stack.length - 1];
    const dir: SwipeDir = Math.random() > 0.3 ? "right" : "left";

    setShowBadge(true);
    setSwiping({ index: topIndex, dir });

    setTimeout(() => {
      setShowBadge(false);
    }, 400);

    setTimeout(() => {
      setStack((prev) => {
        const next = prev.slice(0, -1);
        if (next.length === 0) {
          return PROFILES.map((_, i) => i);
        }
        return next;
      });
      setSwiping(null);
    }, 600);
  }, [swiping, stack]);

  useEffect(() => {
    const interval = setInterval(doSwipe, 2000);
    return () => clearInterval(interval);
  }, [doSwipe]);

  // Show up to 3 cards in the stack
  const visible = stack.slice(-3);

  return (
    <div className="relative h-[360px] w-[260px] md:h-[420px] md:w-[300px]">
      {visible.map((profileIndex, stackPos) => {
        const profile = PROFILES[profileIndex];
        const isTop = stackPos === visible.length - 1;
        const depth = visible.length - 1 - stackPos;

        const isAnimating = swiping && swiping.index === profileIndex;

        let transform = `translateY(${depth * 8}px) scale(${1 - depth * 0.04})`;
        let opacity = 1 - depth * 0.15;
        let transition =
          "transform 0.6s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.6s ease";

        if (isAnimating) {
          const xDir = swiping.dir === "right" ? 1 : -1;
          transform = `translateX(${xDir * 400}px) rotate(${xDir * 25}deg)`;
          opacity = 0;
          transition =
            "transform 0.6s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.5s ease";
        }

        return (
          <div
            key={`${profileIndex}-${stack.length}`}
            className="absolute inset-0 rounded-3xl shadow-lg overflow-hidden"
            style={{
              transform,
              opacity,
              transition,
              zIndex: stackPos,
            }}
          >
            {/* Card background */}
            <div
              className={`h-full w-full bg-gradient-to-b ${profile.gradient} flex flex-col`}
            >
              {/* Photo area */}
              <div className="relative flex-1 flex items-center justify-center overflow-hidden">
                {profile.image ? (
                  <img
                    src={profile.image}
                    alt={profile.name}
                    className="absolute inset-0 h-full w-full object-cover"
                  />
                ) : (
                  <span className="text-7xl md:text-8xl">{profile.emoji}</span>
                )}

                {/* Like/Nope badge */}
                {isTop && showBadge && swiping && (
                  <div
                    className={`absolute top-6 rounded-lg border-4 px-4 py-1 text-2xl font-extrabold uppercase tracking-wider ${
                      swiping.dir === "right"
                        ? "left-4 rotate-[-15deg] border-green-500 text-green-500"
                        : "right-4 rotate-[15deg] border-red-500 text-red-500"
                    }`}
                    style={{
                      animation:
                        "badgePop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)",
                    }}
                  >
                    {swiping.dir === "right" ? "LIKE" : "NOPE"}
                  </div>
                )}
              </div>

              {/* Info bar */}
              <div className="bg-white px-5 py-4">
                <div className="flex items-baseline gap-2">
                  <span className="text-xl font-bold text-ink">
                    {profile.name}
                  </span>
                  <span className="text-lg text-ink-light">{profile.age}</span>
                </div>
                <p className="mt-0.5 text-xs text-ink-muted">
                  {profile.distance}
                </p>
                <p className="mt-1.5 text-sm text-ink-light leading-snug">
                  {profile.bio}
                </p>

                {/* Action buttons */}
                <div className="mt-3 flex items-center justify-center gap-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full border-2 border-red-300 text-red-400">
                    <svg
                      className="h-5 w-5"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2.5}
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M6 18L18 6M6 6l12 12"
                      />
                    </svg>
                  </div>
                  <div className="flex h-12 w-12 items-center justify-center rounded-full border-2 border-green-400 text-green-500">
                    <svg
                      className="h-6 w-6"
                      fill="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
                    </svg>
                  </div>
                  <div className="flex h-10 w-10 items-center justify-center rounded-full border-2 border-blue-300 text-blue-400">
                    <svg
                      className="h-5 w-5"
                      fill="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );
      })}

      <style jsx>{`
        @keyframes badgePop {
          0% {
            transform: scale(0.5);
            opacity: 0;
          }
          100% {
            transform: scale(1);
            opacity: 1;
          }
        }
      `}</style>
    </div>
  );
}
