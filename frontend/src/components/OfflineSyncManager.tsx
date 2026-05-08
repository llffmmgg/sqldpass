"use client";

import { useEffect } from "react";
import { isCapacitorApp } from "@/lib/platform";
import { drainPendingSolves } from "@/lib/solveOffline";
import { useToast } from "@/components/Toast";

// 안드로이드 앱에서 IndexedDB 큐에 쌓인 오프라인 회차 제출을 서버로 흘려보낸다.
// - 앱 부팅 후 1.5초 뒤 한 번 시도 (이미 온라인일 때 자연스럽게 sync)
// - `online` 이벤트가 발생할 때마다 한 번 더 시도
// - 둘 다 drainInFlight 락으로 직렬화 (중복 POST 방지)
export default function OfflineSyncManager() {
  const toast = useToast();

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (!isCapacitorApp()) return;

    let cancelled = false;

    const attempt = async () => {
      try {
        const result = await drainPendingSolves();
        if (cancelled) return;
        if (result.succeeded > 0) {
          toast.show(
            `오프라인 결과 ${result.succeeded}건이 동기화됐어요`,
            "success",
          );
        }
      } catch {
        // 조용히 실패 — 다음 online 이벤트에서 재시도된다.
      }
    };

    const initialTimer = window.setTimeout(() => {
      if (navigator.onLine) attempt();
    }, 1500);

    const onOnline = () => attempt();
    window.addEventListener("online", onOnline);

    return () => {
      cancelled = true;
      window.clearTimeout(initialTimer);
      window.removeEventListener("online", onOnline);
    };
  }, [toast]);

  return null;
}
