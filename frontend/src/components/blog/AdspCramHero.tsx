"use client";

import { useState } from "react";
import Link from "next/link";

import { useToast } from "@/components/Toast";
import { isLoggedIn } from "@/lib/auth";
import { downloadAdspCramPdf, PdfDownloadError } from "@/lib/payment";
import { useSubscription } from "@/hooks/useSubscription";

const PRIMARY_BTN =
  "inline-flex items-center gap-2 rounded-lg border border-primary bg-primary px-4 py-2 text-sm font-semibold text-[var(--primary-fg)] transition-colors hover:bg-primary-hover disabled:opacity-60";

const OUTLINE_BTN =
  "inline-flex items-center gap-2 rounded-lg border border-border bg-surface px-4 py-2 text-sm font-semibold text-text transition-colors hover:border-primary/40 hover:text-primary";

function DownloadIcon() {
  return (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M7 10l5 5 5-5M12 15V3" />
    </svg>
  );
}

export default function AdspCramHero() {
  const toast = useToast();
  const { subscription, loading } = useSubscription();
  const [busy, setBusy] = useState(false);

  const loggedIn = isLoggedIn();
  const canDownload = loggedIn && subscription.active;

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
    <div className="not-prose my-6">
      {loading ? (
        <button disabled className={`${OUTLINE_BTN} opacity-60`}>
          <DownloadIcon />
          확인 중...
        </button>
      ) : !loggedIn ? (
        <button
          onClick={() => alert("로그인 후 이용 가능합니다.")}
          className={OUTLINE_BTN}
        >
          <DownloadIcon />
          로그인하고 PDF 받기
        </button>
      ) : !canDownload ? (
        <Link href="/checkout" className={OUTLINE_BTN}>
          <DownloadIcon />
          Thunder 플랜 보고 PDF 받기
        </Link>
      ) : (
        <button onClick={handleDownload} disabled={busy} className={PRIMARY_BTN}>
          <DownloadIcon />
          {busy ? "PDF 준비 중..." : "전체 PDF 다운로드"}
        </button>
      )}
    </div>
  );
}
