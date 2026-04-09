"use client";

import { useEffect, useMemo, useState } from "react";

/**
 * 정보처리기사 실기 시험 D-day 카운트다운.
 *
 * 시험 일정은 큐넷 공고 기준. 새 회차가 발표되면 SCHEDULES 배열에 추가.
 * 현재 날짜 기준 가장 가까운 미래 회차를 자동 선택해서 표시한다.
 * 모든 회차가 지났으면 컴포넌트 자체가 렌더링되지 않음.
 */
type ExamSchedule = {
  /** 회차 라벨 (예: "2026년 정기 1회") */
  label: string;
  /** 시험 시작일 (KST) — 'YYYY-MM-DD' */
  date: string;
};

// 큐넷 공고 기준 — 정확한 일정은 큐넷에서 확인하고 갱신하세요.
const SCHEDULES: ExamSchedule[] = [
  { label: "2026년 정기 1회", date: "2026-04-25" },
  { label: "2026년 정기 2회", date: "2026-07-25" },
  { label: "2026년 정기 3회", date: "2026-10-31" },
];

function pickUpcoming(now: Date): ExamSchedule | null {
  const today = new Date(now);
  today.setHours(0, 0, 0, 0);
  for (const s of SCHEDULES) {
    const d = new Date(s.date + "T00:00:00+09:00");
    if (d.getTime() >= today.getTime()) return s;
  }
  return null;
}

function diffDays(target: Date, now: Date): number {
  const ms = target.getTime() - now.getTime();
  return Math.ceil(ms / (1000 * 60 * 60 * 24));
}

export default function EngineerExamCountdown() {
  const [now, setNow] = useState<Date | null>(null);

  useEffect(() => {
    setNow(new Date());
    // 1분마다 갱신 (자정 넘기면 D-day 한 칸 줄어듦)
    const t = setInterval(() => setNow(new Date()), 60_000);
    return () => clearInterval(t);
  }, []);

  const upcoming = useMemo(() => (now ? pickUpcoming(now) : null), [now]);

  if (!now || !upcoming) return null;

  const target = new Date(upcoming.date + "T00:00:00+09:00");
  const days = diffDays(target, now);
  const isToday = days === 0;
  const isUrgent = days <= 7 && days > 0;

  const targetDateLabel = target.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short",
  });

  return (
    <div className="mx-auto inline-flex max-w-full items-center gap-3 rounded-2xl border border-emerald-500/30 bg-emerald-500/[0.08] px-5 py-3 text-left shadow-[0_0_24px_rgba(16,185,129,0.12)]">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-300">
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5"
          />
        </svg>
      </div>
      <div className="min-w-0">
        <p className="text-xs font-medium uppercase tracking-wide text-emerald-300/80">
          정보처리기사 실기 · {upcoming.label}
        </p>
        <p className="mt-0.5 flex flex-wrap items-baseline gap-x-2 text-sm text-muted">
          <span
            className={`text-xl font-bold tabular-nums ${
              isToday ? "text-emerald-300" : isUrgent ? "text-amber-300" : "text-foreground"
            }`}
          >
            {isToday ? "D-DAY" : `D-${days}`}
          </span>
          <span className="text-xs text-muted/80">{targetDateLabel} 시작</span>
        </p>
      </div>
    </div>
  );
}
