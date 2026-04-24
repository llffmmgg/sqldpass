"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 시 localStorage 닉네임 + API 페치는 effect 내 setState 필요 */

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import {
  getOverallStats,
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
import StudyActivityChart from "@/components/StudyActivityChart";
import AdBanner from "@/components/AdBanner";
import { Container } from "@/components/ui";

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

type SubjectStat = { id: number; name: string; total: number; correct: number; rate: number };

function getSubjectStats(solves: SolveSummaryResponse[], subjectMap: Record<number, string>): SubjectStat[] {
  const map: Record<number, { name: string; total: number; correct: number }> = {};
  for (const s of solves) {
    if (s.subjectId == null) continue;
    const sid = s.subjectId;
    if (!map[sid]) {
      map[sid] = { name: subjectMap[sid] || `과목 ${sid}`, total: 0, correct: 0 };
    }
    map[sid].total += s.totalCount;
    map[sid].correct += s.correctCount;
  }
  return Object.entries(map).map(([id, data]) => ({
    id: Number(id),
    ...data,
    rate: data.total > 0 ? Math.round((data.correct / data.total) * 100) : 0,
  }));
}

type MergedSubject = {
  id: number;
  name: string;
  rate: number;
  correct: number;
  total: number;
  wrongCount: number;
  wrongRate: number;
};

/** subjectStats(정답률) + weakStats(오답수)를 한 행으로 합치고, 약한 과목 우선 정렬 */
function mergeSubjectStats(
  subjectStats: SubjectStat[],
  weakStats: WrongAnswerStatsResponse[]
): MergedSubject[] {
  const wrongById = new Map<number, WrongAnswerStatsResponse>();
  for (const w of weakStats) wrongById.set(w.subjectId, w);

  // 풀이가 있는 과목 + 오답만 있는 과목 모두 포함
  const ids = new Set<number>();
  for (const s of subjectStats) ids.add(s.id);
  for (const w of weakStats) if (w.wrongCount > 0) ids.add(w.subjectId);

  const merged: MergedSubject[] = [];
  for (const id of ids) {
    const s = subjectStats.find((x) => x.id === id);
    const w = wrongById.get(id);
    merged.push({
      id,
      name: s?.name ?? w?.subjectName ?? `과목 ${id}`,
      rate: s?.rate ?? 0,
      correct: s?.correct ?? 0,
      total: s?.total ?? 0,
      wrongCount: w?.wrongCount ?? 0,
      wrongRate: w?.wrongRate ?? 0,
    });
  }
  // 약한 순: 오답률 desc → 정답률 asc
  merged.sort((a, b) => {
    if (b.wrongRate !== a.wrongRate) return b.wrongRate - a.wrongRate;
    return a.rate - b.rate;
  });
  return merged;
}

function toLocalDateStr(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function getRecentActivity(solves: SolveSummaryResponse[]): { date: string; count: number }[] {
  const map: Record<string, number> = {};
  for (const s of solves) {
    const date = toLocalDateStr(new Date(s.solvedAt));
    map[date] = (map[date] || 0) + s.totalCount;
  }
  const result: { date: string; count: number }[] = [];
  const today = new Date();
  for (let i = 13; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const key = toLocalDateStr(d);
    result.push({ date: key, count: map[key] || 0 });
  }
  return result;
}

function rateColor(rate: number) {
  if (rate >= 80) return "text-green-400";
  if (rate >= 60) return "text-amber-400";
  return "text-red-400";
}

function rateBarColor(rate: number) {
  if (rate >= 80) return "bg-green-500";
  if (rate >= 60) return "bg-amber-500";
  return "bg-red-500";
}

function rateAccentBorder(rate: number) {
  if (rate >= 80) return "bg-green-500";
  if (rate >= 60) return "bg-amber-500";
  return "bg-red-500";
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
  const [overallAvg, setOverallAvg] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [nickname, setNickname] = useState<string | null>(null);

  useEffect(() => {
    setNickname(getNickname());
    Promise.all([getSolves(), getSubjects(), getWrongAnswerStats(), getOverallStats()])
      .then(([solvesData, subjects, stats, overall]) => {
        setSolves(solvesData);
        setSubjectMap(buildSubjectMap(subjects));
        setWeakStats(stats);
        setOverallAvg(overall.avgDailyCount);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading)
    return (
      <main className="min-h-screen bg-background flex items-center justify-center">
        <Spinner />
      </main>
    );

  const totalSolved = solves.reduce((acc, s) => acc + s.totalCount, 0);
  const totalCorrect = solves.reduce((acc, s) => acc + s.correctCount, 0);
  const overallRate = totalSolved > 0 ? Math.round((totalCorrect / totalSolved) * 100) : 0;
  const streak = getStreakDays(solves);
  const subjectStats = getSubjectStats(solves, subjectMap);
  const merged = mergeSubjectStats(subjectStats, weakStats);
  const activity = getRecentActivity(solves);

  // 가장 약한 과목 — 오답이 있는 것 중 첫 번째
  const focus = merged.find((m) => m.wrongCount > 0) ?? null;

  return (
    <main className="min-h-screen bg-background text-foreground">
      <Container size="default" className="py-12">
        {/* 헤더 */}
        <div className="flex items-center gap-5">
          <Image
            src="/dashboard-mascot.webp"
            alt="대시보드 마스코트"
            width={220}
            height={220}
            className="shrink-0"
            priority
          />
          <div>
            <h1 className="text-3xl font-bold sm:text-4xl">
              {nickname ? `${nickname}님의 학습 현황` : "학습 대시보드"}
            </h1>
            <p className="mt-1 text-sm text-muted">합격을 향한 여정을 한눈에 확인하세요.</p>
          </div>
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
              className="mt-6 inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-fg transition-colors hover:bg-primary-hover"
            >
              오늘의 학습 시작
            </Link>
          </div>
        )}

        {totalSolved > 0 && (
          <>
            {/* ── Today's Focus — 가장 큰 카드 ─────────────────────── */}
            <div className="mt-8 rounded-2xl border border-amber-500/20 bg-gradient-to-br from-amber-500/[0.08] via-amber-500/[0.04] to-transparent p-6 sm:p-7">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <p className="text-[11px] font-semibold uppercase tracking-wider text-amber-300">
                    오늘의 학습
                  </p>
                  {focus ? (
                    <>
                      <h2 className="mt-2 text-xl font-bold sm:text-2xl">
                        <span className="text-amber-300">{focus.name}</span> 부터 다시 풀어보세요
                      </h2>
                      <p className="mt-1 text-sm text-muted">
                        오답 {focus.wrongCount}개 · 오답률 {Math.round(focus.wrongRate)}%
                      </p>
                    </>
                  ) : (
                    <>
                      <h2 className="mt-2 text-xl font-bold sm:text-2xl">
                        오늘도 한 세트 풀어볼까요?
                      </h2>
                      <p className="mt-1 text-sm text-muted">오답이 모두 정리됐습니다. 새 문제로 실력을 더 다져보세요.</p>
                    </>
                  )}
                </div>
                <Link
                  href={focus ? `/wrong-answers?subjectId=${focus.id}` : "/solve"}
                  className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-fg transition-all hover:bg-primary-hover hover:scale-[1.02]"
                >
                  {focus ? "복습 시작" : "문제 풀기"}
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>
                </Link>
              </div>
            </div>

            {/* ── KPI — 보조 정보로 강등 (작은 패딩, 작은 폰트) ──────── */}
            <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4">
              <div className="rounded-lg border border-border bg-surface p-3">
                <p className="text-[11px] font-medium text-muted">총 풀이</p>
                <p className="mt-0.5 text-xl font-bold">
                  {totalSolved}
                  <span className="ml-1 text-xs font-normal text-muted">문제</span>
                </p>
              </div>
              <div className="rounded-lg border border-border bg-surface p-3">
                <p className="text-[11px] font-medium text-muted">정답률</p>
                <p className={`mt-0.5 text-xl font-bold ${rateColor(overallRate)}`}>
                  {overallRate}
                  <span className="ml-0.5 text-xs font-normal">%</span>
                </p>
              </div>
              <div className="rounded-lg border border-border bg-surface p-3">
                <p className="text-[11px] font-medium text-muted">연속 학습</p>
                <p className="mt-0.5 text-xl font-bold text-primary">
                  {streak}
                  <span className="ml-1 text-xs font-normal text-muted">일</span>
                </p>
              </div>
              <div className="rounded-lg border border-border bg-surface p-3">
                <p className="text-[11px] font-medium text-muted">풀이 세션</p>
                <p className="mt-0.5 text-xl font-bold">
                  {solves.length}
                  <span className="ml-1 text-xs font-normal text-muted">회</span>
                </p>
              </div>
            </div>

            <AdBanner
              desktopSlot={process.env.NEXT_PUBLIC_ADSENSE_SLOT_DASHBOARD_DESKTOP}
              desktopWidth={728}
              desktopHeight={90}
              mobileSlot={process.env.NEXT_PUBLIC_ADSENSE_SLOT_DASHBOARD_MOBILE}
            />

            {/* ── 최근 2주 학습량 ──────────────────────────────────── */}
            <StudyActivityChart data={activity} overallAvg={overallAvg} />

            {/* ── 과목별 학습 현황 (정답률 + 오답수 통합) ─────────────── */}
            <div className="mt-6 rounded-xl border border-border bg-surface p-5">
              <h2 className="text-sm font-semibold">과목별 학습 현황</h2>
              <p className="mt-1 text-xs text-muted/70">약한 순으로 정렬됩니다</p>
              <div className="mt-4 space-y-2.5">
                {merged.length === 0 && (
                  <p className="text-sm text-muted">데이터가 없습니다.</p>
                )}
                {merged.map((m, i) => {
                  const isTopWeak = i === 0 && m.wrongCount > 0;
                  return (
                    <div
                      key={m.id}
                      className={`relative flex items-center gap-3 rounded-lg px-3 py-2.5 transition-colors ${
                        isTopWeak ? "bg-red-500/[0.04]" : ""
                      }`}
                    >
                      {/* 좌측 컬러 바 */}
                      <span
                        className={`h-10 w-1 shrink-0 rounded-full ${rateAccentBorder(m.rate)}`}
                      />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <span className="truncate text-sm font-medium">{m.name}</span>
                          <span className={`shrink-0 text-sm font-semibold tabular-nums ${rateColor(m.rate)}`}>
                            {m.rate}%
                          </span>
                        </div>
                        <div className="mt-1.5 h-1 w-full rounded-full bg-border/50">
                          <div
                            className={`h-full rounded-full transition-all ${rateBarColor(m.rate)}`}
                            style={{ width: `${m.rate}%` }}
                          />
                        </div>
                        <div className="mt-1 flex items-center justify-between text-[11px] text-muted/70">
                          <span>
                            {m.correct}/{m.total} 정답
                            {m.wrongCount > 0 && <> · {m.wrongCount}개 오답</>}
                          </span>
                          {m.wrongCount > 0 && (
                            <Link
                              href={`/wrong-answers?subjectId=${m.id}`}
                              className="rounded text-amber-400/80 transition-colors hover:text-amber-300"
                            >
                              이 과목 복습 →
                            </Link>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* ── 최근 풀이 ──────────────────────────────────────── */}
            <div className="mt-6 rounded-xl border border-border bg-surface p-5">
              <h2 className="text-sm font-semibold">최근 풀이</h2>
              <div className="mt-4 space-y-2">
                {solves.slice(0, 5).map((solve) => {
                  const isMock = solve.mockExamId != null;
                  const label = isMock
                    ? `모의고사 #${solve.mockExamId}`
                    : solve.subjectId != null
                    ? subjectMap[solve.subjectId] || `과목 ${solve.subjectId}`
                    : "풀이";
                  return (
                    <Link
                      key={solve.id}
                      href={`/history/${solve.id}`}
                      className="flex items-center justify-between gap-3 rounded-lg px-3 py-2.5 transition-colors hover:bg-background"
                    >
                      <div className="flex min-w-0 items-center gap-2.5">
                        <span
                          className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-md ${
                            isMock ? "bg-violet-500/10 text-violet-400" : "bg-amber-500/10 text-amber-400"
                          }`}
                          aria-hidden="true"
                          title={isMock ? "모의고사" : "과목별"}
                        >
                          {isMock ? (
                            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
                              <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5.586a1 1 0 0 1 .707.293l5.414 5.414a1 1 0 0 1 .293.707V19a2 2 0 0 1-2 2z" />
                            </svg>
                          ) : (
                            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
                              <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.042A8.967 8.967 0 0 0 6 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 0 1 6 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 0 1 6-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0 0 18 18a8.967 8.967 0 0 0-6 2.292m0-14.25v14.25" />
                            </svg>
                          )}
                        </span>
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium">{label}</p>
                          <p className="text-xs text-muted">
                            {new Date(solve.solvedAt).toLocaleDateString("ko-KR")} ·{" "}
                            {solve.correctCount}/{solve.totalCount} 정답
                          </p>
                        </div>
                      </div>
                      <span className={`shrink-0 text-lg font-bold ${rateColor(solve.score)}`}>
                        {solve.score}
                        <span className="text-xs font-normal text-muted">점</span>
                      </span>
                    </Link>
                  );
                })}
              </div>
            </div>
          </>
        )}
      </Container>
    </main>
  );
}
