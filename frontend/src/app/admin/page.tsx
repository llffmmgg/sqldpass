"use client";

import { useEffect, useState } from "react";
import { getStats, type AdminStats } from "@/lib/adminApi";

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);

  useEffect(() => {
    getStats().then(setStats);
  }, []);

  if (!stats) {
    return <p className="text-muted">로딩 중...</p>;
  }

  const cards = [
    { label: "총 문제 수", value: stats.totalQuestions, color: "text-amber-400" },
    { label: "총 회원 수", value: stats.totalMembers, color: "text-violet-400" },
    { label: "총 풀이 수", value: stats.totalSolves, color: "text-green-400" },
    { label: "오늘 생성된 문제", value: stats.todayQuestions, color: "text-blue-400" },
  ];

  return (
    <div>
      <h1 className="text-2xl font-bold">대시보드</h1>

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((card) => (
          <div
            key={card.label}
            className="rounded-xl border border-border bg-surface p-6"
          >
            <p className="text-sm text-muted">{card.label}</p>
            <p className={`mt-2 text-3xl font-bold ${card.color}`}>
              {card.value.toLocaleString()}
            </p>
          </div>
        ))}
      </div>

      {/* 과목별 풀이 통계 */}
      {stats.subjectStats && stats.subjectStats.length > 0 && (
        <div className="mt-10">
          <h2 className="text-lg font-bold">과목별 풀이 현황</h2>
          <div className="mt-4 overflow-x-auto rounded-xl border border-border">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-surface/50 text-left text-xs text-muted">
                  <th className="px-4 py-3 font-medium">과목</th>
                  <th className="px-4 py-3 font-medium text-right">사용자 수</th>
                  <th className="px-4 py-3 font-medium text-right">풀이 횟수</th>
                  <th className="px-4 py-3 font-medium text-right">총 문제 수</th>
                  <th className="px-4 py-3 font-medium text-right">인당 평균</th>
                </tr>
              </thead>
              <tbody>
                {stats.subjectStats.map((s) => (
                  <tr key={s.subjectId} className="border-b border-border last:border-none hover:bg-surface/50">
                    <td className="px-4 py-3 font-medium">{s.subjectName}</td>
                    <td className="px-4 py-3 text-right tabular-nums text-violet-400">
                      {s.uniqueUsers.toLocaleString()}명
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      {s.solveCount.toLocaleString()}회
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums text-muted">
                      {s.totalQuestions.toLocaleString()}문제
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums text-muted">
                      {s.uniqueUsers > 0
                        ? (s.solveCount / s.uniqueUsers).toFixed(1)
                        : "0"}회
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
