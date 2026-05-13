"use client";

import { useSyncExternalStore, type ReactNode } from "react";
import { isLoggedIn } from "@/lib/auth";
import LoginRequired from "@/components/LoginRequired";

function subscribeAuth(callback: () => void) {
  if (typeof window === "undefined") return () => {};
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}

export default function AuthGuard({ children }: { children: ReactNode }) {
  // localStorage(외부 store) 구독 — SSR 단계에선 null로 두어 hydration mismatch 회피.
  const loggedIn = useSyncExternalStore<boolean | null>(
    subscribeAuth,
    () => isLoggedIn(),
    () => null,
  );

  if (loggedIn === null) return null;
  // dev UI preview — 백엔드 없이 화면만 확인할 때 NEXT_PUBLIC_DEV_UI_PREVIEW=true 면 가드 우회.
  // 운영 빌드(NODE_ENV=production) 에서는 환경변수 무시. 절대 활성 안 됨.
  if (!loggedIn) {
    if (
      process.env.NODE_ENV !== "production" &&
      process.env.NEXT_PUBLIC_DEV_UI_PREVIEW === "true"
    ) {
      return <>{children}</>;
    }
    return <LoginRequired />;
  }
  return <>{children}</>;
}
