"use client";

import { useEffect, useState } from "react";
import { getActiveNotice, type ActiveNotice } from "@/lib/noticeApi";

const STORAGE_KEY = "site-banner-dismissed-v";

/**
 * 하드코딩 폴백 — API 공지가 없을 때 표시.
 * `expiresAt` 이후에는 자동 비활성 (시험일 다음 날 00:00 권장).
 */
const FALLBACK_NOTICE: {
  body: string;
  version: number;
  expiresAt?: string; // ISO date — 이 시각 이후 배너 숨김
} | null = {
  body: "내일은 정보처리기사 실기 시험일! 마지막까지 화이팅입니다 🙌",
  version: 101,
  expiresAt: "2026-04-19T00:00:00+09:00",
};

export function SiteNoticeBanner() {
  const [notice, setNotice] = useState<ActiveNotice | null>(null);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getActiveNotice("BANNER").then((n) => {
      if (cancelled) return;
      let target: ActiveNotice | null = n;
      if (!target && FALLBACK_NOTICE) {
        if (FALLBACK_NOTICE.expiresAt && Date.now() >= new Date(FALLBACK_NOTICE.expiresAt).getTime()) {
          return;
        }
        target = {
          body: FALLBACK_NOTICE.body,
          version: FALLBACK_NOTICE.version,
        } as ActiveNotice;
      }
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
    <div className="flex items-center justify-center gap-3 border-b border-primary/30 bg-primary/10 px-4 py-2.5 text-center text-sm font-medium text-primary">
      <span className="whitespace-pre-wrap break-words">
        <span className="mr-1">📢</span>
        {notice.body}
      </span>
      <button
        type="button"
        onClick={dismiss}
        aria-label="닫기"
        className="shrink-0 rounded p-1 text-primary hover:bg-primary/15"
      >
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  );
}
