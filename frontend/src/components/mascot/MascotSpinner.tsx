"use client";

import MascotImage from "./MascotImage";

interface MascotSpinnerProps {
  message?: string;
  size?: "sm" | "md";
}

export default function MascotSpinner({
  message = "로딩 중...",
  size = "md",
}: MascotSpinnerProps) {
  const ringSize = size === "sm" ? 64 : 96;
  const mascotSize = size === "sm" ? 44 : 72;
  const ring = ringSize;
  const padding = (ring - mascotSize) / 2;

  return (
    <div
      className={`flex flex-col items-center justify-center gap-3 ${
        size === "sm" ? "py-6" : "py-16"
      }`}
      role="status"
      aria-live="polite"
    >
      <div
        className="relative"
        style={{ width: ring, height: ring }}
      >
        <span
          className="absolute inset-0 rounded-full border-2 border-border border-t-primary animate-spin"
          aria-hidden
        />
        <span
          className="absolute inline-flex items-center justify-center mascot-float"
          style={{ inset: padding }}
          aria-hidden
        >
          <MascotImage pose="work" size={mascotSize} />
        </span>
      </div>
      {message && (
        <p className="text-sm text-text-muted">{message}</p>
      )}
      <style jsx>{`
        @keyframes mascotFloat {
          0%, 100% { transform: translateY(-2px); }
          50%      { transform: translateY(2px); }
        }
        .mascot-float {
          animation: mascotFloat 2.4s ease-in-out infinite;
          will-change: transform;
        }
        @media (prefers-reduced-motion: reduce) {
          .mascot-float { animation: none; }
        }
      `}</style>
    </div>
  );
}
