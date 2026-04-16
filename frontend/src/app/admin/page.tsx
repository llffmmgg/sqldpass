"use client";

import { useEffect, useState } from "react";
import { getStats, type AdminStats } from "@/lib/adminApi";
import PageHeader from "@/components/admin/PageHeader";
import StatCard from "@/components/admin/StatCard";
import DataTable, { TableSkeleton } from "@/components/admin/DataTable";
import EmptyState from "@/components/admin/EmptyState";

const ICON_SIZE = "h-4 w-4";

const ICONS = {
  questions: (
    <svg className={ICON_SIZE} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5.586a1 1 0 0 1 .707.293l5.414 5.414a1 1 0 0 1 .293.707V19a2 2 0 0 1-2 2z" />
    </svg>
  ),
  members: (
    <svg className={ICON_SIZE} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
    </svg>
  ),
  solves: (
    <svg className={ICON_SIZE} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
    </svg>
  ),
  today: (
    <svg className={ICON_SIZE} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5" />
    </svg>
  ),
};

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStats().then(setStats).finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <PageHeader
        title="대시보드"
        description="전체 서비스 지표와 과목별 사용 현황을 한눈에 확인하세요."
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {loading || !stats ? (
          <>
            {Array.from({ length: 4 }).map((_, i) => (
              <div
                key={i}
                className="h-[116px] animate-pulse rounded-xl border border-border bg-surface"
              />
            ))}
          </>
        ) : (
          <>
            <StatCard label="총 문제 수" value={stats.totalQuestions} tone="amber" icon={ICONS.questions} />
            <StatCard label="총 회원 수" value={stats.totalMembers} tone="violet" icon={ICONS.members} />
            <StatCard label="총 풀이 수" value={stats.totalSolves} tone="green" icon={ICONS.solves} />
            <StatCard label="오늘 생성된 문제" value={stats.todayQuestions} tone="blue" icon={ICONS.today} />
          </>
        )}
      </div>

      <section className="mt-10">
        <div className="mb-4 flex items-baseline justify-between">
          <h2 className="text-base font-semibold">과목별 풀이 현황</h2>
          <p className="text-xs text-muted">사용자 수 기준 정렬</p>
        </div>

        {loading ? (
          <DataTable>
            <DataTable.Head>
              <DataTable.HeadCell>과목</DataTable.HeadCell>
              <DataTable.HeadCell align="right">사용자 수</DataTable.HeadCell>
              <DataTable.HeadCell align="right">풀이 횟수</DataTable.HeadCell>
              <DataTable.HeadCell align="right">총 문제 수</DataTable.HeadCell>
              <DataTable.HeadCell align="right">인당 평균</DataTable.HeadCell>
            </DataTable.Head>
            <TableSkeleton cols={5} rows={5} />
          </DataTable>
        ) : !stats?.subjectStats || stats.subjectStats.length === 0 ? (
          <EmptyState
            title="과목별 통계가 아직 없어요"
            description="회원들이 문제를 풀기 시작하면 여기에 표시됩니다."
          />
        ) : (
          <DataTable maxHeight="520px">
            <DataTable.Head>
              <DataTable.HeadCell>과목</DataTable.HeadCell>
              <DataTable.HeadCell align="right">사용자 수</DataTable.HeadCell>
              <DataTable.HeadCell align="right">풀이 횟수</DataTable.HeadCell>
              <DataTable.HeadCell align="right">총 문제 수</DataTable.HeadCell>
              <DataTable.HeadCell align="right">인당 평균</DataTable.HeadCell>
            </DataTable.Head>
            <tbody>
              {stats.subjectStats.map((s) => (
                <DataTable.Row key={s.subjectId}>
                  <DataTable.Cell className="font-medium">{s.subjectName}</DataTable.Cell>
                  <DataTable.Cell align="right" mono className="text-violet-400">
                    {s.uniqueUsers.toLocaleString()}<span className="ml-0.5 text-xs text-muted">명</span>
                  </DataTable.Cell>
                  <DataTable.Cell align="right" mono>
                    {s.solveCount.toLocaleString()}<span className="ml-0.5 text-xs text-muted">회</span>
                  </DataTable.Cell>
                  <DataTable.Cell align="right" mono className="text-muted">
                    {s.totalQuestions.toLocaleString()}
                  </DataTable.Cell>
                  <DataTable.Cell align="right" mono className="text-muted">
                    {s.uniqueUsers > 0 ? (s.solveCount / s.uniqueUsers).toFixed(1) : "0"}
                  </DataTable.Cell>
                </DataTable.Row>
              ))}
            </tbody>
          </DataTable>
        )}
      </section>
    </div>
  );
}
