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
  if (!loggedIn) return <LoginRequired />;
  return <>{children}</>;
}
