/**
 * 공지사항 공개 조회 — 클라이언트 컴포넌트(SiteNoticeBanner / SiteNoticeModal)에서 사용.
 */

const BASE = "";

export type NoticeDisplayType = "BANNER" | "MODAL";

export interface ActiveNotice {
  id: number;
  displayType: NoticeDisplayType;
  title: string | null;
  body: string;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** 활성 공지가 없으면 null. */
export async function getActiveNotice(type: NoticeDisplayType): Promise<ActiveNotice | null> {
  try {
    const res = await fetch(`${BASE}/api/notices/active?type=${type}`, {
      // 브라우저/CDN HTTP 캐시는 백엔드의 Cache-Control 헤더를 따른다 (PublicCacheControlInterceptor).
      // SSR/RSC 경로에서 호출될 때를 위한 Next.js 캐시 힌트는 30분 ISR.
      next: { revalidate: 1800 },
    });
    if (res.status === 204) return null;
    if (!res.ok) return null;
    return (await res.json()) as ActiveNotice;
  } catch {
    return null;
  }
}
