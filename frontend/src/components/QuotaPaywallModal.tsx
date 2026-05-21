"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui";
import {
  QUOTA_EXCEEDED_EVENT,
  type QuotaErrorCode,
  type QuotaExceededPayload,
} from "@/lib/quotaEvents";

// 전역 페이월 모달 — fetchApi 의 402 가드가 quota-exceeded 이벤트를 발행하면 표시.
//
// 글로벌 layout 에서 한 번만 마운트한다. 페이지 단위로 두지 않는 이유:
// 한도 초과는 detail 진입(GET /api/mock-exams/{id}) 등 어느 경로에서든 발생할 수 있어서
// 라우트별로 분산해 두면 누락 위험이 크다.
//
// 카피·CTA 는 step1.md 의 확정 표(DAILY_QUESTION_LIMIT / DAILY_MOCK_LIMIT) 그대로 사용.
// 단정 문구("유료 회원만 가능합니다") 금지, backdrop-blur / drop-shadow glow / opacity pulse 금지.

type Copy = {
  title: string;
  body: string;
  ctaLabel: string;
};

const COPY: Record<QuotaErrorCode, Copy> = {
  DAILY_QUESTION_LIMIT: {
    title: "오늘의 30문제 완주! 🐙",
    body: "오늘 무료 문제를 모두 풀었어요.\n플랜을 이용하면 매일 문제를 더 풀 수 있어요.",
    ctaLabel: "플랜 보기",
  },
  DAILY_MOCK_LIMIT: {
    title: "오늘 모의고사 1회 완료",
    body: "오늘 무료 모의고사를 모두 풀었어요.\n플랜을 이용하면 매일 모의고사를 더 풀 수 있어요.",
    ctaLabel: "플랜 보기",
  },
};

export default function QuotaPaywallModal() {
  const [info, setInfo] = useState<QuotaExceededPayload | null>(null);

  useEffect(() => {
    function onQuota(e: Event) {
      const ce = e as CustomEvent<QuotaExceededPayload>;
      if (!ce.detail) return;
      setInfo(ce.detail);
    }
    window.addEventListener(QUOTA_EXCEEDED_EVENT, onQuota as EventListener);
    return () => window.removeEventListener(QUOTA_EXCEEDED_EVENT, onQuota as EventListener);
  }, []);

  // Esc 로 닫기
  useEffect(() => {
    if (!info) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setInfo(null);
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [info]);

  const handleClose = useCallback(() => setInfo(null), []);

  if (!info) return null;

  const copy = COPY[info.error];

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="quota-paywall-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={handleClose}
    >
      <div
        className="w-full max-w-md rounded-2xl border border-primary/30 bg-surface p-6 text-sm leading-relaxed text-text shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="text-[11px] font-semibold uppercase tracking-wider text-primary">
          {info.error === "DAILY_MOCK_LIMIT" ? "오늘의 모의고사" : "오늘의 무료 풀이"}
        </p>
        <h2
          id="quota-paywall-title"
          className="mt-1.5 text-xl font-bold tracking-tight text-text"
        >
          {copy.title}
        </h2>
        <p className="mt-3 whitespace-pre-line text-sm text-text-muted">{copy.body}</p>

        <div className="mt-5 rounded-lg border border-border bg-bg px-4 py-3 text-xs text-text-muted">
          <div className="flex items-center justify-between">
            <span>오늘 사용</span>
            <span className="tabular-nums font-semibold text-text">
              {info.used} / {info.limit}
            </span>
          </div>
          <p className="mt-1.5 text-text">내일 0시에 다시 열려요</p>
        </div>

        {info.error === "DAILY_MOCK_LIMIT" && (
          <p className="mt-2 text-[11px] text-text-subtle">
            PASS+ 회차는 Thunder 부터
          </p>
        )}

        <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row">
          <Button
            variant="secondary"
            size="md"
            onClick={handleClose}
            className="flex-1"
          >
            내일 다시 오기
          </Button>
          <Link
            href="/checkout"
            onClick={handleClose}
            className="inline-flex flex-1 items-center justify-center rounded-sm bg-primary px-3.5 py-2 text-sm font-medium text-[var(--primary-fg)] transition-colors hover:bg-primary-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--primary-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--bg)]"
          >
            {copy.ctaLabel}
          </Link>
        </div>
      </div>
    </div>
  );
}
