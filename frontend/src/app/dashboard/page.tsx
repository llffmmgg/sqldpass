"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  getSolves,
  getSubjects,
  getWrongAnswerStats,
  type SolveSummaryResponse,
  type Subject,
  type WrongAnswerStatsResponse,
} from "@/lib/api";
import { getNickname } from "@/lib/auth";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";

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

function getStreakDays(solves: SolveSummaryResponse[]): number {
  if (solves.length === 0) return 0;
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const dates = new Set(
    solves.map((s) => {
      const d = new Date(s.solvedAt);
      d.setHours(0, 0, 0, 0);
      return d.getTime();
    })
  );

  let streak = 0;
  const day = new Date(today);
  while (dates.has(day.getTime())) {
    streak++;
    day.setDate(day.getDate() - 1);
  }
  return streak;
}

function getSubjectStats(solves: SolveSummaryResponse[], subjectMap: Record<number, string>) {
  const map: Record<number, { name: string; total: number; correct: number }> = {};
  for (const s of solves) {
    // 모의고사 풀이는 과목별 통계에서 제외 (subjectId가 null)
    if (s.subjectId == null) continue;
    const sid = s.subjectId;
    if (!map[sid]) {
      map[sid] = { name: subjectMap[sid] || `과목 ${sid}`, total: 0, correct: 0 };
    }
    map[sid].total += s.totalCount;
    map[sid].correct += s.correctCount;
  }
  return Object.entries(map)
    .map(([id, data]) => ({ id: Number(id), ...data, rate: data.total > 0 ? Math.round((data.correct / data.total) * 100) : 0 }))
    .sort((a, b) => b.total - a.total);
}

function getRecentActivity(solves: SolveSummaryResponse[]): { date: string; count: number }[] {
  const map: Record<string, number> = {};
  for (const s of solves) {
    const date = new Date(s.solvedAt).toISOString().slice(0, 10);
    map[date] = (map[date] || 0) + s.totalCount;
  }
  // 최근 14일
  const result: { date: string; count: number }[] = [];
  const today = new Date();
  for (let i = 13; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    result.push({ date: key, count: map[key] || 0 });
  }
  return result;
}

function rateColor(rate: number) {
  if (rate >= 80) return "text-green-400";
  if (rate >= 60) return "text-amber-400";
  return "text-red-400";
}

export default function DashboardPage() {
  return (
    <AuthGuard>
      <DashboardPageContent />
    </AuthGuard>
  );
}

function DashboardPageContent() {
  const [solves, setSolves] = useState<SolveSummaryResponse[]>([]);
  const [subjectMap, setSubjectMap] = useState<Record<number, string>>({});
  const [weakStats, setWeakStats] = useState<WrongAnswerStatsResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [nickname, setNickname] = useState<string | null>(null);

  useEffect(() => {
    setNickname(getNickname());
    Promise.all([getSolves(), getSubjects(), getWrongAnswerStats()])
      .then(([solvesData, subjects, stats]) => {
        setSolves(solvesData);
        setSubjectMap(buildSubjectMap(subjects));
        setWeakStats(stats);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <main className="min-h-screen bg-background flex items-center justify-center"><Spinner /></main>;

  const totalSolved = solves.reduce((acc, s) => acc + s.totalCount, 0);
  const totalCorrect = solves.reduce((acc, s) => acc + s.correctCount, 0);
  const overallRate = totalSolved > 0 ? Math.round((totalCorrect / totalSolved) * 100) : 0;
  const streak = getStreakDays(solves);
  const subjectStats = getSubjectStats(solves, subjectMap);
  const activity = getRecentActivity(solves);
  const maxActivity = Math.max(...activity.map((a) => a.count), 1);

  // 취약 과목 top 3
  const weakSubjects = [...weakStats]
    .filter((s) => s.wrongCount > 0)
    .sort((a, b) => b.wrongRate - a.wrongRate)
    .slice(0, 3);

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6">
        {/* 헤더 */}
        <div>
          <h1 className="text-2xl font-bold sm:text-3xl">
            {nickname ? `${nickname}님의 학습 현황` : "학습 대시보드"}
          </h1>
          <p className="mt-1 text-sm text-muted">SQLD 합격을 향한 여정을 한눈에 확인하세요.</p>
        </div>

        {/* 빈 상태 */}
        {totalSolved === 0 && (
          <div className="mt-12 py-16 text-center">
            <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-2xl bg-surface border border-border">
              <svg className="h-9 w-9 text-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" />
              </svg>
            </div>
            <p className="mt-4 text-muted">아직 학습 데이터가 없습니다.</p>
            <p className="mt-1 text-sm text-muted/60">문제를 풀면 여기에 통계가 쌓여요.</p>
            <Link
              href="/solve"
              className="mt-6 inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-zinc-900 transition-colors hover:bg-primary-hover"
            >
              문제 풀러 가기
            </Link>
          </div>
        )}

        {totalSolved > 0 && (
          <>
            {/* 핵심 지표 카드 */}
            <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
              <div className="rounded-xl border border-border bg-surface p-4">
                <p className="text-xs font-medium text-muted">총 풀이</p>
                <p className="mt-1 text-2xl font-bold">{totalSolved}<span className="ml-1 text-sm font-normal text-muted">문제</span></p>
              </div>
              <div className="rounded-xl border border-border bg-surface p-4">
                <p className="text-xs font-medium text-muted">정답률</p>
                <p className={`mt-1 text-2xl font-bold ${rateColor(overallRate)}`}>{overallRate}<span className="ml-0.5 text-sm font-normal">%</span></p>
              </div>
              <div className="rounded-xl border border-border bg-surface p-4">
                <p className="text-xs font-medium text-muted">연속 학습</p>
                <p className="mt-1 text-2xl font-bold text-primary">{streak}<span className="ml-1 text-sm font-normal text-muted">일</span></p>
              </div>
              <div className="rounded-xl border border-border bg-surface p-4">
                <p className="text-xs font-medium text-muted">풀이 세션</p>
                <p className="mt-1 text-2xl font-bold">{solves.length}<span className="ml-1 text-sm font-normal text-muted">회</span></p>
              </div>
            </div>

            {/* 최근 14일 활동 */}
            <div className="mt-6 rounded-xl border border-border bg-surface p-5">
              <h2 className="text-sm font-semibold">최근 2주 학습량</h2>
              <div className="mt-4 flex items-end gap-1.5 h-24">
                {activity.map((day) => {
                  const height = day.count > 0 ? Math.max((day.count / maxActivity) * 100, 8) : 4;
                  const isToday = day.date === new Date().toISOString().slice(0, 10);
                  return (
                    <div key={day.date} className="flex flex-1 flex-col items-center gap-1">
                      <div
                        className={`w-full rounded-sm transition-all ${
                          day.count > 0
                            ? isToday ? "bg-primary" : "bg-primary/50"
                            : "bg-border/50"
                        }`}
                        style={{ height: `${height}%` }}
                        title={`${day.date}: ${day.count}문제`}
                      />
                    </div>
                  );
                })}
              </div>
              <div className="mt-2 flex justify-between text-[10px] text-muted/60">
                <span>{activity[0]?.date.slice(5)}</span>
                <span>오늘</span>
              </div>
            </div>

            {/* 과목별 정답률 + 취약 과목 */}
            <div className="mt-6 grid gap-6 sm:grid-cols-2">
              {/* 과목별 정답률 */}
              <div className="rounded-xl border border-border bg-surface p-5">
                <h2 className="text-sm font-semibold">과목별 정답률</h2>
                <div className="mt-4 space-y-3">
                  {subjectStats.map((s) => (
                    <div key={s.id}>
                      <div className="flex items-center justify-between text-sm">
                        <span className="truncate">{s.name}</span>
                        <span className={`ml-2 font-semibold ${rateColor(s.rate)}`}>{s.rate}%</span>
                      </div>
                      <div className="mt-1 h-1.5 w-full rounded-full bg-border/50">
                        <div
                          className={`h-full rounded-full transition-all ${
                            s.rate >= 80 ? "bg-green-500" : s.rate >= 60 ? "bg-amber-500" : "bg-red-500"
                          }`}
                          style={{ width: `${s.rate}%` }}
                        />
                      </div>
                      <p className="mt-0.5 text-[11px] text-muted/60">{s.correct}/{s.total} 정답</p>
                    </div>
                  ))}
                  {subjectStats.length === 0 && (
                    <p className="text-sm text-muted">데이터가 없습니다.</p>
                  )}
                </div>
              </div>

              {/* 취약 과목 */}
              <div className="rounded-xl border border-border bg-surface p-5">
                <h2 className="text-sm font-semibold">취약 영역 TOP 3</h2>
                <div className="mt-4 space-y-3">
                  {weakSubjects.map((s, i) => (
                    <div key={s.subjectId} className="flex items-start gap-3">
                      <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                        i === 0 ? "bg-red-500/15 text-red-400" : "bg-surface border border-border text-muted"
                      }`}>
                        {i + 1}
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium truncate">{s.subjectName}</p>
                        <p className="text-xs text-muted">
                          오답 {s.wrongCount}회 &middot; 오답률 {Math.round(s.wrongRate)}%
                        </p>
                      </div>
                      <Link
                        href={`/solve`}
                        className="shrink-0 rounded-md border border-border px-2.5 py-1 text-xs text-muted transition-colors hover:border-primary/40 hover:text-primary"
                      >
                        학습하기
                      </Link>
                    </div>
                  ))}
                  {weakSubjects.length === 0 && (
                    <p className="text-sm text-muted">오답 데이터가 없습니다.</p>
                  )}
                </div>
              </div>
            </div>

            {/* 최근 풀이 기록 */}
            <div className="mt-6 rounded-xl border border-border bg-surface p-5">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-semibold">최근 풀이</h2>
              </div>
              <div className="mt-4 space-y-2">
                {solves.slice(0, 5).map((solve) => (
                  <Link
                    key={solve.id}
                    href={`/history/${solve.id}`}
                    className="flex items-center justify-between rounded-lg px-3 py-2.5 transition-colors hover:bg-background"
                  >
                    <div className="min-w-0">
                      <p className="text-sm font-medium truncate">
                        {solve.mockExamId != null
                          ? `모의고사 #${solve.mockExamId}`
                          : solve.subjectId != null
                          ? subjectMap[solve.subjectId] || `과목 ${solve.subjectId}`
                          : "풀이"}
                      </p>
                      <p className="text-xs text-muted">
                        {new Date(solve.solvedAt).toLocaleDateString("ko-KR")} &middot; {solve.correctCount}/{solve.totalCount} 정답
                      </p>
                    </div>
                    <span className={`text-lg font-bold ${rateColor(solve.score)}`}>
                      {solve.score}<span className="text-xs font-normal text-muted">점</span>
                    </span>
                  </Link>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </main>
  );
}
