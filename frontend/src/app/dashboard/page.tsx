"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 시 localStorage 닉네임 + API 페치는 effect 내 setState 필요 */

import Link from "next/link";
import { useEffect, useState } from "react";
import MascotImage from "@/components/mascot/MascotImage";
import MascotEmpty from "@/components/mascot/MascotEmpty";
import {
  getMyBestScores,
  getOverallStats,
  getSolves,
  getSubjects,
  getWrongAnswerStats,
  type BestScoreMap,
  type SolveSummaryResponse,
  type Subject,
  type WrongAnswerStatsResponse,
} from "@/lib/api";
import { getNickname } from "@/lib/auth";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";
import StudyActivityChart from "@/components/StudyActivityChart";
import AdResponsive from "@/components/AdResponsive";
import ExamGoalHero from "@/components/dashboard/ExamGoalHero";
import { inferActiveCerts } from "@/lib/dashboard/activeCerts";
import { Container } from "@/components/ui";
import {
  CERT_TOKENS,
  certFromRootName,
  type CertKey,
} from "@/lib/cert-tokens";

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

/**
 * leaf subject id → CertKey 매핑.
 * SQLD 만 root 가 "1과목: 데이터 모델링" / "2과목: SQL 기본/활용" 으로 나뉘는데
 * certFromRootName 이 SQLD 로 fallback 되므로 그대로 SQLD 매핑됨.
 */
function buildSubjectCertMap(subjects: Subject[]): Record<number, CertKey> {
  const map: Record<number, CertKey> = {};
  for (const root of subjects) {
    const cert = certFromRootName(root.name);
    map[root.id] = cert;
    for (const child of root.children) map[child.id] = cert;
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

function getRecentActivity(
  solves: SolveSummaryResponse[],
): { date: string; total: number; correct: number; wrong: number }[] {
  const map: Record<string, { total: number; correct: number }> = {};
  for (const s of solves) {
    const date = toLocalDateStr(new Date(s.solvedAt));
    const cur = map[date] ?? { total: 0, correct: 0 };
    cur.total += Number(s.totalCount) || 0;
    cur.correct += Number(s.correctCount) || 0;
    map[date] = cur;
  }
  const result: { date: string; total: number; correct: number; wrong: number }[] = [];
  const today = new Date();
  // 30일치로 빌드 — StudyActivityChart 가 7/14/30 토글로 슬라이스
  for (let i = 29; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const key = toLocalDateStr(d);
    const v = map[key] ?? { total: 0, correct: 0 };
    result.push({
      date: key,
      total: v.total,
      correct: v.correct,
      wrong: Math.max(0, v.total - v.correct),
    });
  }
  return result;
}

// Supabase 모노톤 — 정답률 신호색은 의미 있어 유지하되 토큰으로 매핑.
function rateColor(rate: number) {
  if (rate >= 80) return "text-primary";
  if (rate >= 60) return "text-warning";
  return "text-danger";
}

/** 최근 7일 vs 이전 7일 풀이수·정답률 delta — Plausible 식 KPI 변화 표시용. */
function computeWeekDeltas(solves: SolveSummaryResponse[]): {
  totalDelta: number;
  accuracyDelta: number;
} {
  const now = Date.now();
  const day = 86_400_000;
  let recentTotal = 0, recentCorrect = 0;
  let prevTotal = 0, prevCorrect = 0;
  for (const s of solves) {
    const at = new Date(s.solvedAt).getTime();
    const ago = (now - at) / day;
    if (ago < 7) {
      recentTotal += s.totalCount;
      recentCorrect += s.correctCount;
    } else if (ago < 14) {
      prevTotal += s.totalCount;
      prevCorrect += s.correctCount;
    }
  }
  const recentAcc = recentTotal > 0 ? (recentCorrect / recentTotal) * 100 : 0;
  const prevAcc = prevTotal > 0 ? (prevCorrect / prevTotal) * 100 : 0;
  return {
    totalDelta: recentTotal - prevTotal,
    accuracyDelta: Math.round((recentAcc - prevAcc) * 10) / 10,
  };
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
  const [subjectCertMap, setSubjectCertMap] = useState<Record<number, CertKey>>({});
  const [weakStats, setWeakStats] = useState<WrongAnswerStatsResponse[]>([]);
  const [overallAvg, setOverallAvg] = useState<number | undefined>(undefined);
  const [bestScores, setBestScores] = useState<BestScoreMap>({});
  const [loading, setLoading] = useState(true);
  const [nickname, setNickname] = useState<string | null>(null);

  useEffect(() => {
    setNickname(getNickname());
    // 라이브러리 권한 없는 회원에게 getWrongAnswerStats / getMyBestScores 가 403 — 다른 데이터까지
    // 막히지 않도록 allSettled 로 독립 처리. 권한 없는 응답은 빈 배열·객체로 폴백.
    Promise.allSettled([
      getSolves(),
      getSubjects(),
      getWrongAnswerStats(),
      getOverallStats(),
      getMyBestScores(),
    ]).then(([solvesRes, subjectsRes, statsRes, overallRes, bestScoresRes]) => {
      if (solvesRes.status === "fulfilled") setSolves(solvesRes.value);
      if (subjectsRes.status === "fulfilled") {
        setSubjectMap(buildSubjectMap(subjectsRes.value));
        setSubjectCertMap(buildSubjectCertMap(subjectsRes.value));
      }
      setWeakStats(statsRes.status === "fulfilled" ? statsRes.value : []);
      if (overallRes.status === "fulfilled") {
        setOverallAvg(overallRes.value.avgDailyCount);
      }
      if (bestScoresRes.status === "fulfilled") setBestScores(bestScoresRes.value);
    }).finally(() => setLoading(false));
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
  const activeCerts = inferActiveCerts(solves, subjectCertMap);
  const { totalDelta, accuracyDelta } = computeWeekDeltas(solves);

  // 약점 과목 TOP 5 — wrongCount 많은 순
  const topWeak = [...merged]
    .filter((m) => m.wrongCount > 0)
    .sort((a, b) => b.wrongCount - a.wrongCount)
    .slice(0, 5);

  // 회차 최고점 TOP 5 — 점수 높은 순
  const topMockExams = Object.entries(bestScores)
    .map(([id, v]) => ({
      id: Number(id),
      correct: v.correct,
      total: v.total,
      pct: v.total > 0 ? Math.round((v.correct / v.total) * 100) : 0,
    }))
    .sort((a, b) => b.pct - a.pct)
    .slice(0, 5);

  // 가장 약한 과목 — 오답이 있는 것 중 첫 번째
  const focus = merged.find((m) => m.wrongCount > 0) ?? null;

  return (
    <main className="min-h-screen bg-background text-foreground">
      <Container size="default" className="py-12">
        {/* 헤더 */}
        <div className="flex items-center gap-5">
          <MascotImage pose="analyze" size={180} priority className="shrink-0" />
          <div>
            <h1 className="text-3xl font-bold sm:text-4xl">
              {nickname ? `${nickname}님의 학습 현황` : "학습 대시보드"}
            </h1>
            <p className="mt-1 text-sm text-muted">풀었던 시험, 점수 변화, 오답 기록을 한눈에 확인해보세요.</p>
          </div>
        </div>

        {/* ── 시험 D-day Hero — 활성 자격증(풀이수 5건+) 있을 때만 표시 ─── */}
        {totalSolved > 0 && <ExamGoalHero activeCerts={activeCerts} />}

        {/* 빈 상태 */}
        {totalSolved === 0 && (
          <div className="mt-8">
            <MascotEmpty
              pose="guide"
              title="첫 회차를 풀어볼까요?"
              description={
                <>
                  첫 문제를 풀고 나면 여기에 학습 흐름과 약점 과목이
                  <br className="hidden sm:block" />
                  자동으로 정리됩니다. 한 번만 풀어도 첫 점이 찍혀요.
                </>
              }
              primaryCta={{ href: "/solve", label: "랜덤 모의고사 시작" }}
              secondaryCta={{ href: "/past-exams", label: "기출 복원 보기" }}
            />
          </div>
        )}

        {totalSolved > 0 && (
          <>
            {/* ── KPI row — Plausible 식 single row + 내부 divider + delta ─── */}
            <div className="mt-6 grid grid-cols-2 overflow-hidden rounded-xl border border-border bg-surface divide-x divide-y divide-border sm:grid-cols-4 sm:divide-y-0">
              <div className="px-5 py-4">
                <p className="text-[11px] font-medium uppercase tracking-wider text-text-subtle">총 풀이</p>
                <div className="mt-1.5 flex items-baseline gap-1.5">
                  <span className="text-2xl font-bold tabular-nums text-text sm:text-3xl">{totalSolved}</span>
                  <span className="text-xs text-text-muted">문제</span>
                  {totalDelta !== 0 && (
                    <span className={`ml-auto text-[11px] tabular-nums ${totalDelta > 0 ? "text-primary" : "text-danger"}`}>
                      {totalDelta > 0 ? "▲" : "▼"}{Math.abs(totalDelta)}
                    </span>
                  )}
                </div>
              </div>
              <div className="px-5 py-4">
                <p className="text-[11px] font-medium uppercase tracking-wider text-text-subtle">정답률</p>
                <div className="mt-1.5 flex items-baseline gap-1.5">
                  <span className={`text-2xl font-bold tabular-nums sm:text-3xl ${rateColor(overallRate)}`}>{overallRate}</span>
                  <span className="text-xs text-text-muted">%</span>
                  {accuracyDelta !== 0 && (
                    <span className={`ml-auto text-[11px] tabular-nums ${accuracyDelta > 0 ? "text-primary" : "text-danger"}`}>
                      {accuracyDelta > 0 ? "▲" : "▼"}{Math.abs(accuracyDelta)}%
                    </span>
                  )}
                </div>
              </div>
              <div className="px-5 py-4">
                <p className="text-[11px] font-medium uppercase tracking-wider text-text-subtle">연속 학습</p>
                <div className="mt-1.5 flex items-baseline gap-1.5">
                  <span className="text-2xl font-bold tabular-nums text-primary sm:text-3xl">{streak}</span>
                  <span className="text-xs text-text-muted">일</span>
                </div>
              </div>
              <div className="px-5 py-4">
                <p className="text-[11px] font-medium uppercase tracking-wider text-text-subtle">풀이 세션</p>
                <div className="mt-1.5 flex items-baseline gap-1.5">
                  <span className="text-2xl font-bold tabular-nums text-text sm:text-3xl">{solves.length}</span>
                  <span className="text-xs text-text-muted">회</span>
                </div>
              </div>
            </div>

            {/* ── 학습 활동 ──────────────────────────────────── */}
            <StudyActivityChart data={activity} overallAvg={overallAvg} />

            {/* ── 2-col 패널 그리드 — Plausible 식 데이터 슬라이스 ────── */}
            <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
              {/* 약점 과목 TOP 5 */}
              <div className="overflow-hidden rounded-xl border border-border bg-surface">
                <header className="flex items-center justify-between gap-2 border-b border-border px-5 py-3">
                  <h3 className="text-sm font-semibold">약점 과목 TOP 5</h3>
                  <Link href="/wrong-answers" className="text-xs text-text-muted hover:text-text">
                    전체 →
                  </Link>
                </header>
                {topWeak.length === 0 ? (
                  <p className="px-5 py-8 text-center text-sm text-text-muted">오답이 없어요 · 탄탄해요 ✓</p>
                ) : (
                  <ul className="divide-y divide-border">
                    {topWeak.map((m) => {
                      const cert = subjectCertMap[m.id];
                      const token = cert ? CERT_TOKENS[cert] : null;
                      return (
                        <li key={m.id}>
                          <Link
                            href={`/wrong-answers?subjectId=${m.id}`}
                            className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 px-5 py-3 transition-colors hover:bg-bg-elevated"
                          >
                            {token ? (
                              <span className={`h-1.5 w-1.5 rounded-full ${token.tailwind.dot}`} aria-hidden />
                            ) : (
                              <span className="h-1.5 w-1.5 rounded-full bg-text-subtle" aria-hidden />
                            )}
                            <div className="min-w-0">
                              <p className="truncate text-sm font-medium">{m.name}</p>
                              <p className="mt-0.5 text-[11px] text-text-subtle tabular-nums">
                                오답 {m.wrongCount} · 정답률 {m.rate}%
                              </p>
                            </div>
                            <span className="text-xs font-semibold text-danger tabular-nums">
                              ▼{Math.round(100 - m.rate)}%
                            </span>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>

              {/* 회차 최고점 TOP 5 */}
              <div className="overflow-hidden rounded-xl border border-border bg-surface">
                <header className="flex items-center justify-between gap-2 border-b border-border px-5 py-3">
                  <h3 className="text-sm font-semibold">회차 최고점 TOP 5</h3>
                  <Link href="/mock-exams" className="text-xs text-text-muted hover:text-text">
                    전체 →
                  </Link>
                </header>
                {topMockExams.length === 0 ? (
                  <p className="px-5 py-8 text-center text-sm text-text-muted">아직 응시한 모의고사 없음</p>
                ) : (
                  <ul className="divide-y divide-border">
                    {topMockExams.map((e) => {
                      const medal = e.pct >= 95 ? "🥇" : e.pct >= 85 ? "🥈" : e.pct >= 75 ? "🥉" : "·";
                      return (
                        <li key={e.id}>
                          <Link
                            href={`/mock-exams/${e.id}`}
                            className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 px-5 py-3 transition-colors hover:bg-bg-elevated"
                          >
                            <span className="w-4 text-center text-base" aria-hidden>{medal}</span>
                            <span className="text-sm font-medium tabular-nums">#{e.id}</span>
                            <span className={`text-sm font-bold tabular-nums ${rateColor(e.pct)}`}>
                              {e.pct}
                              <span className="ml-0.5 text-[10px] font-normal text-text-subtle">점</span>
                            </span>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>

              {/* 오늘의 권장 — Today's Focus 감증 */}
              <div className="overflow-hidden rounded-xl border border-border bg-surface">
                <header className="flex items-center justify-between gap-2 border-b border-border px-5 py-3">
                  <h3 className="text-sm font-semibold">오늘의 권장</h3>
                </header>
                <div className="px-5 py-4">
                  {focus ? (
                    <Link
                      href={`/wrong-answers?subjectId=${focus.id}`}
                      className="block rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 transition-colors hover:bg-primary/10"
                    >
                      <p className="text-sm">
                        <span className="font-semibold text-primary">{focus.name}</span> 부터 복습
                      </p>
                      <p className="mt-0.5 text-xs text-text-muted tabular-nums">
                        오답 {focus.wrongCount}개 · 정답률 {Math.round(focus.rate)}%
                      </p>
                    </Link>
                  ) : (
                    <Link
                      href="/solve"
                      className="block rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 transition-colors hover:bg-primary/10"
                    >
                      <p className="text-sm font-semibold text-primary">오늘도 한 세트 풀어볼까요?</p>
                      <p className="mt-0.5 text-xs text-text-muted">오답이 모두 정리됐어요. 새 문제로 실력 더 다지기</p>
                    </Link>
                  )}
                </div>
              </div>

              {/* 자격증 진행 — activeCerts 기반 */}
              <div className="overflow-hidden rounded-xl border border-border bg-surface">
                <header className="flex items-center justify-between gap-2 border-b border-border px-5 py-3">
                  <h3 className="text-sm font-semibold">자격증 진행</h3>
                </header>
                {activeCerts.length === 0 ? (
                  <p className="px-5 py-8 text-center text-sm text-text-muted">아직 응시한 자격증 없음</p>
                ) : (
                  <ul className="divide-y divide-border">
                    {activeCerts.map((a) => {
                      const token = CERT_TOKENS[a.cert];
                      return (
                        <li
                          key={a.cert}
                          className="grid grid-cols-[auto_minmax(0,1fr)_auto_auto] items-center gap-3 px-5 py-3"
                        >
                          <span className={`h-1.5 w-1.5 rounded-full ${token.tailwind.dot}`} aria-hidden />
                          <span className="truncate text-sm font-medium">{token.label}</span>
                          <div className="hidden h-1.5 w-24 overflow-hidden rounded-sm bg-bg-elevated sm:block">
                            <div
                              className="h-full bg-primary"
                              style={{ width: `${Math.min(100, a.recent5AvgScore)}%` }}
                            />
                          </div>
                          <span className={`text-xs font-semibold tabular-nums ${rateColor(a.recent5AvgScore)}`}>
                            {a.recent5AvgScore}점
                          </span>
                        </li>
                      );
                    })}
                  </ul>
                )}
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
                          className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-bg-elevated text-text-muted"
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

            {/* ── 페이지 하단 광고 ─────────────────────────────────── */}
            <div className="mt-8">
              <AdResponsive adSlot="4461637143" height={90} />
            </div>
          </>
        )}
      </Container>
    </main>
  );
}
