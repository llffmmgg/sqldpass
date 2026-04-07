import type { MetadataRoute } from "next";

const SITE_URL = "https://sqldpass.com";

/**
 * Phase 0 정적 사이트맵.
 * Phase 1에서 공개 API 기반 동적 확장 예정 (learn/[cert], learn/[cert]/[category], q/[id]).
 */
export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date();
  return [
    {
      url: `${SITE_URL}/`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 1.0,
    },
  ];
}
