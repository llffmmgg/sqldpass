"use client";

import { useEffect, useState } from "react";
import { getActiveNotice, type ActiveNotice } from "@/lib/noticeApi";
import { Button } from "@/components/ui";

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
        className="flex max-h-[85dvh] w-full max-w-md flex-col rounded-2xl border border-primary/30 bg-surface p-6 text-sm leading-relaxed text-text shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {notice.title && (
          <p className="shrink-0 text-base font-bold text-primary">{notice.title}</p>
        )}

        <div className="mt-3 flex-1 space-y-3 overflow-y-auto whitespace-pre-wrap break-words pr-1 text-text">
          {notice.body}
        </div>

        <div className="mt-6 flex shrink-0 flex-col-reverse gap-2 sm:flex-row">
          <Button variant="secondary" size="md" onClick={dismissForWeek} className="flex-1">
            7일간 보지 않기
          </Button>
          <Button variant="primary" size="md" onClick={closeOnce} className="flex-1">
            확인했어요
          </Button>
        </div>
      </div>
    </div>
  );
}
