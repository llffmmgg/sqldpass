"use client";

import Link from "next/link";

type Difficulty = "쉬움" | "보통" | "어려움" | "매우 어려움";

type Props = {
  href: string;
  cert: string;            // "SQLD" / "정처기 실기" 등 자격증 라벨
  examLabel: string;       // "10회" 또는 "2024년 39회"
  title: string;
  difficulty?: Difficulty | null;
  totalQuestions: number;
  durationMin: number;
  tier?: "free" | "pass+";
  isNew?: boolean;
  score?: number | null;       // 풀이 후 best 정답 수
  scoreTotal?: number | null;  // 풀이 후 총 문항 (보통 totalQuestions 와 동일)
  verified?: "ribbon" | "corner" | "footer" | null;
};

export default function MockExamCard({
  href,
  cert,
  examLabel,
  title,
  difficulty = null,
  totalQuestions,
  durationMin,
  tier = "free",
  isNew = false,
  score = null,
  scoreTotal = null,
  verified = null,
}: Props) {
  const isPlus = tier === "pass+";
  const hasScore = score !== null;
  const isHardDiff = difficulty === "어려움" || difficulty === "매우 어려움";

  return (
    <Link href={href} className="group block">
      <div
        className={[
          "relative flex h-full flex-col rounded-xl border p-5 sm:p-6",
          // 더블라인: PASS+ 외곽 amber ring + bg gap
          isPlus
            ? "border-amber-500/40 ring-1 ring-amber-500/40 ring-offset-[3px] ring-offset-bg"
            : "border-border",
          // 본체 배경 — 라이트/다크 자동 적응 (sqldpass 토큰)
          isPlus
            ? "bg-gradient-to-b from-amber-500/[0.06] to-surface"
            : "bg-gradient-to-b from-surface to-surface/60",
          // 그림자 + 인터랙션
          "shadow-[0_1px_0_rgba(255,255,255,0.6)_inset,0_8px_24px_rgba(0,0,0,0.05)] transition-all duration-200",
          "dark:shadow-[0_1px_0_rgba(255,255,255,0.03)_inset,0_8px_24px_rgba(0,0,0,0.3)]",
          "hover:-translate-y-0.5 hover:shadow-[0_1px_0_rgba(255,255,255,0.6)_inset,0_12px_32px_rgba(0,0,0,0.08)]",
          "dark:hover:shadow-[0_1px_0_rgba(255,255,255,0.03)_inset,0_12px_32px_rgba(0,0,0,0.4)]",
        ].join(" ")}
      >
        {/* 우상단: 점수 — solved 표시 */}
        {hasScore && (
          <div
            className="pointer-events-none absolute -top-2 right-3 select-none font-mono text-[34px] font-extrabold tabular-nums text-amber-600/90 dark:text-amber-400/90 sm:right-4 sm:text-[40px]"
            style={{ transform: "rotate(-6deg)" }}
            aria-hidden
          >
            {score}
            {scoreTotal != null && (
              <span className="text-[22px] sm:text-[26px]">/{scoreTotal}</span>
            )}
          </div>
        )}

        {/* 전문가 검수: 리본 (우상단 사선) */}
        {verified === "ribbon" && !hasScore && (
          <div className="pointer-events-none absolute -right-px -top-px overflow-hidden rounded-tr-xl">
            <div className="relative h-[68px] w-[68px]">
              <div className="absolute right-[-22px] top-[14px] rotate-45 bg-emerald-600 px-7 py-1 font-mono text-[9px] font-bold uppercase tracking-[1.5px] text-white shadow-md dark:bg-emerald-500">
                검수완료
              </div>
            </div>
          </div>
        )}

        {/* 전문가 검수: 코너 노치 (점수와 공존 가능 — 점수가 있으면 좌측으로) */}
        {verified === "corner" && (
          <div
            className={`absolute top-3 sm:top-4 ${
              hasScore ? "left-3 sm:left-4" : "right-3 sm:right-4"
            }`}
          >
            <span
              title="전문가 검수 완료"
              className="inline-flex h-5 items-center gap-1 rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2 font-mono text-[9.5px] font-semibold uppercase tracking-[1.2px] text-emerald-700 dark:border-emerald-400/40 dark:bg-emerald-400/10 dark:text-emerald-300"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={3}
                className="h-2.5 w-2.5"
              >
                <path d="M5 13l4 4L19 7" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              검수
            </span>
          </div>
        )}

        {/* Eyebrow: NEW · cert · 회차 · 난이도 */}
        <div
          className={`flex flex-wrap items-center gap-x-2 gap-y-1 font-mono text-[10.5px] uppercase tracking-[1.4px] ${
            verified === "corner" && !hasScore ? "pr-14" : ""
          }`}
        >
          {isNew && (
            <>
              <span className="font-bold text-amber-700 dark:text-amber-300">NEW</span>
              <Dot />
            </>
          )}
          <span className="text-text-subtle">{cert}</span>
          <Dot />
          <span className="text-text-subtle">{examLabel}</span>
          {difficulty && (
            <>
              <Dot />
              <span
                className={
                  isHardDiff
                    ? "text-amber-700 dark:text-amber-400"
                    : "text-text-subtle"
                }
              >
                {difficulty}
              </span>
            </>
          )}
        </div>

        {/* 제목 */}
        <h3
          className={`mt-3 text-[17px] font-bold leading-[1.4] tracking-[-0.012em] text-text sm:mt-3.5 sm:text-[18px] ${
            hasScore ? "pr-16 sm:pr-20" : ""
          }`}
        >
          {title}
        </h3>

        {/* 메타 (문항 / 시간) */}
        <div className="mt-4 flex items-center gap-4 text-xs text-text-muted">
          <span className="inline-flex items-center gap-1.5">
            <DocIcon className="h-3.5 w-3.5 text-text-subtle" />
            {totalQuestions}문항
          </span>
          <span className="inline-flex items-center gap-1.5">
            <ClockIcon className="h-3.5 w-3.5 text-text-subtle" />
            {durationMin}분
          </span>
        </div>

        <div className="flex-1" />

        {/* 하단: PASS+ 라벨 + CTA */}
        <div className="mt-5 flex items-center justify-between gap-3">
          {isPlus ? (
            <span className="inline-flex items-center gap-1.5 rounded border border-amber-500/40 bg-amber-500/10 px-2 py-0.5 text-[10.5px] font-bold tracking-wide text-amber-700 dark:text-amber-300">
              <span className="h-1 w-1 rounded-full bg-amber-500 shadow-[0_0_8px_rgba(251,207,106,0.8)] dark:bg-amber-300" />
              PASS+
            </span>
          ) : (
            <span className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-text-subtle">
              FREE
            </span>
          )}

          <span
            className={[
              "rounded-md px-3 py-1.5 text-[12.5px] font-bold tracking-[-0.005em] transition-all duration-200",
              isPlus
                ? "bg-gradient-to-b from-amber-300 to-amber-400 text-amber-950 shadow-[0_4px_12px_rgba(245,181,68,0.3)] group-hover:brightness-105"
                : "border border-border text-text group-hover:border-border-strong group-hover:bg-surface-hover",
            ].join(" ")}
          >
            {hasScore ? "다시 풀기" : "응시하기"} →
          </span>
        </div>

        {/* 전문가 검수: 푸터 라인 (가장 미니멀) */}
        {verified === "footer" && (
          <div className="mt-4 flex items-center gap-2 border-t border-dashed border-border pt-3">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth={2.5}
              className="h-3 w-3 text-emerald-600 dark:text-emerald-400"
            >
              <path d="M5 13l4 4L19 7" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <span className="font-mono text-[10px] uppercase tracking-[1.2px] text-text-subtle">
              EXPERT REVIEWED
            </span>
          </div>
        )}
      </div>
    </Link>
  );
}

function Dot() {
  return (
    <span className="text-border-strong" aria-hidden>
      ·
    </span>
  );
}

function DocIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z" />
      <path d="M14 3v5h5" />
    </svg>
  );
}

function ClockIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </svg>
  );
}
