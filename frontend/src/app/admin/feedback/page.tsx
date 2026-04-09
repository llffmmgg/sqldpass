"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  getFeedbacks,
  updateFeedbackStatus,
  replyFeedback,
  type AdminFeedback,
  type AdminFeedbackPage,
  type FeedbackStatus,
  type FeedbackType,
} from "@/lib/adminApi";
import { formatDate } from "@/lib/format";

type StatusFilter = FeedbackStatus | "ALL";

const STATUS_TABS: { value: StatusFilter; label: string }[] = [
  { value: "NEW", label: "신규" },
  { value: "REVIEWED", label: "검토됨" },
  { value: "RESOLVED", label: "처리완료" },
  { value: "WONTFIX", label: "거절" },
  { value: "ALL", label: "전체" },
];

const TYPE_LABEL: Record<FeedbackType, string> = {
  QUESTION_ERROR: "🐞 문제 오류",
  BUG: "🛠 사이트 버그",
  FEATURE: "💡 기능 제안",
  OTHER: "💬 기타",
};

const TYPE_CLASS: Record<FeedbackType, string> = {
  QUESTION_ERROR: "border-red-500/40 bg-red-500/10 text-red-300",
  BUG: "border-orange-500/40 bg-orange-500/10 text-orange-300",
  FEATURE: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  OTHER: "border-violet-500/40 bg-violet-500/10 text-violet-300",
};

export default function AdminFeedbackPage() {
  const [filter, setFilter] = useState<StatusFilter>("NEW");
  const [data, setData] = useState<AdminFeedbackPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [replyDrafts, setReplyDrafts] = useState<Record<number, string>>({});
  const [editingReplyId, setEditingReplyId] = useState<number | null>(null);
  const [replySavingId, setReplySavingId] = useState<number | null>(null);

  function applyUpdatedFeedback(updated: AdminFeedback, prevStatus: FeedbackStatus) {
    const droppedFromFilter = filter !== "ALL" && updated.status !== filter && updated.status !== prevStatus;
    setData((prev) => {
      if (!prev) return prev;
      if (droppedFromFilter) {
        return {
          ...prev,
          content: prev.content.filter((x) => x.id !== updated.id),
          totalElements: prev.totalElements - 1,
        };
      }
      return {
        ...prev,
        content: prev.content.map((x) => (x.id === updated.id ? updated : x)),
      };
    });
  }

  async function handleReplySave(fb: AdminFeedback) {
    const draft = (replyDrafts[fb.id] ?? fb.adminReply ?? "").trim();
    if (!draft) {
      alert("답변 내용을 입력해주세요.");
      return;
    }
    setReplySavingId(fb.id);
    try {
      const updated = await replyFeedback(fb.id, draft);
      applyUpdatedFeedback(updated, fb.status);
      setEditingReplyId(null);
      setReplyDrafts((prev) => {
        const next = { ...prev };
        delete next[fb.id];
        return next;
      });
    } catch (e) {
      alert(e instanceof Error ? e.message : "답변 저장 실패");
    } finally {
      setReplySavingId(null);
    }
  }

  useEffect(() => {
    setLoading(true);
    getFeedbacks(filter, page, 20)
      .then(setData)
      .finally(() => setLoading(false));
  }, [filter, page]);

  async function handleStatusChange(fb: AdminFeedback, status: FeedbackStatus) {
    if (status === fb.status) return;
    setUpdatingId(fb.id);
    try {
      await updateFeedbackStatus(fb.id, status);
      // 현재 필터에서 빠져야 하면 목록에서 제거
      if (filter !== "ALL" && status !== filter) {
        setData((prev) =>
          prev
            ? {
                ...prev,
                content: prev.content.filter((x) => x.id !== fb.id),
                totalElements: prev.totalElements - 1,
              }
            : prev,
        );
      } else {
        setData((prev) =>
          prev
            ? {
                ...prev,
                content: prev.content.map((x) => (x.id === fb.id ? { ...x, status } : x)),
              }
            : prev,
        );
      }
    } catch (e) {
      alert(e instanceof Error ? e.message : "상태 변경 실패");
    } finally {
      setUpdatingId(null);
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold">피드백 관리</h1>
      <p className="mt-1 text-sm text-muted">사용자 피드백/오류 신고를 확인하고 처리 상태를 갱신합니다.</p>

      {/* 상태 탭 */}
      <div className="mt-6 flex gap-2 border-b border-border">
        {STATUS_TABS.map((tab) => {
          const active = filter === tab.value;
          return (
            <button
              key={tab.value}
              onClick={() => {
                setFilter(tab.value);
                setPage(0);
              }}
              className={`relative -mb-px border-b-2 px-4 py-2 text-sm font-medium transition ${
                active
                  ? "border-amber-500 text-amber-300"
                  : "border-transparent text-muted hover:text-foreground"
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {loading && <p className="mt-6 text-muted">로딩 중...</p>}

      {data && data.content.length === 0 && (
        <p className="mt-12 text-center text-muted">해당 상태의 피드백이 없습니다.</p>
      )}

      {data && data.content.length > 0 && (
        <>
          <div className="mt-6 space-y-3">
            {data.content.map((fb) => (
              <div
                key={fb.id}
                className="rounded-xl border border-border bg-surface p-5"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted tabular-nums">#{fb.id}</span>
                    <span
                      className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-bold ${TYPE_CLASS[fb.type]}`}
                    >
                      {TYPE_LABEL[fb.type]}
                    </span>
                    <span className="text-xs text-muted">
                      {fb.memberNickname ?? "?"}
                    </span>
                    {fb.questionId && (
                      <Link
                        href={`/admin/questions/${fb.questionId}`}
                        className="text-xs text-amber-400 underline-offset-2 hover:underline"
                      >
                        문제 #{fb.questionId}
                      </Link>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted/70">{formatDate(fb.createdAt)}</span>
                    <select
                      value={fb.status}
                      onChange={(e) => handleStatusChange(fb, e.target.value as FeedbackStatus)}
                      disabled={updatingId === fb.id}
                      className="rounded-md border border-border bg-background px-2 py-1 text-xs"
                    >
                      <option value="NEW">신규</option>
                      <option value="REVIEWED">검토됨</option>
                      <option value="RESOLVED">처리완료</option>
                      <option value="WONTFIX">거절</option>
                    </select>
                  </div>
                </div>
                <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-relaxed">
                  {fb.content}
                </p>
                {fb.pageUrl && (
                  <p className="mt-2 text-[11px] text-muted/60">
                    페이지: <span className="font-mono">{fb.pageUrl}</span>
                  </p>
                )}

                {/* 어드민 답변 영역 */}
                <div className="mt-4 border-t border-border/60 pt-3">
                  {fb.adminReply && editingReplyId !== fb.id ? (
                    <div className="rounded-lg border border-emerald-500/20 bg-emerald-500/5 p-3">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-[11px] font-semibold uppercase tracking-wide text-emerald-300">
                          어드민 답변 {fb.repliedAt && `· ${formatDate(fb.repliedAt)}`}
                        </span>
                        <button
                          type="button"
                          onClick={() => {
                            setEditingReplyId(fb.id);
                            setReplyDrafts((prev) => ({ ...prev, [fb.id]: fb.adminReply ?? "" }));
                          }}
                          className="text-[11px] text-muted hover:text-foreground"
                        >
                          수정
                        </button>
                      </div>
                      <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-emerald-100/90">
                        {fb.adminReply}
                      </p>
                    </div>
                  ) : (
                    <div>
                      <label className="text-[11px] font-semibold uppercase tracking-wide text-muted">
                        답변 작성 (저장 시 자동 처리완료 + 작성자 알림)
                      </label>
                      <textarea
                        value={replyDrafts[fb.id] ?? fb.adminReply ?? ""}
                        onChange={(e) =>
                          setReplyDrafts((prev) => ({ ...prev, [fb.id]: e.target.value }))
                        }
                        rows={3}
                        maxLength={2000}
                        placeholder="사용자에게 전달할 답변을 입력하세요."
                        className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-amber-500/60 focus:outline-none focus:ring-2 focus:ring-amber-500/30"
                      />
                      <div className="mt-2 flex items-center justify-end gap-2">
                        {editingReplyId === fb.id && (
                          <button
                            type="button"
                            onClick={() => {
                              setEditingReplyId(null);
                              setReplyDrafts((prev) => {
                                const next = { ...prev };
                                delete next[fb.id];
                                return next;
                              });
                            }}
                            className="rounded-md border border-border bg-background px-3 py-1 text-xs text-muted hover:text-foreground"
                          >
                            취소
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => handleReplySave(fb)}
                          disabled={replySavingId === fb.id}
                          className="rounded-md bg-amber-500 px-3 py-1 text-xs font-semibold text-zinc-900 transition hover:bg-amber-400 disabled:opacity-50"
                        >
                          {replySavingId === fb.id ? "저장 중…" : "답변 저장"}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* 페이지네이션 */}
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
