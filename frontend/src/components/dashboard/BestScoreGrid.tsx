"use client";

import Link from "next/link";
import type { BestScoreMap } from "@/lib/api";

type Props = {
  bestScores: BestScoreMap;
};

type ScoredExam = {
  mockExamId: number;
  correct: number;
  total: number;
  pct: number;
  medal: "gold" | "silver" | "bronze" | "none";
};

function medalFor(pct: number): ScoredExam["medal"] {
  if (pct >= 95) return "gold";
  if (pct >= 85) return "silver";
  if (pct >= 75) return "bronze";
  return "none";
}

const MEDAL_META: Record<ScoredExam["medal"], { icon: string; label: string; ring: string; text: string }> = {
  gold: {
    icon: "🥇",
    label: "Gold",
    ring: "ring-amber-500/50",
    text: "text-amber-500",
  },
  silver: {
    icon: "🥈",
    label: "Silver",
    ring: "ring-silver/50",
    text: "text-silver",
  },
  bronze: {
    icon: "🥉",
    label: "Bronze",
    ring: "ring-orange-500/40",
    text: "text-orange-500",
  },
  none: {
    icon: "·",
    label: "도전",
    ring: "ring-border",
    text: "text-text-muted",
  },
};

export default function BestScoreGrid({ bestScores }: Props) {
  const scored: ScoredExam[] = Object.entries(bestScores).map(([id, { correct, total }]) => {
    const pct = total > 0 ? Math.round((correct / total) * 100) : 0;
    return {
      mockExamId: Number(id),
      correct,
      total,
      pct,
      medal: medalFor(pct),
    };
  });

  if (scored.length === 0) return null;

  // 점수 높은 순
  scored.sort((a, b) => b.pct - a.pct);

  const goldCount = scored.filter((s) => s.medal === "gold").length;
  const silverCount = scored.filter((s) => s.medal === "silver").length;
  const bronzeCount = scored.filter((s) => s.medal === "bronze").length;

  return (
    <section className="mt-6 overflow-hidden rounded-xl border border-border bg-surface p-5">
      <header className="flex flex-wrap items-baseline justify-between gap-2">
        <div>
          <h2 className="text-sm font-semibold">회차별 최고 점수</h2>
          <p className="mt-1 text-xs text-text-muted">
            응시한 모의고사 {scored.length}회 ·
            {goldCount > 0 && <span className="ml-1">🥇 {goldCount}</span>}
            {silverCount > 0 && <span className="ml-1">🥈 {silverCount}</span>}
            {bronzeCount > 0 && <span className="ml-1">🥉 {bronzeCount}</span>}
          </p>
        </div>
        <Link
          href="/mock-exams"
          className="text-xs text-text-muted hover:text-text"
        >
          전체 모의고사 보기 →
        </Link>
      </header>

      <ul className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
        {scored.map((s) => {
          const meta = MEDAL_META[s.medal];
          return (
            <li key={s.mockExamId}>
              <Link
                href={`/mock-exams/${s.mockExamId}`}
                className="group block rounded-lg border border-border bg-bg-elevated p-3 transition-colors hover:border-border-strong"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-medium text-text-muted">
                    #{s.mockExamId}
                  </span>
                  <span className={`text-base ${meta.text}`} aria-label={meta.label}>
                    {meta.icon}
                  </span>
                </div>
                <p className="mt-2 text-xl font-bold tabular-nums text-text">
                  {s.pct}
                  <span className="ml-0.5 text-xs font-normal text-text-subtle">점</span>
                </p>
                <p className="mt-0.5 text-[11px] text-text-subtle tabular-nums">
                  {s.correct}/{s.total} 정답
                </p>
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
