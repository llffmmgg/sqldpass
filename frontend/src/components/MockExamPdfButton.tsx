"use client";

import { useState } from "react";

import { useToast } from "@/components/Toast";
import { isLoggedIn } from "@/lib/auth";
import { downloadMockExamPdfAsUser, PdfDownloadError } from "@/lib/payment";

export default function MockExamPdfButton({
  examId,
  className = "",
}: {
  examId: number;
  className?: string;
}) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);

  return (
    <button
      onClick={async () => {
        if (!isLoggedIn()) {
          alert("로그인 후 이용 가능합니다.");
          return;
        }
        if (busy) return;
        setBusy(true);
        try {
          await downloadMockExamPdfAsUser(examId);
        } catch (e) {
          if (e instanceof PdfDownloadError && e.code === "PDF_REQUIRES_SUBSCRIPTION") {
            toast.show(
              "PDF 다운로드는 All Pass 플랜 전용입니다. 우측 상단 프로필에서 업그레이드할 수 있어요.",
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
      }}
      disabled={busy}
      className={
        className ||
        "inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-text-muted transition-colors hover:border-primary/40 hover:text-text disabled:opacity-60"
      }
      title="시험지 PDF 다운로드"
    >
      <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M7 10l5 5 5-5M12 15V3" />
      </svg>
      {busy ? "PDF 준비 중..." : "PDF 다운로드"}
    </button>
  );
}
