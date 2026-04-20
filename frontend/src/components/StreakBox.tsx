"use client";

import { useEffect, useState } from "react";
import { getMyStreak, type Streak } from "@/lib/streakApi";

export default function StreakBox() {
  const [streak, setStreak] = useState<Streak | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMyStreak()
      .then(setStreak)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="h-24 animate-pulse rounded-xl border border-border bg-surface/60" />;
  }
  if (!streak) return null;

  return (
    <div className="rounded-xl border border-border bg-surface/60 p-5">
      <div className="flex items-center gap-2 text-sm font-semibold">
        <span className="text-lg">🔥</span>
        <span>연속 학습</span>
      </div>
      <div className="mt-3 flex flex-wrap items-baseline gap-4">
        <div>
          <div className="text-[11px] text-muted">현재</div>
          <div className="text-3xl font-bold tabular-nums text-primary">
            {streak.currentStreak}
            <span className="ml-1 text-sm font-medium text-muted">일</span>
          </div>
        </div>
        <div>
          <div className="text-[11px] text-muted">최장</div>
          <div className="text-xl font-semibold tabular-nums">
            {streak.longestStreak}
            <span className="ml-1 text-xs font-medium text-muted">일</span>
          </div>
        </div>
        {streak.lastSolveDate && (
          <div className="ml-auto text-[11px] text-muted">
            마지막 풀이 <span className="tabular-nums">{streak.lastSolveDate}</span>
            {streak.solvedToday && (
              <span className="ml-2 rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-300">
                오늘 완료
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
