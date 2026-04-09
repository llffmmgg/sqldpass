"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 시 localStorage 기반 인증 체크는 hydration mismatch 회피를 위해 effect에서 setState 필요 (profile/page.tsx 와 동일 패턴) */

import { useEffect, useState } from "react";
import Link from "next/link";
import { isLoggedIn } from "@/lib/auth";
import { getMyFeedbacks, type FeedbackResponseDto, type FeedbackType, type FeedbackStatus } from "@/lib/feedbackApi";
import { formatDate } from "@/lib/format";
import LoginRequired from "@/components/LoginRequired";

const TYPE_LABEL: Record<FeedbackType, string> = {
  QUESTION_ERROR: "문제 오류",
  BUG: "사이트 버그",
  FEATURE: "기능 제안",
  OTHER: "기타",
};

const TYPE_CLASS: Record<FeedbackType, string> = {
  QUESTION_ERROR: "border-red-500/40 bg-red-500/10 text-red-300",
  BUG: "border-orange-500/40 bg-orange-500/10 text-orange-300",
  FEATURE: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  OTHER: "border-violet-500/40 bg-violet-500/10 text-violet-300",
};

const STATUS_LABEL: Record<FeedbackStatus, string> = {
  NEW: "접수됨",
  REVIEWED: "검토중",
  RESOLVED: "처리완료",
  WONTFIX: "반영 안 함",
};

const STATUS_CLASS: Record<FeedbackStatus, string> = {
  NEW: "border-zinc-500/40 bg-zinc-500/10 text-zinc-300",
  REVIEWED: "border-blue-500/40 bg-blue-500/10 text-blue-300",
  RESOLVED: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  WONTFIX: "border-rose-500/40 bg-rose-500/10 text-rose-300",
};

export default function MyFeedbackPage() {
  const [items, setItems] = useState<FeedbackResponseDto[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    if (!isLoggedIn()) return;
    getMyFeedbacks()
      .then(setItems)
      .catch((e) => setError(e instanceof Error ? e.message : "불러오기 실패"));
  }, []);

  if (!mounted) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <p className="text-muted">로딩 중…</p>
      </main>
    );
  }

  if (!isLoggedIn()) return <LoginRequired />;

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-2xl px-4 py-12">
        <h1 className="text-2xl font-bold">내 건의사항</h1>
        <p className="mt-1 text-sm text-muted">
          내가 보낸 건의/오류 신고와 어드민 답변을 확인할 수 있습니다.
        </p>

        {error && <p className="mt-6 text-sm text-rose-400">{error}</p>}

        {items && items.length === 0 && (
          <p className="mt-12 text-center text-muted">아직 보낸 건의사항이 없습니다.</p>
        )}

        {items && items.length > 0 && (
          <ul className="mt-8 space-y-3">
            {items.map((fb) => (
              <li key={fb.id} className="rounded-xl border border-border bg-surface p-5">
                <div className="flex flex-wrap items-center gap-2">
                  <span
                    className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-bold ${TYPE_CLASS[fb.type]}`}
                  >
                    {TYPE_LABEL[fb.type]}
                  </span>
                  <span
                    className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-bold ${STATUS_CLASS[fb.status]}`}
                  >
                    {STATUS_LABEL[fb.status]}
                  </span>
                  <span className="ml-auto text-xs text-muted/70">{formatDate(fb.createdAt)}</span>
                </div>

                <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-relaxed text-foreground/90">
                  {fb.content}
                </p>

                {fb.adminReply && (
                  <div className="mt-4 rounded-lg border border-emerald-500/20 bg-emerald-500/5 p-3">
                    <div className="text-[11px] font-semibold uppercase tracking-wide text-emerald-300">
                      운영진 답변 {fb.repliedAt && `· ${formatDate(fb.repliedAt)}`}
                    </div>
                    <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-emerald-100/90">
                      {fb.adminReply}
                    </p>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}

        <Link
          href="/"
          className="mt-8 inline-block text-sm text-muted hover:text-foreground"
        >
          ← 홈으로
        </Link>
      </div>
    </main>
  );
}
