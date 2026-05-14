"use client";

import { useEffect, useState, useSyncExternalStore } from "react";

import { isLoggedIn } from "@/lib/auth";
import { getActiveSubscription, type ActiveSubscription } from "@/lib/payment";

const INACTIVE: ActiveSubscription = {
  active: false,
  plan: null,
  expiresAt: null,
  removesAds: false,
  allowsPdf: false,
  hasLibraryAccess: false,
  allowsPremium: false,
};

// 페이지에 광고가 여러 개 마운트되어도 API 한 번만 호출하도록 모듈 캐시.
// 페이지 이동 / 새로고침 시 재호출.
let cachedPromise: Promise<ActiveSubscription> | null = null;

function fetchSubscriptionCached(): Promise<ActiveSubscription> {
  if (cachedPromise) return cachedPromise;
  cachedPromise = getActiveSubscription().catch(() => INACTIVE);
  return cachedPromise;
}

/**
 * 결제 성공 / 로그인 / 로그아웃 등 구독 상태가 변할 수 있는 지점에서 호출.
 * 다음 useSubscription 마운트 시 백엔드에서 새 데이터를 다시 받는다.
 */
export function invalidateSubscriptionCache(): void {
  cachedPromise = null;
}

function subscribeAuth(callback: () => void) {
  if (typeof window === "undefined") return () => {};
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}

/**
 * 회원의 활성 구독 정보를 가져온다.
 * 비로그인이면 INACTIVE 즉시 반환. 네트워크 실패 시에도 INACTIVE.
 *
 * 광고 컴포넌트 / PDF 다운로드 버튼 등에서 가시성 분기에 사용.
 */
export function useSubscription(): { subscription: ActiveSubscription; loading: boolean } {
  // 로그인 상태(localStorage 토큰)를 외부 store로 구독 — 비로그인 분기를 render 단계에서 derive하여
  // effect 안 sync setState(set-state-in-effect) 회피. SSR 단계에선 null.
  const loggedIn = useSyncExternalStore<boolean | null>(
    subscribeAuth,
    () => isLoggedIn(),
    () => null,
  );
  const [data, setData] = useState<ActiveSubscription | null>(null);

  useEffect(() => {
    if (loggedIn !== true) return;
    let cancelled = false;
    fetchSubscriptionCached().then((s) => {
      if (!cancelled) setData(s);
    });
    return () => {
      cancelled = true;
    };
  }, [loggedIn]);

  if (loggedIn === false) {
    return { subscription: INACTIVE, loading: false };
  }
  // SSR(null) 또는 로그인됐지만 fetch 미완료 → loading=true.
  return { subscription: data ?? INACTIVE, loading: data === null };
}
