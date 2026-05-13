"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

import { getPublicMockExams } from "@/lib/publicApi";

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
  // 회차 실제 제목 fetch — getPublicMockExams 는 비로그인 공개 endpoint 라
  // 잠금 상태 사용자도 접근 가능. fetch 성공 시 blur 처리된 placeholder 제목
  // 자리에 실제 회차 이름 표시. 보안: blur 는 클라이언트 CSS 라 DevTools 로
  // 풀 수 있지만, 회차 제목 자체는 결제 동기 유발용으로 공개 허용 범위.
  const [realTitle, setRealTitle] = useState<string | null>(null);
  useEffect(() => {
    const numericId = Number(examNumber);
    if (!Number.isFinite(numericId)) return;
    let cancelled = false;
    getPublicMockExams()
      .then((rows) => {
        if (cancelled) return;
        const match = rows.find((r) => r.id === numericId);
        if (match) setRealTitle(match.name);
      })
      .catch(() => {
        // fallback — placeholder 제목 유지
      });
    return () => {
      cancelled = true;
    };
  }, [examNumber]);

  return (
    <div className="relative flex min-h-[80vh] items-center justify-center px-6 py-16">
      {/* 상단 미니 breadcrumb */}
      <div className="absolute left-7 top-6 hidden items-center gap-2 font-mono text-[11px] tracking-wide text-text-subtle sm:flex">
        <span>mock-exams</span>
        <span className="opacity-50">›</span>
        <span className="text-text-muted">{examNumber}</span>
      </div>

      {/* primary radial glow — 옅게 */}
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(62,207,142,0.07),transparent_60%)]" />

      <div className="relative w-full max-w-[540px]">
        <div className="relative overflow-hidden rounded-2xl border border-border bg-surface px-7 pt-7 pb-8 shadow-xl sm:px-9">
          {/* 메타 라인 */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <PassPlusLabel />
              <span className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-text-subtle">
                SQLD · {examNumber}회
              </span>
            </div>
            <span className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-text-subtle">
              HARD
            </span>
          </div>

          {/* 가려진 제목 — blur 강도를 낮춰 형체는 어렴풋이 인식되되 글자 내용은 못 읽게.
              하단 fade overlay 도 약하게 (via-surface/15) 로 — 블러만으로 가림 효과 충분. */}
          <div className="relative mt-5">
            <h2
              className="select-none text-[26px] font-bold leading-[1.3] tracking-[-0.01em] text-text"
              style={{ filter: "blur(4px)" }}
              aria-hidden
            >
              {realTitle ?? `SQLD 실전 모의고사 ${examNumber}회`}
            </h2>
            <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-transparent via-surface/15 to-surface/60" />
          </div>

          {/* 흐려진 본문 — 본문 bar 는 그대로 (글자 모양 placeholder) */}
          <div className="mt-4 flex flex-col gap-2.5" aria-hidden>
            {[100, 88, 92, 70].map((w, i) => (
              <div
                key={i}
                className="h-[7px] rounded-sm bg-text-muted/25"
                style={{ width: `${w}%`, filter: "blur(1.5px)" }}
              />
            ))}
          </div>

          {/* hairline */}
          <div className="mt-7 h-px bg-[linear-gradient(90deg,transparent,var(--border)_20%,var(--border)_80%,transparent)]" />

          {/* 안내 영역 */}
          <div className="mt-6">
            <div className="flex items-start gap-4">
              <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-[10px] border border-primary/40 bg-primary/10">
                <LockIcon className="h-[17px] w-[17px] text-primary" />
              </div>
              <div className="flex-1 pt-0.5">
                <div className="text-[15px] font-bold tracking-[-0.01em] text-text">
                  PASS+ 회차예요
                </div>
                <div className="mt-1.5 text-[13px] leading-[1.65] text-text-muted">
                  고난이도 회차는 PASS+에서 풀 수 있어요. Thunder 3,900원부터, Pro 9,900원.
                </div>
              </div>
            </div>

            {/* CTA */}
            <div className="mt-5 flex gap-2.5">
              <Link
                href={pricingHref}
                className="flex-1 rounded-lg bg-primary px-[18px] py-3 text-center text-[13.5px] font-bold tracking-[-0.005em] text-primary-fg shadow-sm transition-colors duration-200 hover:bg-primary-hover"
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
    <span className="inline-flex items-center gap-1.5 rounded border border-primary/40 bg-primary/10 px-2 py-0.5 text-[10.5px] font-bold tracking-wide text-primary">
      <span className="h-1 w-1 rounded-full bg-primary" />
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
