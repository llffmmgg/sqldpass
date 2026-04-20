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
      cache: "no-store",
    });
    if (res.status === 204) return null;
    if (!res.ok) return null;
    return (await res.json()) as ActiveNotice;
  } catch {
    return null;
  }
}
