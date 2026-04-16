"use client";

import { useEffect, useState, use } from "react";
import Link from "next/link";
import {
  getMemberDashboard,
  getAdminSolveDetail,
  type AdminMemberDashboard,
  type AdminSolveDetail,
} from "@/lib/adminApi";
import { formatDateTime } from "@/lib/format";
import PageHeader from "@/components/admin/PageHeader";
import StatusBadge from "@/components/admin/StatusBadge";

export default function AdminMemberDetailPage({
  params,
}: {
  params: Promise<{ memberId: string }>;
}) {
  const { memberId } = use(params);
  const [data, setData] = useState<AdminMemberDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedSolveId, setExpandedSolveId] = useState<number | null>(null);
  const [solveDetail, setSolveDetail] = useState<AdminSolveDetail | null>(null);
  const [solveLoading, setSolveLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    getMemberDashboard(Number(memberId))
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [memberId]);

  async function handleSolveClick(solveId: number) {
    if (expandedSolveId === solveId) {
      setExpandedSolveId(null);
      setSolveDetail(null);
      return;
    }
    setExpandedSolveId(solveId);
    setSolveLoading(true);
    try {
      const detail = await getAdminSolveDetail(solveId);
      setSolveDetail(detail);
    } catch {
      setSolveDetail(null);
    } finally {
      setSolveLoading(false);
    }
  }

  if (loading) {
    return (
      <div>
        <PageHeader title="회원 상세" backHref="/admin/members" crumbs={[{ label: "회원 관리", href: "/admin/members" }, { label: "상세" }]} />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-[88px] animate-pulse rounded-xl border border-border bg-surface" />
          ))}
        </div>
        <div className="mt-6 h-48 animate-pulse rounded-xl border border-border bg-surface" />
      </div>
    );
  }
  if (error)
    return (
      <div>
        <PageHeader title="회원 상세" backHref="/admin/members" />
        <div className="rounded-xl border border-red-500/30 bg-red-500/5 p-5 text-sm text-red-400">
          {error}
        </div>
      </div>
    );
  if (!data) return null;

  const { member, stats, recentActivity, subjectStats, weakSubjects, recentSolves } = data;
  const maxActivity = Math.max(...recentActivity.map((a) => a.count), 1);

  return (
    <div>
      <PageHeader
        title={member.nickname}
        backHref="/admin/members"
        crumbs={[{ label: "회원 관리", href: "/admin/members" }, { label: member.nickname }]}
        actions={
          <>
            <span className="inline-flex items-center gap-1.5 text-xs text-muted">
              <span className="text-muted/60">ID</span>
              <span className="font-mono text-foreground">#{member.id}</span>
            </span>
            <StatusBadge tone="violet">{(member.provider || "").toLowerCase()}</StatusBadge>
            <span className="text-xs text-muted">
              가입 <span className="text-foreground">{formatDateTime(member.createdAt)}</span>
            </span>
          </>
        }
      />

      {/* 핵심 지표 — 풀이 수 + 연속 접속일을 가장 강조 */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <BigMetric label="누적 풀이" value={stats.totalSolved} unit="문제" highlight />
        <BigMetric label="연속 접속" value={stats.streakDays} unit="일" highlight />
        <BigMetric label="정답률" value={stats.overallRate} unit="%" colorByRate={stats.overallRate} />
        <BigMetric label="총 세션" value={stats.totalSessions} unit="회" />
      </div>

      {stats.totalSolved === 0 && (
        <div className="mt-12 rounded-xl border border-border bg-surface p-8 text-center">
          <p className="text-muted">아직 풀이 데이터가 없습니다.</p>
          <p className="mt-1 text-sm text-muted/60">이 사용자는 아직 문제를 풀지 않았어요.</p>
        </div>
      )}

      {stats.totalSolved > 0 && (
        <>
          {/* 최근 14일 활동 */}
          <section className="mt-8 rounded-xl border border-border bg-surface p-5">
            <h2 className="text-sm font-semibold text-muted">최근 14일 활동</h2>
            <div className="mt-4 flex h-32 items-end gap-1.5">
              {recentActivity.map((a, i) => {
                const heightPct = (a.count / maxActivity) * 100;
                return (
                  <div key={i} className="flex flex-1 flex-col items-center gap-1">
                    <div
                      className="w-full rounded-t bg-amber-500/40 transition-all hover:bg-amber-500/60"
                      style={{ height: `${heightPct}%`, minHeight: a.count > 0 ? "4px" : "0" }}
                      title={`${a.date}: ${a.count}문제`}
                    />
                  </div>
                );
              })}
            </div>
            <div className="mt-2 flex justify-between text-[10px] text-muted/60">
              <span>{recentActivity[0]?.date.slice(5)}</span>
              <span>{recentActivity[recentActivity.length - 1]?.date.slice(5)}</span>
            </div>
          </section>

          {/* 과목별 정답률 */}
          {subjectStats.length > 0 && (
            <section className="mt-6 rounded-xl border border-border bg-surface p-5">
              <h2 className="text-sm font-semibold text-muted">과목별 정답률</h2>
              <div className="mt-4 space-y-3">
                {subjectStats.map((s) => (
                  <div key={s.subjectId}>
                    <div className="flex items-center justify-between text-sm">
                      <span className="font-medium">{s.subjectName}</span>
                      <span className={`tabular-nums ${rateColor(s.rate)}`}>
                        {s.rate}% <span className="text-xs text-muted">({s.correct}/{s.total})</span>
                      </span>
                    </div>
                    <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-zinc-800">
                      <div
                        className={`h-full ${barColor(s.rate)}`}
                        style={{ width: `${s.rate}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* 취약 과목 */}
          {weakSubjects.length > 0 && (
            <section className="mt-6 rounded-xl border border-red-500/30 bg-red-500/5 p-5">
              <h2 className="text-sm font-semibold text-red-300">취약 과목 TOP 3</h2>
              <ul className="mt-3 space-y-2 text-sm">
                {weakSubjects.map((w, i) => (
                  <li key={w.subjectId} className="flex items-center justify-between">
                    <span>
                      <span className="text-red-400">{i + 1}.</span> {w.subjectName}
                    </span>
                    <span className="tabular-nums text-red-300">
                      오답률 {w.wrongRate}% <span className="text-xs text-muted">({w.wrongCount}건)</span>
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {/* 최근 풀이 */}
          {recentSolves.length > 0 && (
            <section className="mt-6 rounded-xl border border-border bg-surface p-5">
              <h2 className="text-sm font-semibold text-muted">최근 풀이 5건</h2>
              <ul className="mt-3 divide-y divide-border/60">
                {recentSolves.map((s) => {
                  const rate = s.totalCount > 0 ? Math.round((s.correctCount / s.totalCount) * 100) : 0;
                  const isExpanded = expandedSolveId === s.id;
                  return (
                    <li key={s.id}>
                      <button
                        onClick={() => handleSolveClick(s.id)}
                        className="flex w-full items-center justify-between py-2 text-sm text-left hover:bg-surface/50 rounded px-1 transition-colors"
                      >
                        <div>
                          <p className="font-medium">
                            {s.mockExamId ? `모의고사 #${s.mockExamId}` : `과목 풀이 #${s.subjectId}`}
                          </p>
                          <p className="text-xs text-muted">{formatDateTime(s.solvedAt)}</p>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={`tabular-nums ${rateColor(rate)}`}>
                            {s.correctCount}/{s.totalCount} <span className="text-xs">({rate}%)</span>
                          </span>
                          <svg
                            className={`h-3.5 w-3.5 text-muted/50 transition-transform ${isExpanded ? "rotate-180" : ""}`}
                            fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}
                          >
                            <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                          </svg>
                        </div>
                      </button>
                      {isExpanded && (
                        <div className="pb-3 pt-1">
                          {solveLoading ? (
                            <p className="text-xs text-muted py-2">로딩 중...</p>
                          ) : solveDetail ? (
                            <div className="space-y-2 max-h-96 overflow-y-auto">
                              {solveDetail.answers.map((a, idx) => (
                                <div
                                  key={a.questionId}
                                  className={`rounded-lg border px-3 py-2 text-xs ${
                                    a.correct ? "border-green-500/20 bg-green-500/5" : "border-red-500/20 bg-red-500/5"
                                  }`}
                                >
                                  <div className="flex items-center justify-between gap-2">
                                    <div className="flex items-center gap-2">
                                      <span className={`font-bold ${a.correct ? "text-green-400" : "text-red-400"}`}>
                                        #{idx + 1} {a.correct ? "O" : "X"}
                                      </span>
                                      <span className="text-muted">{a.subjectName}</span>
                                      <Link
                                        href={`/admin/questions/${a.questionId}`}
                                        className="text-violet-400 hover:text-violet-300"
                                        onClick={(e) => e.stopPropagation()}
                                      >
                                        ID:{a.questionId}
                                      </Link>
                                    </div>
                                  </div>
                                  <p className="mt-1 text-foreground/80 line-clamp-2">
                                    {a.questionContent.split("\n")[0]}
                                  </p>
                                  <div className="mt-1.5 flex items-center gap-3 text-[11px]">
                                    {a.questionType === "MCQ" ? (
                                      <>
                                        <span className={a.correct ? "text-green-400" : "text-red-400"}>
                                          선택: {a.selectedOption}번
                                        </span>
                                        {!a.correct && (
                                          <span className="text-green-400">정답: {a.correctOption}번</span>
                                        )}
                                      </>
                                    ) : (
                                      <>
                                        <span className={a.correct ? "text-green-400" : "text-red-400"}>
                                          답변: {a.userAnswerText || "(미입력)"}
                                        </span>
                                        {!a.correct && a.correctAnswer && (
                                          <span className="text-green-400">정답: {a.correctAnswer}</span>
                                        )}
                                      </>
                                    )}
                                  </div>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <p className="text-xs text-red-400 py-2">상세 조회 실패</p>
                          )}
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            </section>
          )}
        </>
      )}
    </div>
  );
}

function BigMetric({
  label,
  value,
  unit,
  highlight,
  colorByRate,
}: {
  label: string;
  value: number;
  unit: string;
  highlight?: boolean;
  colorByRate?: number;
}) {
  const valueColor = colorByRate != null ? rateColor(colorByRate) : highlight ? "text-amber-400" : "text-foreground";
  return (
    <div
      className={`rounded-xl border p-4 ${
        highlight ? "border-amber-500/30 bg-amber-500/5" : "border-border bg-surface"
      }`}
    >
      <p className="text-xs font-medium text-muted">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${valueColor}`}>
        {value}
        <span className="ml-1 text-sm font-normal text-muted">{unit}</span>
      </p>
    </div>
  );
}

function rateColor(rate: number): string {
  if (rate >= 80) return "text-green-400";
  if (rate >= 60) return "text-amber-400";
  return "text-red-400";
}

function barColor(rate: number): string {
  if (rate >= 80) return "bg-green-500";
  if (rate >= 60) return "bg-amber-500";
  return "bg-red-500";
}
