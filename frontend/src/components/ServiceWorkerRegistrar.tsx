"use client";

import { useEffect } from "react";
import { isCapacitorApp } from "@/lib/platform";

// /sw.js 는 안드로이드 Capacitor 앱에서만 의미가 있다 (HTML/JS 셸 캐시로 오프라인 풀이를
// 보조). 웹에는 굳이 깔지 않는다 — 이전 빌드에서 한 번이라도 깐 적이 있으면 정리까지 한다.
export default function ServiceWorkerRegistrar() {
  useEffect(() => {
    if (typeof window === "undefined") return;
    if (!("serviceWorker" in navigator)) return;
    if (process.env.NODE_ENV !== "production") return;

    if (isCapacitorApp()) {
      registerForApp();
    } else {
      // 과거 빌드에서 웹에도 SW 가 등록됐을 수 있으니 (NODE_ENV=production 만 보던 시절)
      // 한 번 청소해 준다. 등록 안 돼 있으면 no-op.
      cleanupForWeb();
    }
  }, []);

  return null;
}

function registerForApp() {
  const onLoad = () => {
    navigator.serviceWorker.register("/sw.js").catch((err) => {
      console.warn("[SW] register failed", err);
    });
  };
  if (document.readyState === "complete") {
    onLoad();
  } else {
    window.addEventListener("load", onLoad, { once: true });
  }
}

async function cleanupForWeb() {
  try {
    const registrations = await navigator.serviceWorker.getRegistrations();
    if (registrations.length === 0) return;
    await Promise.all(registrations.map((r) => r.unregister().catch(() => false)));
    // 우리 SW 가 만든 캐시도 같이 비운다 — 다른 SW 가 만든 건 건드리지 않음.
    if (typeof caches !== "undefined") {
      const keys = await caches.keys();
      await Promise.all(
        keys
          .filter((k) => k.startsWith("sqldpass-"))
          .map((k) => caches.delete(k).catch(() => false)),
      );
    }
  } catch {
    // 정리 실패는 사용자 경험에 영향 없음 — 다음 방문 때 다시 시도된다.
  }
}
