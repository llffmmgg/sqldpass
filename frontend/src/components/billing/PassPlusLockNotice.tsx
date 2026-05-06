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
      <div className="absolute left-7 top-6 flex items-center gap-2 font-mono text-[11px] tracking-wide text-neutral-500 dark:text-neutral-500">
        <span>mock-exams</span>
        <span className="opacity-50">›</span>
        <span className="text-neutral-600 dark:text-neutral-400">{examNumber}</span>
      </div>

      {/* amber radial glow */}
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(245,181,68,0.06),transparent_60%)] dark:bg-[radial-gradient(circle_at_50%_0%,rgba(245,181,68,0.05),transparent_60%)]" />

      <div className="relative w-full max-w-[540px]">
        <div
          className="
            relative rounded-[14px] px-9 pt-7 pb-8
            border border-neutral-200 dark:border-neutral-800
            bg-gradient-to-b from-white to-neutral-50
            dark:from-neutral-900 dark:to-neutral-950
            shadow-[0_1px_0_0_rgba(255,255,255,0.6)_inset,0_24px_60px_rgba(0,0,0,0.08),0_2px_8px_rgba(0,0,0,0.04)]
            dark:shadow-[0_1px_0_0_rgba(255,255,255,0.04)_inset,0_24px_60px_rgba(0,0,0,0.5),0_2px_8px_rgba(0,0,0,0.3)]
          "
        >
          {/* 메타 라인 */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <PassPlusLabel size="sm" />
              <span className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-neutral-500 dark:text-neutral-500">
                SQLD · {examNumber}회
              </span>
            </div>
            <span className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-amber-700 dark:text-amber-600">
              HARD
            </span>
          </div>

          {/* 가려진 제목 */}
          <div className="relative mt-5">
            <h2
              className="select-none text-[26px] font-bold leading-[1.3] tracking-[-0.01em] text-neutral-900 dark:text-neutral-100"
              style={{ filter: "blur(7px)" }}
              aria-hidden
            >
              SQLD 실전 모의고사 {examNumber}회
            </h2>
            <div className="absolute inset-0 bg-[linear-gradient(180deg,transparent_20%,rgba(250,250,250,0.92)_90%)] dark:bg-[linear-gradient(180deg,transparent_20%,rgba(20,20,22,0.85)_90%)]" />
          </div>

          {/* 흐려진 본문 */}
          <div className="mt-4 flex flex-col gap-2.5" aria-hidden>
            {[100, 88, 92, 70].map((w, i) => (
              <div
                key={i}
                className="h-[7px] rounded-sm bg-neutral-200/70 dark:bg-neutral-800/55"
                style={{ width: `${w}%`, filter: "blur(2px)" }}
              />
            ))}
          </div>

          {/* hairline */}
          <div className="mt-7 h-px bg-[linear-gradient(90deg,transparent,#e5e5e5_20%,#e5e5e5_80%,transparent)] dark:bg-[linear-gradient(90deg,transparent,#2a2a2e_20%,#2a2a2e_80%,transparent)]" />

          {/* 안내 영역 */}
          <div className="mt-6">
            <div className="flex items-start gap-4">
              <div
                className="
                  flex h-10 w-10 flex-shrink-0 items-center justify-center
                  rounded-[10px]
                  border border-amber-500/40 dark:border-amber-500/30
                  bg-gradient-to-b from-amber-500/15 to-amber-500/5
                  shadow-[0_0_0_1px_rgba(255,255,255,0.5)_inset,0_8px_16px_rgba(245,181,68,0.12)]
                  dark:shadow-[0_0_0_1px_rgba(0,0,0,0.4)_inset,0_8px_16px_rgba(245,181,68,0.08)]
                "
              >
                <LockIcon className="h-[17px] w-[17px] text-amber-600 dark:text-amber-300" />
              </div>
              <div className="flex-1 pt-0.5">
                <div className="text-[15px] font-bold tracking-[-0.01em] text-neutral-900 dark:text-neutral-100">
                  PASS+ 회차예요
                </div>
                <div className="mt-1.5 text-[13px] leading-[1.65] text-neutral-600 dark:text-neutral-400">
                  어려움 이상은 PASS+에서 풀 수 있어요. 3일권 3,900원부터, 한달권 9,900원.
                </div>
              </div>
            </div>

            {/* CTA */}
            <div className="mt-5 flex gap-2.5">
              <Link
                href={pricingHref}
                className="
                  flex-1 rounded-lg px-[18px] py-3
                  bg-gradient-to-b from-amber-300 to-amber-400
                  text-[13.5px] font-bold tracking-[-0.005em] text-amber-950
                  shadow-[0_1px_0_rgba(255,255,255,0.4)_inset,0_-1px_0_rgba(0,0,0,0.1)_inset,0_8px_20px_rgba(245,181,68,0.30)]
                  dark:shadow-[0_1px_0_rgba(255,255,255,0.3)_inset,0_-1px_0_rgba(0,0,0,0.15)_inset,0_8px_20px_rgba(245,181,68,0.25)]
                  text-center transition hover:brightness-105
                "
              >
                요금제 보기 →
              </Link>
              <Link
                href={backHref}
                className="
                  rounded-lg px-[18px] py-3
                  border border-neutral-300 dark:border-neutral-700
                  text-[13px] font-medium text-neutral-600 dark:text-neutral-400
                  transition hover:bg-neutral-100 dark:hover:bg-neutral-900
                "
              >
                목록으로
              </Link>
            </div>
          </div>

          {/* 메타 푸터 */}
          <div className="mt-7 grid grid-cols-3 border-t border-neutral-200 dark:border-neutral-800 pt-[18px]">
            {[
              ["문항", String(totalQuestions)],
              ["권장 시간", `${durationMin}분`],
              ["난이도", difficulty],
            ].map(([k, v], i) => (
              <div
                key={k}
                className={i === 0 ? "" : "border-l border-neutral-200 dark:border-neutral-800 pl-4"}
              >
                <div className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-neutral-500">
                  {k}
                </div>
                <div className="mt-1.5 text-sm font-semibold text-neutral-900 dark:text-neutral-100">
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

function PassPlusLabel({ size = "sm" }: { size?: "sm" | "md" }) {
  const sz = size === "md" ? "text-[12px] py-1 px-2.5" : "text-[10.5px] py-0.5 px-2";
  return (
    <span
      className={`
        inline-flex items-center gap-1.5 rounded
        border border-amber-500/40 bg-amber-500/10
        font-bold tracking-wide text-amber-700 dark:text-amber-300
        ${sz}
      `}
    >
      <span className="h-1 w-1 rounded-full bg-amber-500 dark:bg-amber-300 shadow-[0_0_8px_rgba(251,207,106,0.8)]" />
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
