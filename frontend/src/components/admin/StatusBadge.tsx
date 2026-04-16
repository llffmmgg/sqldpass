import type { ReactNode } from "react";

type Tone =
  | "neutral"
  | "primary"
  | "accent"
  | "amber"
  | "violet"
  | "green"
  | "blue"
  | "rose"
  | "emerald"
  | "teal"
  | "sky"
  | "indigo";

type StatusBadgeProps = {
  children: ReactNode;
  tone?: Tone;
  /** 작은 dot 표시 — 상태(활성/비활성 등)에 적합 */
  dot?: boolean;
  /** 굵기 강조 — 기본 font-medium, strong=semibold */
  strong?: boolean;
  className?: string;
};

const TONES: Record<Tone, string> = {
  neutral: "bg-muted/10 text-muted ring-muted/20",
  primary: "bg-primary/10 text-primary ring-primary/25",
  accent: "bg-accent/10 text-accent ring-accent/25",
  amber: "bg-amber-400/10 text-amber-400 ring-amber-400/25",
  violet: "bg-violet-500/10 text-violet-400 ring-violet-500/25",
  green: "bg-green-500/10 text-green-400 ring-green-500/25",
  blue: "bg-blue-500/10 text-blue-400 ring-blue-500/25",
  rose: "bg-rose-500/10 text-rose-400 ring-rose-500/25",
  emerald: "bg-emerald-500/10 text-emerald-400 ring-emerald-500/25",
  teal: "bg-teal-500/10 text-teal-400 ring-teal-500/25",
  sky: "bg-sky-500/10 text-sky-400 ring-sky-500/25",
  indigo: "bg-indigo-500/10 text-indigo-400 ring-indigo-500/25",
};

const DOT_BG: Record<Tone, string> = {
  neutral: "bg-muted",
  primary: "bg-primary",
  accent: "bg-accent",
  amber: "bg-amber-400",
  violet: "bg-violet-400",
  green: "bg-green-400",
  blue: "bg-blue-400",
  rose: "bg-rose-400",
  emerald: "bg-emerald-400",
  teal: "bg-teal-400",
  sky: "bg-sky-400",
  indigo: "bg-indigo-400",
};

export default function StatusBadge({
  children,
  tone = "neutral",
  dot = false,
  strong = false,
  className = "",
}: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11px] ring-1 ring-inset ${
        strong ? "font-semibold" : "font-medium"
      } ${TONES[tone]} ${className}`}
    >
      {dot && <span className={`h-1.5 w-1.5 rounded-full ${DOT_BG[tone]}`} aria-hidden="true" />}
      {children}
    </span>
  );
}
