"use client";

import { useCallback, useEffect } from "react";
import MascotImage from "@/components/mascot/MascotImage";
import { Button } from "@/components/ui";
import { markSolveTutorialSeen } from "@/lib/tutorialStorage";

interface Props {
  open: boolean;
  onClose: () => void;
}

export default function SolveTutorialModal({ open, onClose }: Props) {
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
              pose="guide"
              size={112}
              className="animate-mascot-in animate-mascot-bounce"
              priority
            />
            <h2
              id="solve-tutorial-title"
              className="mt-4 text-lg font-bold tracking-tight text-text"
            >
              안녕! 빠르게 푸는 팁 하나 알려줄게.
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-text-muted">
              보기를 <span className="font-semibold text-text">더블클릭</span>하면
              한 번에 진행할 수 있어.
            </p>
            <p className="mt-1.5 text-sm leading-relaxed text-text-muted">
              키보드 <span className="font-semibold text-text">1~4</span>번,
              <span className="font-semibold text-text"> Enter</span>로도 빨라져요.
            </p>
            <Button
              variant="primary"
              size="md"
              onClick={handleClose}
              autoFocus
              className="mt-6 w-full"
            >
              알겠어요
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
