"use client";

import { useEffect, useSyncExternalStore } from "react";

import { Button } from "@/components/ui";

const STORAGE_KEY = "all-pass-6month-notice-dismissed-2026-05";

// sessionStorage 자체엔 onChange 이벤트가 없어, dismiss 시 내부 listeners 로 강제 갱신.
const listeners = new Set<() => void>();

function subscribe(cb: () => void) {
  listeners.add(cb);
  return () => {
    listeners.delete(cb);
  };
}

function getSnapshot(): boolean {
  try {
    return sessionStorage.getItem(STORAGE_KEY) === "1";
  } catch {
    return false;
  }
}

// SSR: dismissed=true 로 시작 → 서버 HTML 에는 모달 없음. hydration 후 클라이언트
// 스냅샷이 실제 sessionStorage 값으로 갈아끼워지며 자연스럽게 모달이 나타난다.
function getServerSnapshot(): boolean {
  return true;
}

function dismissNotice() {
  try {
    sessionStorage.setItem(STORAGE_KEY, "1");
  } catch {
    // 차단 환경에선 무시 — 본 세션 동안만 닫힘 유지 (listeners 만 알림)
  }
  listeners.forEach((cb) => cb());
}

/**
 * All Pass 플랜 정책 변경(평생→6개월) 안내 모달.
 * - PG사 정책상 평생 상품 유지 불가 → 6개월(180일) 로 전환.
 * - 기존 평생 구매자는 권리 유지됨을 함께 안내해 불안 해소.
 * - sessionStorage 로 세션당 1회만 노출. STORAGE_KEY 의 날짜 suffix 로 향후 다른 공지와 분리.
 */
export default function AllPassPolicyNotice() {
  const dismissed = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  const open = !dismissed;

  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") dismissNotice();
    }
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[60] bg-black/70"
      onClick={dismissNotice}
      role="dialog"
      aria-modal="true"
      aria-labelledby="all-pass-policy-title"
    >
      <div className="flex min-h-full items-center justify-center px-4 py-6 sm:py-8">
        <div
          className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 shadow-xl sm:p-7"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="mb-4 flex items-center gap-2.5">
            <span className="inline-flex items-center rounded border border-primary/40 bg-primary/10 px-2 py-0.5 text-[10.5px] font-bold tracking-wide text-primary">
              안내
            </span>
            <h2
              id="all-pass-policy-title"
              className="text-[17px] font-bold tracking-tight text-text"
            >
              All Pass 플랜 변경 안내
            </h2>
          </div>

          <div className="space-y-3 text-[13.5px] leading-[1.7] text-text-muted">
            <p>
              결제 대행사(PG) 정책상{" "}
              <strong className="text-text">평생 이용권</strong> 상품을 더 이상 유지할 수 없게
              되었어요. 이에 따라 All Pass 플랜의 이용 기간이{" "}
              <strong className="text-text">6개월(180일)</strong> 로 변경되었습니다.
            </p>
            <p>
              <strong className="text-text">이미 평생 All Pass 를 구매하신 분들은</strong>{" "}
              그대로 평생 이용 권리가 유지되니 안심해주세요. 변경 사항은 신규 결제부터만
              적용됩니다.
            </p>
          </div>

          <div className="mt-6 flex justify-end">
            <Button variant="primary" size="md" onClick={dismissNotice}>
              확인했어요
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
