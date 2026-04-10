"use client";

import { useEffect } from "react";
import { usePathname, useSearchParams } from "next/navigation";
import { trackPageview } from "@/lib/gtag";

/**
 * App Router SPA 라우트 변경 시 page_view 이벤트 수동 발송.
 * 초기 페이지 로드는 gtag('config') 가 자동으로 잡으므로 첫 마운트도 safe (중복은 GA 측에서 동일 timestamp로 1건 처리).
 */
export default function GAPageview() {
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    if (!pathname) return;
    if (pathname.startsWith("/admin")) return;
    const query = searchParams?.toString();
    const path = query ? `${pathname}?${query}` : pathname;
    trackPageview(path);
  }, [pathname, searchParams]);

  return null;
}
