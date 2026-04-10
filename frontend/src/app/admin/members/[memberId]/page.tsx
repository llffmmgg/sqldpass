"use client";

import { useEffect, useState, use } from "react";
import Link from "next/link";
import { getMemberDashboard, type AdminMemberDashboard } from "@/lib/adminApi";
import { formatDate } from "@/lib/format";

export default function AdminMemberDetailPage({
  params,
}: {
  params: Promise<{ memberId: string }>;
}) {
  const { memberId } = use(params);
  const [data, setData] = useState<AdminMemberDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    getMemberDashboard(Number(memberId))
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [memberId]);

  if (loading) return <p className="mt-6 text-muted">로딩 중...</p>;
  if (error)
    return (
      <div>
        <p className="text-red-400">{error}</p>
        <Link href="/admin/members" className="mt-4 inline-block text-sm text-muted hover:text-foreground">
          ← 멤버 목록으로
        </Link>
      </div>
    );
  if (!data) return null;

  const { member, stats, recentActivity, subjectStats, weakSubjects, recentSolves } = data;
  const maxActivity = Math.max(...recentActivity.map((a) => a.count), 1);

  return (
    <div>
      {/* 헤더 */}
      <div>
        <Link href="/admin/members" className="text-sm text-muted hover:text-foreground">
          ← 멤버 목록으로
        </Link>
        <h1 className="mt-2 text-2xl font-bold">{member.nickname}</h1>
        <div className="mt-1 flex items-center gap-3 text-sm text-muted">
          <span>ID #{member.id}</span>
          <span>·</span>
          <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs text-violet-400">
            {member.provider}
          </span>
          <span>·</span>
          <span>가입일 {formatDate(member.createdAt)}</span>
        </div>
      </div>

      {/* 핵심 지표 — 풀이 수 + 연속 접속일을 가장 강조 */}
      <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
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
                  return (
                    <li key={s.id} className="flex items-center justify-between py-2 text-sm">
                      <div>
                        <p className="font-medium">
                          {s.mockExamId ? (
                            <Link href={`/admin/mock-exams/${s.mockExamId}`} className="hover:text-violet-300 transition-colors">
                              모의고사 #{s.mockExamId}
                            </Link>
                          ) : (
                            `과목 풀이 #${s.subjectId}`
                          )}
                        </p>
                        <p className="text-xs text-muted">{formatDate(s.solvedAt)}</p>
                      </div>
                      <span className={`tabular-nums ${rateColor(rate)}`}>
                        {s.correctCount}/{s.totalCount} <span className="text-xs">({rate}%)</span>
                      </span>
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
