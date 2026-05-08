"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { isCapacitorApp } from "@/lib/platform";

// Routes that exist for SEO / 웹 전용 사용자 (게시판, 블로그, 자격증 랜딩).
// 안드로이드 앱 모드에서 진입하면 모의고사 허브로 강제 리다이렉트한다.
const BLOCKED_PREFIXES = ["/board", "/blog", "/learn"];
const APP_HOME = "/mock-exams";

function isBlocked(pathname: string | null): boolean {
  if (!pathname) return false;
  if (pathname === "/") return true;
  return BLOCKED_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

export default function AppRouteGuard() {
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!isCapacitorApp()) return;
    if (isBlocked(pathname)) {
      router.replace(APP_HOME);
    }
  }, [pathname, router]);

  return null;
}
