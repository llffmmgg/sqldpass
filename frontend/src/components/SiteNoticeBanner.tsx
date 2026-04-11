"use client";

import { useEffect, useState } from "react";
import { getActiveNotice, type ActiveNotice } from "@/lib/noticeApi";

const STORAGE_KEY = "site-banner-dismissed-v";

/** 하드코딩 폴백 — API 공지가 없을 때 표시. 불필요해지면 null로 변경. */
const FALLBACK_NOTICE = {
  body: "현재 전체 문항을 전문가가 꼼꼼히 검토하고 있어요! 모의고사는 '전문가 검수' 표시된 항목부터 풀어보시는 걸 추천드려요 🙌",
  version: 100,
};

export function SiteNoticeBanner() {
  const [notice, setNotice] = useState<ActiveNotice | null>(null);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getActiveNotice("BANNER").then((n) => {
      if (cancelled) return;
      const target = n ?? (FALLBACK_NOTICE as ActiveNotice | null);
      if (!target) return;
      try {
        if (localStorage.getItem(STORAGE_KEY + target.version) === "1") {
          setDismissed(true);
          return;
        }
      } catch {}
      setNotice(target);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!notice || dismissed) return null;

  function dismiss() {
    try {
      if (notice) localStorage.setItem(STORAGE_KEY + notice.version, "1");
    } catch {}
    setDismissed(true);
  }

  return (
    <div className="flex items-center justify-center gap-3 border-b border-amber-300 bg-amber-100 px-4 py-2.5 text-center text-sm font-medium text-amber-950 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200">
      <span className="whitespace-pre-wrap break-words">
        <span className="mr-1">📢</span>
        {notice.body}
      </span>
      <button
        onClick={dismiss}
        aria-label="닫기"
        className="shrink-0 rounded p-1 text-amber-800 hover:bg-amber-200 dark:text-amber-300 dark:hover:bg-amber-500/20"
      >
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  );
}
