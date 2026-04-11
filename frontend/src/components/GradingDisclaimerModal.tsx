"use client";

import { useEffect, useState } from "react";

const STORAGE_KEY = "grading-disclaimer-v2";
const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000;

export function GradingDisclaimerModal() {
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

  function dismissForever() {
    try {
      localStorage.setItem(STORAGE_KEY, String(Number.MAX_SAFE_INTEGER));
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
        <p className="text-base font-bold text-amber-300">💡 시작 전에 잠깐만요!</p>

        <div className="mt-3 space-y-3 text-foreground/90">
          <p>안녕하세요 :) 풀이에 들어가기 전에 한 가지만 말씀드릴게요.</p>
          <p>
            저희 문제는 <b>최대한 보수적으로</b> 채점하고 있어요. 특히 서술형 문항은 원래 사람이
            직접 채점하지만, 저희는 키워드 기반으로 컴퓨터가 채점하고 있어서 정답과 비슷하게
            작성하셔도 표현이 조금만 달라도 오답으로 처리될 수 있어요.
            이 점 조금만 감안해 주시면 감사하겠습니다 🙏
          </p>
          <p>
            &quot;이건 맞은 것 같은데?&quot; 싶은 문항이 있다면 해설을 꼭 확인해 주세요.
          </p>
          <p className="text-muted">좋은 풀이 시간 되세요!</p>
        </div>

        <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row">
          <button
            onClick={dismissForever}
            className="flex-1 rounded-lg border border-border bg-surface py-2.5 text-xs font-medium text-muted hover:text-foreground"
          >
            앞으로 보지 않기
          </button>
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
