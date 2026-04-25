"use client";

import { useEffect, useState } from "react";
import { getStats, type AdminStats, type AdminCertActivity } from "@/lib/adminApi";
import PageHeader from "@/components/admin/PageHeader";
import StatCard from "@/components/admin/StatCard";
import DataTable, { TableSkeleton } from "@/components/admin/DataTable";
import EmptyState from "@/components/admin/EmptyState";
import TrendChart from "@/components/admin/TrendChart";
import { CERT_TOKENS, type CertKey } from "@/lib/cert-tokens";

const SLUG_TO_CERT_KEY: Record<string, CertKey> = {
  sqld: "SQLD",
  engineer: "ENGINEER_PRACTICAL",
  "engineer-written": "ENGINEER_WRITTEN",
  "computer-literacy-1": "COMPUTER_LITERACY_1",
  "computer-literacy-2": "COMPUTER_LITERACY_2",
  adsp: "ADSP",
};

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
            <StatCard
              label="총 회원 수"
              value={stats.totalMembers}
              sub={`오늘 +${(stats.todayMembers ?? 0).toLocaleString()}`}
              tone="violet"
              icon={ICONS.members}
            />
            <StatCard
              label="전체 풀이 (회원+비회원)"
              value={stats.totalSolves + (stats.totalAnonymousSolves ?? 0)}
              sub={`회원 ${stats.totalSolves.toLocaleString()} · 비회원 ${(stats.totalAnonymousSolves ?? 0).toLocaleString()}`}
              tone="green"
              icon={ICONS.solves}
            />
            <StatCard
              label="오늘 풀이"
              value={(stats.todaySolves ?? 0) + (stats.todayAnonymousSolves ?? 0)}
              sub={`회원 ${(stats.todaySolves ?? 0).toLocaleString()} · 비회원 ${(stats.todayAnonymousSolves ?? 0).toLocaleString()}`}
              tone="blue"
              icon={ICONS.today}
            />
          </>
        )}
      </div>

      <section className="mt-8">
        <TrendChart />
      </section>

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

      <section className="mt-10">
        <div className="mb-4 flex items-baseline justify-between">
          <h2 className="text-base font-semibold">자격증별 풀이 활동</h2>
          <p className="text-xs text-muted">모의고사 / 기출 복원 분리 · 누적+오늘자</p>
        </div>

        {loading ? (
          <CertActivitySkeleton />
        ) : !stats?.certActivity || stats.certActivity.length === 0 ? (
          <EmptyState
            title="자격증별 활동이 아직 없어요"
            description="모의고사·기출 복원이 풀리면 여기에 표시됩니다."
          />
        ) : (
          <CertActivityGrid items={stats.certActivity} />
        )}
      </section>
    </div>
  );
}

function CertActivitySkeleton() {
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          className="h-40 animate-pulse rounded-lg border border-border bg-surface/40"
        />
      ))}
    </div>
  );
}

function CertActivityGrid({ items }: { items: AdminCertActivity[] }) {
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
      {items.map((item) => {
        const certKey = SLUG_TO_CERT_KEY[item.certSlug];
        const dotClass = certKey ? CERT_TOKENS[certKey].tailwind.dot : "bg-zinc-500";
        const textClass = certKey ? CERT_TOKENS[certKey].tailwind.text : "text-zinc-400";
        const todayUnique =
          item.mockExam.todayUniqueMembers + item.pastExam.todayUniqueMembers;
        return (
          <div
            key={item.certSlug}
            className="rounded-lg border border-border bg-surface/60 p-4"
          >
            <div className="flex items-center gap-2">
              <span className={`inline-block h-2 w-2 rounded-full ${dotClass}`} />
              <h3 className="text-sm font-semibold">{item.certName}</h3>
              <span className="ml-auto text-[10px] text-muted">
                오늘{" "}
                <span className="font-semibold tabular-nums text-foreground">
                  {todayUnique.toLocaleString()}
                </span>
                명
              </span>
            </div>

            <CertActivityRow label="모의고사" bucket={item.mockExam} accent={textClass} />
            <CertActivityRow label="기출 복원" bucket={item.pastExam} accent={textClass} />
          </div>
        );
      })}
    </div>
  );
}

function CertActivityRow({
  label,
  bucket,
  accent,
}: {
  label: string;
  bucket: AdminCertActivity["mockExam"];
  accent: string;
}) {
  return (
    <div className="mt-3 rounded-md border border-border bg-background/40 px-3 py-2 text-xs">
      <div className="flex items-center justify-between font-medium">
        <span className={accent}>{label}</span>
        <span className="tabular-nums text-muted">
          오늘 {bucket.todaySolves.toLocaleString()}회 ·{" "}
          {bucket.todayQuestions.toLocaleString()}문항
        </span>
      </div>
      <div className="mt-1 flex items-center justify-between text-muted">
        <span>
          누적{" "}
          <span className="font-semibold tabular-nums text-foreground">
            {bucket.uniqueMembers.toLocaleString()}
          </span>
          명
        </span>
        <span className="tabular-nums">
          {bucket.totalSolves.toLocaleString()}회 ·{" "}
          {bucket.totalQuestions.toLocaleString()}문항
        </span>
      </div>
    </div>
  );
}
