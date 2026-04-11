import type { MetadataRoute } from "next";
import {
  getPublicAllQuestionIds,
  getPublicCategoriesByCert,
  getPublicCerts,
} from "@/lib/publicApi";
import { getAllSlugs } from "@/lib/blog";

const SITE_URL = "https://www.sqldpass.com";

/**
 * 동적 사이트맵:
 * - 정적 랜딩 페이지
 * - /learn, /learn/[cert], /learn/[cert]/[category]
 * - /q/[id] (모든 공개 문제)
 *
 * 공개 API 실패 시 최소 정적 페이지만 반환 (try-catch 방어).
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const now = new Date();

  const blogSlugs = getAllSlugs();
  const blogEntries: MetadataRoute.Sitemap = [
    {
      url: `${SITE_URL}/blog`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 0.8,
    },
    ...blogSlugs.map((slug) => ({
      url: `${SITE_URL}/blog/${slug}`,
      lastModified: now,
      changeFrequency: "monthly" as const,
      priority: 0.7,
    })),
  ];

  const staticEntries: MetadataRoute.Sitemap = [
    {
      url: `${SITE_URL}/`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 1.0,
    },
    {
      url: `${SITE_URL}/learn`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 0.9,
    },
    ...blogEntries,
    {
      url: `${SITE_URL}/solve`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 0.8,
    },
    {
      url: `${SITE_URL}/mock-exams`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 0.8,
    },
    {
      url: `${SITE_URL}/about`,
      lastModified: now,
      changeFrequency: "monthly",
      priority: 0.5,
    },
    {
      url: `${SITE_URL}/privacy`,
      lastModified: now,
      changeFrequency: "yearly",
      priority: 0.3,
    },
    {
      url: `${SITE_URL}/terms`,
      lastModified: now,
      changeFrequency: "yearly",
      priority: 0.3,
    },
  ];

  try {
    const certs = await getPublicCerts();
    const certEntries: MetadataRoute.Sitemap = certs.map((c) => ({
      url: `${SITE_URL}/learn/${c.slug}`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 0.8,
    }));

    const categoryEntries: MetadataRoute.Sitemap = [];
    for (const cert of certs) {
      try {
        const categories = await getPublicCategoriesByCert(cert.slug);
        for (const cat of categories) {
          categoryEntries.push({
            url: `${SITE_URL}/learn/${cert.slug}/${cat.slug}`,
            lastModified: now,
            changeFrequency: "weekly",
            priority: 0.7,
          });
        }
      } catch {
        /* 스킵 */
      }
    }

    let questionEntries: MetadataRoute.Sitemap = [];
    try {
      const ids = await getPublicAllQuestionIds();
      questionEntries = ids.map((id) => ({
        url: `${SITE_URL}/q/${id}`,
        lastModified: now,
        changeFrequency: "monthly",
        priority: 0.6,
      }));
    } catch {
      /* 스킵 */
    }

    return [
      ...staticEntries,
      ...certEntries,
      ...categoryEntries,
      ...questionEntries,
    ];
  } catch {
    return staticEntries;
  }
}
