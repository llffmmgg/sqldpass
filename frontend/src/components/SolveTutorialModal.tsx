"use client";

import { useCallback, useEffect } from "react";
import MascotImage from "@/components/mascot/MascotImage";
import { Button } from "@/components/ui";
import { markSolveTutorialSeen } from "@/lib/tutorialStorage";

export type SolveTutorialKind = "graded" | "batch";

interface Props {
  open: boolean;
  onClose: () => void;
  // graded: 한 문제씩 풀고 즉시 정/오답 확인 (/solve, /solve/bookmarks)
  // batch: 시험처럼 한 번에 풀고 끝에 채점 (/mock-exams, /past-exams 기출)
  kind: SolveTutorialKind;
}

const CONTENT: Record<
  SolveTutorialKind,
  { title: string; subtitle: string; tips: { label: string; desc: string }[]; cta: string }
> = {
  graded: {
    title: "문제 풀이는 이렇게!",
    subtitle: "한 문제 풀고 바로 정답을 확인할 수 있어요.",
    tips: [
      { label: "보기 한 번 탭", desc: "답이 선택돼요." },
      { label: "같은 보기 빠르게 두 번 탭", desc: "정답을 바로 확인해요." },
      { label: "키보드 1~4번", desc: "보기 선택, Enter로 제출·다음." },
      { label: "정답을 본 뒤", desc: "다음 버튼으로 진행해요." },
    ],
    cta: "풀어보러 가기",
  },
  batch: {
    title: "모의고사는 이렇게!",
    subtitle: "실제 시험처럼 한 번에 풀고 마지막에 채점해요.",
    tips: [
      { label: "보기 한 번 탭", desc: "답이 저장돼요. 언제든 바꿀 수 있어요." },
      { label: "같은 보기 빠르게 두 번 탭", desc: "다음 문제로 바로 넘어가요." },
      { label: "이전·다음 버튼", desc: "원하는 문제로 이동할 수 있어요." },
      { label: "다 풀었다면", desc: "맨 아래 제출 버튼으로 채점하세요." },
    ],
    cta: "시작하기",
  },
};

export default function SolveTutorialModal({ open, onClose, kind }: Props) {
  const handleClose = useCallback(() => {
    markSolveTutorialSeen();
    onClose();
  }, [onClose]);

  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") handleClose();
    }
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open, handleClose]);

  if (!open) return null;

  const c = CONTENT[kind];

  return (
    <div
      className="fixed inset-0 z-[60] bg-black/60"
      onClick={handleClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="solve-tutorial-title"
    >
      <div className="flex min-h-full items-center justify-center px-4 py-6">
        <div
          className="w-full max-w-sm rounded-2xl border border-border bg-surface p-6 shadow-xl sm:max-w-md sm:p-7"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex flex-col items-center text-center">
            <MascotImage
              pose="tutorial"
              size={132}
              className="animate-mascot-in animate-mascot-bounce"
              priority
            />
            <h2
              id="solve-tutorial-title"
              className="mt-4 text-lg font-bold tracking-tight text-text sm:text-xl"
            >
              {c.title}
            </h2>
            <p className="mt-1.5 text-sm leading-relaxed text-text-muted">
              {c.subtitle}
            </p>

            <ul className="mt-5 w-full space-y-2.5 text-left">
              {c.tips.map((tip, i) => (
                <li
                  key={tip.label}
                  className="flex items-start gap-3 rounded-lg border border-border bg-bg-elevated px-3.5 py-2.5"
                >
                  <span className="mt-0.5 inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary/15 text-[11px] font-bold text-primary">
                    {i + 1}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-semibold text-text">
                      {tip.label}
                    </span>
                    <span className="block text-xs leading-relaxed text-text-muted">
                      {tip.desc}
                    </span>
                  </span>
                </li>
              ))}
            </ul>

            <Button
              variant="primary"
              size="md"
              onClick={handleClose}
              autoFocus
              className="mt-6 w-full"
            >
              {c.cta}
            </Button>
            <button
              type="button"
              onClick={handleClose}
              className="mt-2 text-xs text-text-muted underline-offset-2 hover:text-text hover:underline"
            >
              다시 보지 않기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
