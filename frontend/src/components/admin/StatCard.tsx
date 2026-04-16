import type { ReactNode } from "react";

type Tone = "default" | "primary" | "accent" | "amber" | "violet" | "green" | "blue" | "rose";

type StatCardProps = {
  label: string;
  value: number | string;
  /** 값 아래 서브 라인 (예: "오늘 +12") */
  sub?: string;
  /** 색상 톤 — 의미 있는 구분에만 사용 */
  tone?: Tone;
  /** 우측 상단 아이콘 */
  icon?: ReactNode;
};

const TONE_VALUE: Record<Tone, string> = {
  default: "text-foreground",
  primary: "text-primary",
  accent: "text-accent",
  amber: "text-amber-400",
  violet: "text-violet-400",
  green: "text-green-400",
  blue: "text-blue-400",
  rose: "text-rose-400",
};

const TONE_ICON_BG: Record<Tone, string> = {
  default: "bg-muted/15 text-muted",
  primary: "bg-primary/10 text-primary",
  accent: "bg-accent/10 text-accent",
  amber: "bg-amber-400/10 text-amber-400",
  violet: "bg-violet-400/10 text-violet-400",
  green: "bg-green-400/10 text-green-400",
  blue: "bg-blue-400/10 text-blue-400",
  rose: "bg-rose-400/10 text-rose-400",
};

/**
 * 관리자 대시보드의 숫자 지표 카드.
 * 큰 숫자는 tabular-nums + mono feel로 스캔하기 쉽게.
 */
export default function StatCard({
  label,
  value,
  sub,
  tone = "default",
  icon,
}: StatCardProps) {
  const displayValue = typeof value === "number" ? value.toLocaleString() : value;
  return (
    <div className="group relative overflow-hidden rounded-xl border border-border bg-surface p-5 transition-colors hover:border-foreground/20">
      <div className="flex items-start justify-between gap-3">
        <p className="text-xs font-medium uppercase tracking-wider text-muted">{label}</p>
        {icon && (
          <span
            className={`flex h-8 w-8 items-center justify-center rounded-md ${TONE_ICON_BG[tone]}`}
            aria-hidden="true"
          >
            {icon}
          </span>
        )}
      </div>
      <p className={`mt-3 text-3xl font-bold tabular-nums leading-none ${TONE_VALUE[tone]}`}>
        {displayValue}
      </p>
      {sub && <p className="mt-2 text-xs text-muted">{sub}</p>}
    </div>
  );
}
