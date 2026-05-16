"use client";

import { useState } from "react";
import Link from "next/link";

import { useToast } from "@/components/Toast";
import { isLoggedIn } from "@/lib/auth";
import { downloadAdspCramPdf, PdfDownloadError } from "@/lib/payment";
import { useSubscription } from "@/hooks/useSubscription";

export default function AdspCramHero() {
  const toast = useToast();
  const { subscription, loading } = useSubscription();
  const [busy, setBusy] = useState(false);

  const loggedIn = isLoggedIn();
  const canDownload = loggedIn && subscription.allowsPdf;

  async function handleDownload() {
    if (busy) return;
    setBusy(true);
    try {
      await downloadAdspCramPdf();
    } catch (e) {
      if (e instanceof PdfDownloadError && e.code === "PDF_REQUIRES_SUBSCRIPTION") {
        toast.show(
          "PDF 다운로드는 Thunder 이상 구독 회원 전용입니다. 우측 상단 프로필에서 업그레이드할 수 있어요.",
          "info",
        );
      } else {
        toast.show(
          e instanceof Error ? e.message : "PDF 다운로드에 실패했습니다.",
          "error",
        );
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="not-prose my-8 rounded-2xl border border-border bg-surface p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <span className="inline-flex items-center rounded-md border border-primary/30 bg-primary/15 px-2 py-0.5 text-[11px] font-semibold tracking-wide text-primary">
            Thunder 회원 전용
          </span>
          <h2 className="mt-3 text-xl font-bold leading-tight text-text sm:text-2xl">
            ADsP 마지막 정리본 PDF
          </h2>
          <p className="mt-2 text-sm leading-relaxed text-text-muted">
            시험 직전 30분, 한 번 더 훑고 들어가세요. 1·2페이지 미리보기는 아래에서 바로 확인할 수 있어요.
          </p>
        </div>

        <div className="shrink-0">
          {loading ? (
            <button
              disabled
              className="inline-flex items-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-semibold text-text-muted opacity-60"
            >
              확인 중...
            </button>
          ) : !loggedIn ? (
            <Link
              href="/login"
              className="inline-flex items-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-semibold text-text transition-colors hover:border-primary/40 hover:text-primary"
            >
              로그인하고 받기
            </Link>
          ) : !canDownload ? (
            <Link
              href="/checkout"
              className="inline-flex items-center gap-2 rounded-lg border border-primary/40 bg-primary/10 px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary/20"
            >
              Thunder 플랜 보기
            </Link>
          ) : (
            <button
              onClick={handleDownload}
              disabled={busy}
              className="inline-flex items-center gap-2 rounded-lg border border-primary bg-primary px-4 py-2 text-sm font-semibold text-[var(--primary-fg)] transition-colors hover:bg-primary-hover disabled:opacity-60"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M7 10l5 5 5-5M12 15V3"
                />
              </svg>
              {busy ? "PDF 준비 중..." : "전체 PDF 다운로드"}
            </button>
          )}
        </div>
      </div>

      {!loading && loggedIn && !canDownload && (
        <p className="mt-4 text-xs text-text-subtle">
          Thunder(3일) 이상 활성 구독자만 받을 수 있어요. Free 사용자는 아래 미리보기만 활용해 주세요.
        </p>
      )}
    </div>
  );
}
