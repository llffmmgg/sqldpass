"use client";

import Link from "next/link";

type Props = {
  examNumber?: string | number;
  totalQuestions?: number;
  durationMin?: number;
  difficulty?: string;
  pricingHref?: string;
  backHref?: string;
};

export default function PassPlusLockNotice({
  examNumber = "10",
  totalQuestions = 50,
  durationMin = 90,
  difficulty = "어려움",
  pricingHref = "/checkout",
  backHref = "/mock-exams",
}: Props) {
  return (
    <div className="relative flex min-h-[80vh] items-center justify-center px-6 py-16">
      {/* 상단 미니 breadcrumb */}
      <div className="absolute left-7 top-6 hidden items-center gap-2 font-mono text-[11px] tracking-wide text-text-subtle sm:flex">
        <span>mock-exams</span>
        <span className="opacity-50">›</span>
        <span className="text-text-muted">{examNumber}</span>
      </div>

      {/* amber radial glow */}
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(245,181,68,0.08),transparent_60%)]" />

      <div className="relative w-full max-w-[540px]">
        <div className="relative overflow-hidden rounded-2xl border border-border bg-surface px-7 pt-7 pb-8 shadow-[var(--shadow-lg)] transition-shadow duration-300 hover:shadow-[var(--shadow-xl)] sm:px-9">
          {/* 메타 라인 */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <PassPlusLabel />
              <span className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-text-subtle">
                SQLD · {examNumber}회
              </span>
            </div>
            <span className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-amber-600 dark:text-amber-400">
              HARD
            </span>
          </div>

          {/* 가려진 제목 */}
          <div className="relative mt-5">
            <h2
              className="select-none text-[26px] font-bold leading-[1.3] tracking-[-0.01em] text-text"
              style={{ filter: "blur(7px)" }}
              aria-hidden
            >
              SQLD 실전 모의고사 {examNumber}회
            </h2>
            <div className="absolute inset-0 bg-gradient-to-b from-transparent via-surface/40 to-surface" />
          </div>

          {/* 흐려진 본문 */}
          <div className="mt-4 flex flex-col gap-2.5" aria-hidden>
            {[100, 88, 92, 70].map((w, i) => (
              <div
                key={i}
                className="h-[7px] rounded-sm bg-text-muted/15"
                style={{ width: `${w}%`, filter: "blur(2px)" }}
              />
            ))}
          </div>

          {/* hairline */}
          <div className="mt-7 h-px bg-[linear-gradient(90deg,transparent,var(--border)_20%,var(--border)_80%,transparent)]" />

          {/* 안내 영역 */}
          <div className="mt-6">
            <div className="flex items-start gap-4">
              <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-[10px] border border-amber-500/40 bg-gradient-to-b from-amber-500/15 to-amber-500/5 shadow-[inset_0_0_0_1px_rgba(255,255,255,0.06),0_8px_16px_rgba(245,181,68,0.10)]">
                <LockIcon className="h-[17px] w-[17px] text-amber-600 dark:text-amber-300" />
              </div>
              <div className="flex-1 pt-0.5">
                <div className="text-[15px] font-bold tracking-[-0.01em] text-text">
                  PASS+ 회차예요
                </div>
                <div className="mt-1.5 text-[13px] leading-[1.65] text-text-muted">
                  고난이도 회차는 PASS+에서 풀 수 있어요. Starter 3,900원부터, Pro 9,900원.
                </div>
              </div>
            </div>

            {/* CTA */}
            <div className="mt-5 flex gap-2.5">
              <Link
                href={pricingHref}
                className="flex-1 rounded-lg bg-gradient-to-b from-amber-300 to-amber-500 px-[18px] py-3 text-center text-[13.5px] font-bold tracking-[-0.005em] text-amber-950 shadow-[inset_0_1px_0_rgba(255,255,255,0.4),inset_0_-1px_0_rgba(0,0,0,0.1),0_8px_20px_rgba(245,181,68,0.30)] transition-all duration-200 hover:-translate-y-0.5 hover:brightness-105 hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.4),inset_0_-1px_0_rgba(0,0,0,0.1),0_12px_24px_rgba(245,181,68,0.45)]"
              >
                요금제 보기 →
              </Link>
              <Link
                href={backHref}
                className="rounded-lg border border-border bg-transparent px-[18px] py-3 text-[13px] font-medium text-text-muted transition-colors hover:border-border-strong hover:bg-surface-hover hover:text-text"
              >
                목록으로
              </Link>
            </div>
          </div>

          {/* 메타 푸터 */}
          <div className="mt-7 grid grid-cols-3 border-t border-border pt-[18px]">
            {[
              ["문항", String(totalQuestions)],
              ["권장 시간", `${durationMin}분`],
              ["난이도", difficulty],
            ].map(([k, v], i) => (
              <div
                key={k}
                className={i === 0 ? "" : "border-l border-border pl-4"}
              >
                <div className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-text-subtle">
                  {k}
                </div>
                <div className="mt-1.5 text-sm font-semibold text-text">
                  {v}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function PassPlusLabel() {
  return (
    <span className="inline-flex items-center gap-1.5 rounded border border-amber-500/40 bg-amber-500/10 px-2 py-0.5 text-[10.5px] font-bold tracking-wide text-amber-700 dark:text-amber-300">
      <span className="h-1 w-1 rounded-full bg-amber-500 shadow-[0_0_8px_rgba(251,207,106,0.8)] dark:bg-amber-300" />
      PASS+
    </span>
  );
}

function LockIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      className={className}
    >
      <rect x="4" y="11" width="16" height="10" rx="2" />
      <path d="M8 11V7a4 4 0 018 0v4" />
    </svg>
  );
}
