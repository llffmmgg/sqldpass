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
              {card.value}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
