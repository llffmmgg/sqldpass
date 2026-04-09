"use client";

import { useEffect, useState } from "react";
import { getActiveNotice, type ActiveNotice } from "@/lib/noticeApi";

const STORAGE_KEY = "site-notice-dismissed-v";
const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000;

export function SiteNoticeModal() {
  const [notice, setNotice] = useState<ActiveNotice | null>(null);

  useEffect(() => {
    let cancelled = false;
    getActiveNotice("MODAL").then((n) => {
      if (cancelled || !n) return;
      try {
        const until = Number(localStorage.getItem(STORAGE_KEY + n.version) ?? 0);
        if (Date.now() < until) return;
      } catch {}
      setNotice(n);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!notice) return null;

  function closeOnce() {
    setNotice(null);
  }
  function dismissForWeek() {
    try {
      if (notice) {
        localStorage.setItem(STORAGE_KEY + notice.version, String(Date.now() + SEVEN_DAYS_MS));
      }
    } catch {}
    setNotice(null);
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
        {notice.title && (
          <p className="text-base font-bold text-amber-300">{notice.title}</p>
        )}

        <div className="mt-3 space-y-3 whitespace-pre-wrap break-words text-foreground/90">
          {notice.body}
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
