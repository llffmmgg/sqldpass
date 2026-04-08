"use client";

import { useState } from "react";
import FeedbackModal from "@/components/FeedbackModal";

interface Props {
  questionId: number;
  size?: "sm" | "md";
}

export default function ReportQuestionButton({ questionId, size = "sm" }: Props) {
  const [open, setOpen] = useState(false);
  const cls =
    size === "sm"
      ? "text-[11px] text-muted/70 hover:text-amber-300"
      : "text-xs text-muted hover:text-amber-300";
  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className={`inline-flex items-center gap-1 transition-colors ${cls}`}
        title="이 문제 오류 신고"
      >
        <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M3 21v-4a4 4 0 014-4h14M3 21l4-4M21 3l-4 4M21 3v4a4 4 0 01-4 4H3" />
        </svg>
        🐞 이 문제 오류 신고
      </button>
      <FeedbackModal
        open={open}
        onClose={() => setOpen(false)}
        defaultQuestionId={questionId}
        defaultType="QUESTION_ERROR"
      />
    </>
  );
}
