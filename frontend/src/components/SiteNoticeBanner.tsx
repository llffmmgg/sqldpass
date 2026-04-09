"use client";

import { useEffect, useState } from "react";
import { getActiveNotice, type ActiveNotice } from "@/lib/noticeApi";

const STORAGE_KEY = "site-banner-dismissed-v";

export function SiteNoticeBanner() {
  const [notice, setNotice] = useState<ActiveNotice | null>(null);

  useEffect(() => {
    let cancelled = false;
    getActiveNotice("BANNER").then((n) => {
      if (cancelled || !n) return;
      try {
        if (localStorage.getItem(STORAGE_KEY + n.version) === "1") return;
      } catch {}
      setNotice(n);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!notice) return null;

  function dismiss() {
    try {
      if (notice) localStorage.setItem(STORAGE_KEY + notice.version, "1");
    } catch {}
    setNotice(null);
  }

  return (
    <div className="flex items-center justify-center gap-3 border-b border-amber-400/30 bg-amber-50 px-4 py-2.5 text-center text-sm text-amber-900 dark:bg-amber-500/10 dark:text-amber-200">
      <span className="whitespace-pre-wrap break-words">
        <span className="mr-1">📢</span>
        {notice.body}
      </span>
      <button
        onClick={dismiss}
        aria-label="닫기"
        className="shrink-0 rounded p-1 text-amber-700 hover:bg-amber-100 dark:text-amber-300 dark:hover:bg-amber-500/20"
      >
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  );
}
