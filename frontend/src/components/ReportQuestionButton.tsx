"use client";

import { useState } from "react";
import FeedbackModal from "@/components/FeedbackModal";

interface Props {
  questionId: number;
  size?: "sm" | "md";
}

export default function ReportQuestionButton({ questionId, size = "sm" }: Props) {
  const [open, setOpen] = useState(false);
  const iconSize = size === "sm" ? "h-4 w-4" : "h-4 w-4";
  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="이 문제 오류 신고"
        title="이 문제 오류 신고"
        className={`inline-flex h-7 w-7 items-center justify-center rounded-md text-text-subtle transition-colors hover:bg-surface-hover hover:text-primary`}
      >
        {/* 깃발(report) 아이콘 — 컴팩트 + 테마 톤 */}
        <svg
          className={iconSize}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth={1.8}
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden
        >
          <path d="M4 21V4h11l1 2h5v9h-7l-1-2H6v8" />
        </svg>
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
