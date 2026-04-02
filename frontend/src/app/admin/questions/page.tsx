"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getQuestions, deleteQuestion, type AdminQuestion, type AdminQuestionPage } from "@/lib/adminApi";
import { formatDate } from "@/lib/format";

export default function AdminQuestionsPage() {
  const [data, setData] = useState<AdminQuestionPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  function load(p: number) {
    setLoading(true);
    getQuestions(p, 20)
      .then(setData)
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(page); }, [page]);

  async function handleDelete(id: number) {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    await deleteQuestion(id);
    load(page);
  }

  return (
    <div>
      <h1 className="text-2xl font-bold">문제 관리</h1>

      {loading && <p className="mt-6 text-muted">로딩 중...</p>}

      {data && (
        <>
          <p className="mt-2 text-sm text-muted">총 {data.totalElements}개</p>

          <div className="mt-4 space-y-2">
            {data.content.map((q) => (
              <div
                key={q.id}
                className="flex items-center justify-between rounded-lg border border-border bg-surface px-4 py-3"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-400">
                      {q.subjectName}
                    </span>
                    <span className="text-xs text-muted">{formatDate(q.createdAt)}</span>
                  </div>
                  <p className="mt-1 truncate text-sm">{q.content.split("\n")[0]}</p>
                  {q.summary && (
                    <p className="mt-0.5 text-xs text-muted">{q.summary}</p>
                  )}
                </div>
                <div className="ml-4 flex shrink-0 gap-2">
                  <Link
                    href={`/admin/questions/${q.id}`}
                    className="rounded border border-border px-3 py-1 text-xs text-muted transition hover:text-foreground"
                  >
                    수정
                  </Link>
                  <button
                    onClick={() => handleDelete(q.id)}
                    className="rounded border border-red-500/30 px-3 py-1 text-xs text-red-400 transition hover:bg-red-500/10"
                  >
                    삭제
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Pagination */}
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
