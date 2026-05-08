"use client";

import { useEffect } from "react";

// /sw.js 를 등록하는 클라이언트 사이드 부트스트래퍼.
// 개발 모드(next dev)에서는 캐시가 디버깅을 방해하므로 등록을 건너뛴다.
export default function ServiceWorkerRegistrar() {
  useEffect(() => {
    if (typeof window === "undefined") return;
    if (!("serviceWorker" in navigator)) return;
    if (process.env.NODE_ENV !== "production") return;

    const onLoad = () => {
      navigator.serviceWorker.register("/sw.js").catch((err) => {
        // 등록 실패는 사용자 경험에 치명적이지 않다 — 조용히 흘려보낸다.
        console.warn("[SW] register failed", err);
      });
    };

    if (document.readyState === "complete") {
      onLoad();
    } else {
      window.addEventListener("load", onLoad, { once: true });
      return () => window.removeEventListener("load", onLoad);
    }
  }, []);

  return null;
}
