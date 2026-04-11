"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getMembers, type AdminMemberPage } from "@/lib/adminApi";
import { formatDateTime } from "@/lib/format";

export default function AdminMembersPage() {
  const [data, setData] = useState<AdminMemberPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    getMembers(page, 20)
      .then(setData)
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div>
      <h1 className="text-2xl font-bold">회원 관리</h1>

      {loading && <p className="mt-6 text-muted">로딩 중...</p>}

      {data && (
        <>
          <p className="mt-2 text-sm text-muted">총 {data.totalElements}명</p>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-muted">
                  <th className="px-3 py-2">ID</th>
                  <th className="px-3 py-2">닉네임</th>
                  <th className="px-3 py-2 text-right">풀이 수</th>
                  <th className="px-3 py-2 text-right">연속 접속</th>
                  <th className="px-3 py-2">이메일</th>
                  <th className="px-3 py-2">가입 방식</th>
                  <th className="px-3 py-2">가입일</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((m) => (
                  <tr
                    key={m.id}
                    className="border-b border-border/50 transition-colors hover:bg-surface/60"
                  >
                    <td className="px-3 py-2 text-muted">{m.id}</td>
                    <td className="px-3 py-2">
                      <Link
                        href={`/admin/members/${m.id}`}
                        className="font-medium text-amber-400 hover:underline"
                      >
                        {m.nickname}
                      </Link>
                    </td>
                    <td className="px-3 py-2 text-right tabular-nums">
                      {m.totalSolved > 0 ? (
                        <span className="font-semibold">{m.totalSolved.toLocaleString()}</span>
                      ) : (
                        <span className="text-muted/50">0</span>
                      )}
                    </td>
                    <td className="px-3 py-2 text-right tabular-nums">
                      {m.streakDays > 0 ? (
                        <span className="inline-flex items-center gap-0.5 font-semibold text-amber-300">
                          🔥 {m.streakDays}일
                        </span>
                      ) : (
                        <span className="text-muted/50">-</span>
                      )}
                    </td>
                    <td className="px-3 py-2 text-muted">{m.email || "-"}</td>
                    <td className="px-3 py-2">
                      <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs text-violet-400">
                        {m.provider}
                      </span>
                    </td>
                    <td className="px-3 py-2 text-muted">{formatDateTime(m.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-6 flex items-center justify-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded border border-border px-3 py-1 text-sm disabled:opacity-30"
            >
              이전
            </button>
            <span className="text-sm text-muted">
              {page + 1} / {data.totalPages || 1}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= data.totalPages - 1}
              className="rounded border border-border px-3 py-1 text-sm disabled:opacity-30"
            >
              다음
            </button>
          </div>
        </>
      )}
    </div>
  );
}
