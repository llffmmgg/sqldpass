"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  getMembers,
  type AdminMemberOrder,
  type AdminMemberPage,
  type AdminMemberSort,
} from "@/lib/adminApi";
import { formatDateTime } from "@/lib/format";
import PageHeader from "@/components/admin/PageHeader";
import DataTable, { TableSkeleton } from "@/components/admin/DataTable";
import EmptyState from "@/components/admin/EmptyState";
import StatusBadge from "@/components/admin/StatusBadge";

const PAGE_SIZE = 50;

const PROVIDER_TONE: Record<string, "violet" | "blue" | "amber" | "neutral"> = {
  google: "blue",
  kakao: "amber",
  naver: "green" as "neutral",
  local: "violet",
};

function providerLabel(p: string) {
  return p?.toLowerCase() ?? "unknown";
}

export default function AdminMembersPage() {
  const [data, setData] = useState<AdminMemberPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [sortKey, setSortKey] = useState<AdminMemberSort>("default");
  const [sortOrder, setSortOrder] = useState<AdminMemberOrder>("desc");

  useEffect(() => {
    setLoading(true);
    getMembers(page, PAGE_SIZE, sortKey, sortOrder)
      .then(setData)
      .finally(() => setLoading(false));
  }, [page, sortKey, sortOrder]);

  const totalPages = data?.totalPages ?? 1;

  const toggleSort = (key: Exclude<AdminMemberSort, "default">) => {
    if (sortKey === key) {
      setSortOrder((prev) => (prev === "desc" ? "asc" : "desc"));
    } else {
      setSortKey(key);
      setSortOrder("desc");
    }
    setPage(0); // 정렬 변경 시 1페이지로
  };

  const sortableFor = (key: Exclude<AdminMemberSort, "default">) => ({
    active: sortKey === key,
    direction: sortOrder,
    onToggle: () => toggleSort(key),
  });

  return (
    <div>
      <PageHeader
        title="회원 관리"
        description={
          data
            ? `총 ${data.totalElements.toLocaleString()}명의 회원이 등록되어 있습니다.`
            : "회원 목록을 불러오는 중..."
        }
      />

      {loading ? (
        <DataTable>
          <DataTable.Head>
            <DataTable.HeadCell>ID</DataTable.HeadCell>
            <DataTable.HeadCell>닉네임</DataTable.HeadCell>
            <DataTable.HeadCell align="right">풀이 수</DataTable.HeadCell>
            <DataTable.HeadCell align="right">정답 수</DataTable.HeadCell>
            <DataTable.HeadCell align="right">푼 날</DataTable.HeadCell>
            <DataTable.HeadCell align="right">연속 접속</DataTable.HeadCell>
            <DataTable.HeadCell>이메일</DataTable.HeadCell>
            <DataTable.HeadCell>가입 방식</DataTable.HeadCell>
            <DataTable.HeadCell align="right">가입일</DataTable.HeadCell>
          </DataTable.Head>
          <TableSkeleton cols={9} rows={10} />
        </DataTable>
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title="회원이 없습니다"
          description="아직 가입한 회원이 없어요. 가입이 발생하면 여기에 표시됩니다."
          icon={
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.6}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
            </svg>
          }
        />
      ) : (
        <>
          <DataTable>
            <DataTable.Head>
              <DataTable.HeadCell>ID</DataTable.HeadCell>
              <DataTable.HeadCell>닉네임</DataTable.HeadCell>
              <DataTable.HeadCell align="right" sortable={sortableFor("totalSolved")}>
                풀이 수
              </DataTable.HeadCell>
              <DataTable.HeadCell align="right" sortable={sortableFor("totalCorrect")}>
                정답 수
              </DataTable.HeadCell>
              <DataTable.HeadCell align="right" sortable={sortableFor("activeDays")}>
                푼 날
              </DataTable.HeadCell>
              <DataTable.HeadCell align="right">연속 접속</DataTable.HeadCell>
              <DataTable.HeadCell>이메일</DataTable.HeadCell>
              <DataTable.HeadCell>가입 방식</DataTable.HeadCell>
              <DataTable.HeadCell align="right">가입일</DataTable.HeadCell>
            </DataTable.Head>
            <tbody>
              {data.content.map((m) => {
                const provider = providerLabel(m.provider);
                const tone = PROVIDER_TONE[provider] ?? "neutral";
                const initial = (m.nickname || "?").trim().charAt(0).toUpperCase();
                const rate =
                  m.totalSolved > 0
                    ? Math.round((m.totalCorrect / m.totalSolved) * 100)
                    : 0;
                return (
                  <DataTable.Row key={m.id}>
                    <DataTable.Cell mono className="text-muted">
                      #{m.id}
                    </DataTable.Cell>
                    <DataTable.Cell>
                      <Link
                        href={`/admin/members/${m.id}`}
                        className="inline-flex items-center gap-2.5 font-medium text-foreground transition-colors hover:text-primary"
                      >
                        <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                          {initial}
                        </span>
                        <span className="truncate">{m.nickname}</span>
                      </Link>
                    </DataTable.Cell>
                    <DataTable.Cell align="right" mono>
                      {m.totalSolved > 0 ? (
                        <span className="font-semibold">{m.totalSolved.toLocaleString()}</span>
                      ) : (
                        <span className="text-muted/50">0</span>
                      )}
                    </DataTable.Cell>
                    <DataTable.Cell align="right" mono>
                      {m.totalCorrect > 0 ? (
                        <span>
                          <span className="font-semibold text-green-400">
                            {m.totalCorrect.toLocaleString()}
                          </span>
                          {m.totalSolved > 0 && (
                            <span className="ml-1 text-[11px] text-muted/60">({rate}%)</span>
                          )}
                        </span>
                      ) : (
                        <span className="text-muted/50">0</span>
                      )}
                    </DataTable.Cell>
                    <DataTable.Cell align="right" mono>
                      {m.activeDays > 0 ? (
                        <span className="font-semibold">
                          {m.activeDays}
                          <span className="ml-0.5 text-[11px] text-muted/60">일</span>
                        </span>
                      ) : (
                        <span className="text-muted/50">—</span>
                      )}
                    </DataTable.Cell>
                    <DataTable.Cell align="right" mono>
                      {m.streakDays > 0 ? (
                        <span className="inline-flex items-center gap-1 font-semibold text-amber-400">
                          <svg className="h-3 w-3" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M12.75 6.75a1 1 0 0 1 1.7-.7A7.95 7.95 0 0 1 16 10c0 1.47-.57 2.78-1.5 3.78a3 3 0 1 1-5 0A7.95 7.95 0 0 1 8 10c0-1.47.57-2.78 1.5-3.78a1 1 0 0 1 1.7.7C11.4 9.1 12 10 12 10s.6-.9.75-3.25Z" />
                          </svg>
                          {m.streakDays}일
                        </span>
                      ) : (
                        <span className="text-muted/50">—</span>
                      )}
                    </DataTable.Cell>
                    <DataTable.Cell className="max-w-[220px] text-muted">
                      <span className="block truncate">{m.email || "—"}</span>
                    </DataTable.Cell>
                    <DataTable.Cell>
                      <StatusBadge tone={tone}>{provider}</StatusBadge>
                    </DataTable.Cell>
                    <DataTable.Cell align="right" mono className="text-xs text-muted">
                      {formatDateTime(m.createdAt)}
                    </DataTable.Cell>
                  </DataTable.Row>
                );
              })}
            </tbody>
          </DataTable>

          <div className="mt-6 flex items-center justify-between">
            <p className="text-xs tabular-nums text-muted">
              {data.content.length > 0 && (
                <>
                  <span className="text-foreground">
                    {page * PAGE_SIZE + 1}–{page * PAGE_SIZE + data.content.length}
                  </span>
                  <span className="mx-1">/</span>
                  {data.totalElements.toLocaleString()}
                </>
              )}
            </p>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="inline-flex h-8 items-center gap-1 rounded-md border border-border px-3 text-xs font-medium text-muted transition-colors hover:border-foreground/30 hover:text-foreground disabled:cursor-not-allowed disabled:opacity-30"
              >
                <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                </svg>
                이전
              </button>
              <span className="text-xs tabular-nums text-muted">
                <span className="text-foreground">{page + 1}</span>
                <span className="mx-1">/</span>
                {totalPages || 1}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
                className="inline-flex h-8 items-center gap-1 rounded-md border border-border px-3 text-xs font-medium text-muted transition-colors hover:border-foreground/30 hover:text-foreground disabled:cursor-not-allowed disabled:opacity-30"
              >
                다음
                <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
