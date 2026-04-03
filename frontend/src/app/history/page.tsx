"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getSolves, getSubjects, type SolveSummaryResponse, type Subject } from "@/lib/api";
import { formatDate } from "@/lib/format";

function buildSubjectMap(subjects: Subject[]): Record<number, string> {
  const map: Record<number, string> = {};
  for (const s of subjects) {
    map[s.id] = s.name;
    for (const child of s.children) {
      map[child.id] = child.name;
    }
  }
  return map;
}

export default function HistoryPage() {
  const [solves, setSolves] = useState<SolveSummaryResponse[]>([]);
  const [subjectMap, setSubjectMap] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getSolves(), getSubjects()])
      .then(([solvesData, subjects]) => {
        setSolves(solvesData);
        setSubjectMap(buildSubjectMap(subjects));
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        <h1 className="text-2xl font-bold sm:text-3xl">풀이 기록</h1>
        <p className="mt-2 text-sm text-muted">지금까지의 학습 이력을 확인하세요.</p>

        <div className="mt-8 space-y-3">
          {loading && (
            <p className="py-12 text-center text-muted">로딩 중...</p>
          )}

          {!loading && solves.length === 0 && (
            <div className="py-16 text-center">
              <p className="text-muted">아직 풀이 기록이 없습니다.</p>
              <Link
                href="/solve"
                className="mt-4 inline-block rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-zinc-900 transition-colors hover:bg-primary-hover"
              >
                문제 풀러 가기
              </Link>
            </div>
          )}

          {solves.map((solve) => (
            <Link
              key={solve.id}
              href={`/history/${solve.id}`}
              className="block rounded-lg border border-border bg-surface px-5 py-4 transition-all duration-300 hover:-translate-y-0.5 hover:border-amber-500/40"
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">
                    {subjectMap[solve.subjectId] || `과목 ${solve.subjectId}`}
                  </p>
                  <p className="mt-1 text-sm text-muted">
                    {formatDate(solve.solvedAt)} &middot; {solve.correctCount}/{solve.totalCount} 정답
                  </p>
                </div>
                <div className="text-right">
                  <span className="text-2xl font-bold bg-gradient-to-r from-amber-400 to-amber-300 bg-clip-text text-transparent">
                    {solve.score}
                  </span>
                  <span className="ml-1 text-sm text-muted">점</span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </main>
  );
}
