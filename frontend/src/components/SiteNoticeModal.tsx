"use client";

import { useEffect, useState } from "react";

const STORAGE_KEY = "site-notice-dismissed-until-v1";
const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000;

export function SiteNoticeModal() {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    try {
      const until = Number(localStorage.getItem(STORAGE_KEY) ?? 0);
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (Date.now() >= until) setOpen(true);
    } catch {
      setOpen(true);
    }
  }, []);

  if (!open) return null;

  function closeOnce() {
    setOpen(false);
  }
  function dismissForWeek() {
    try {
      localStorage.setItem(STORAGE_KEY, String(Date.now() + SEVEN_DAYS_MS));
    } catch {}
    setOpen(false);
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={closeOnce}
    >
      <div
        className="w-full max-w-md rounded-2xl border border-amber-500/30 bg-surface p-6 text-sm leading-relaxed text-foreground shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="text-base font-bold text-amber-300">📢 공지드려요!</p>

        <div className="mt-3 space-y-3 text-foreground/90">
          <p>안녕하세요, SQLD Pass에 와주셔서 정말 감사해요 🙇‍♀️</p>
          <p>
            그동안 여러분이 보내주신 <b>피드백 문제들을 모두 업데이트</b>했고,
            지금은 <b>전체 문제를 하나하나 다시 검토</b>하고 있어요.
          </p>
          <p>
            조금 더 정확하고 좋은 퀄리티의 문제로 보답드릴 수 있도록 열심히 다듬는 중이니,
            혹시 풀다가 어색한 부분이 보이면 너그럽게 봐주시면 감사하겠습니다 💛
          </p>
          <p className="text-muted">언제나 더 좋은 학습 경험을 위해 노력할게요. 화이팅이에요! 🌱</p>
        </div>

        <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row">
          <button
            onClick={dismissForWeek}
            className="flex-1 rounded-lg border border-border bg-surface py-2.5 text-xs font-medium text-muted hover:text-foreground"
          >
            7일간 보지 않기
          </button>
          <button
            onClick={closeOnce}
            className="flex-1 rounded-lg bg-primary py-2.5 text-sm font-semibold text-zinc-900 hover:bg-primary-hover"
          >
            확인했어요
          </button>
        </div>
      </div>
    </div>
  );
}
