"use client";

import { useEffect, useState } from "react";

import { isLoggedIn } from "@/lib/auth";
import { getActiveSubscription, type ActiveSubscription } from "@/lib/payment";

const INACTIVE: ActiveSubscription = {
  active: false,
  plan: null,
  expiresAt: null,
  removesAds: false,
  allowsPdf: false,
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
 * 회원의 활성 구독 정보를 가져온다.
 * 비로그인이면 INACTIVE 즉시 반환. 네트워크 실패 시에도 INACTIVE.
 *
 * 광고 컴포넌트 / PDF 다운로드 버튼 등에서 가시성 분기에 사용.
 */
export function useSubscription(): { subscription: ActiveSubscription; loading: boolean } {
  const [subscription, setSubscription] = useState<ActiveSubscription>(INACTIVE);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isLoggedIn()) {
      setSubscription(INACTIVE);
      setLoading(false);
      return;
    }
    let cancelled = false;
    fetchSubscriptionCached()
      .then((s) => {
        if (!cancelled) setSubscription(s);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return { subscription, loading };
}
