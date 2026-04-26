import type { MetadataRoute } from "next";
import fs from "fs";
import path from "path";
import matter from "gray-matter";
import {
  getPublicAllQuestionIds,
  getPublicCategoriesByCert,
  getPublicCerts,
  getPublicPastExamsByCert,
} from "@/lib/publicApi";
import { getAllSlugs } from "@/lib/blog";

const SITE_URL = "https://www.sqldpass.com";
const BLOG_DIR = path.join(process.cwd(), "content", "blog");

// 정적 페이지 마지막 수정일 (내용 변경 시 수동 업데이트)
const STATIC_LAST_MOD: Record<string, string> = {
  "/": "2026-04-23",
  "/learn": "2026-04-22",
  "/blog": "2026-04-25",
  "/solve": "2026-04-16",
  "/mock-exams": "2026-04-16",
  "/past-exams": "2026-04-23",
  "/cbt-mock-exam": "2026-04-22",
  "/about": "2026-04-22",
  "/changelog": "2026-04-22",
  "/privacy": "2026-04-09",
  "/terms": "2026-04-09",
};

// 동적 DB 페이지는 배포일 공통값
const DYNAMIC_LAST_MOD = "2026-04-22";

function blogPostDate(slug: string): string {
  try {
    const filePath = path.join(BLOG_DIR, `${slug}.mdx`);
    const raw = fs.readFileSync(filePath, "utf-8");
    const { data } = matter(raw);
    return typeof data.date === "string" ? data.date : DYNAMIC_LAST_MOD;
  } catch {
    return DYNAMIC_LAST_MOD;
  }
}

/**
 * 동적 사이트맵:
 * - 정적 랜딩 페이지 (날짜 고정)
 * - 블로그 글 (frontmatter date)
 * - /learn, /learn/[cert], /learn/[cert]/[category] (동적 날짜)
 * - /q/[id] (모든 공개 문제, 동적 날짜)
 *
 * 공개 API 실패 시 최소 정적 페이지만 반환 (try-catch 방어).
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const blogSlugs = getAllSlugs();
  const blogEntries: MetadataRoute.Sitemap = [
    {
      url: `${SITE_URL}/blog`,
      lastModified: STATIC_LAST_MOD["/blog"],
      changeFrequency: "weekly",
      priority: 0.8,
    },
    ...blogSlugs.map((slug) => ({
      url: `${SITE_URL}/blog/${slug}`,
      lastModified: blogPostDate(slug),
      changeFrequency: "monthly" as const,
      priority: 0.7,
    })),
  ];

  const staticEntries: MetadataRoute.Sitemap = [
    {
      url: `${SITE_URL}/`,
      lastModified: STATIC_LAST_MOD["/"],
      changeFrequency: "weekly",
      priority: 1.0,
    },
    {
      url: `${SITE_URL}/learn`,
      lastModified: STATIC_LAST_MOD["/learn"],
      changeFrequency: "weekly",
      priority: 0.9,
    },
    ...blogEntries,
    {
      url: `${SITE_URL}/solve`,
      lastModified: STATIC_LAST_MOD["/solve"],
      changeFrequency: "weekly",
      priority: 0.8,
    },
    {
      url: `${SITE_URL}/mock-exams`,
      lastModified: STATIC_LAST_MOD["/mock-exams"],
      changeFrequency: "weekly",
      priority: 0.8,
    },
    {
      url: `${SITE_URL}/past-exams`,
      lastModified: STATIC_LAST_MOD["/past-exams"],
      changeFrequency: "weekly",
      priority: 0.8,
    },
    {
      url: `${SITE_URL}/cbt-mock-exam`,
      lastModified: STATIC_LAST_MOD["/cbt-mock-exam"],
      changeFrequency: "monthly",
      priority: 0.8,
    },
    {
      url: `${SITE_URL}/about`,
      lastModified: STATIC_LAST_MOD["/about"],
      changeFrequency: "monthly",
      priority: 0.5,
    },
    {
      url: `${SITE_URL}/changelog`,
      lastModified: STATIC_LAST_MOD["/changelog"],
      changeFrequency: "weekly",
      priority: 0.4,
    },
    {
      url: `${SITE_URL}/privacy`,
      lastModified: STATIC_LAST_MOD["/privacy"],
      changeFrequency: "yearly",
      priority: 0.3,
    },
    {
      url: `${SITE_URL}/terms`,
      lastModified: STATIC_LAST_MOD["/terms"],
      changeFrequency: "yearly",
      priority: 0.3,
    },
  ];

  try {
    const certs = await getPublicCerts();
    const certEntries: MetadataRoute.Sitemap = certs.map((c) => ({
      url: `${SITE_URL}/learn/${c.slug}`,
      lastModified: DYNAMIC_LAST_MOD,
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
            lastModified: DYNAMIC_LAST_MOD,
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
        lastModified: DYNAMIC_LAST_MOD,
        changeFrequency: "monthly",
        priority: 0.6,
      }));
    } catch {
      /* 스킵 */
    }

    // 기출 복원 블로그 글 (SEO용 자동 생성)
    const pastExamBlogEntries: MetadataRoute.Sitemap = [];
    try {
      const { pastExamBlogSlug } = await import("@/lib/pastExamBlog");
      for (const cert of certs) {
        try {
          const list = await getPublicPastExamsByCert(cert.slug);
          for (const exam of list) {
            const slug = pastExamBlogSlug(exam);
            pastExamBlogEntries.push({
              url: `${SITE_URL}/blog/past-exam/${slug}`,
              lastModified: (exam.createdAt ?? "").slice(0, 10) || DYNAMIC_LAST_MOD,
              changeFrequency: "monthly",
              priority: 0.7,
            });
          }
        } catch {
          /* 스킵 */
        }
      }
    } catch {
      /* 스킵 */
    }

    const pastExamCertEntries: MetadataRoute.Sitemap = certs.map((cert) => ({
      url: `${SITE_URL}/past-exams/${cert.slug}`,
      lastModified: DYNAMIC_LAST_MOD,
      changeFrequency: "weekly",
      priority: 0.8,
    }));

    const pastExamEntries: MetadataRoute.Sitemap = [];
    for (const cert of certs) {
      try {
        const pastExams = await getPublicPastExamsByCert(cert.slug);
        for (const pe of pastExams) {
          pastExamEntries.push({
            url: `${SITE_URL}/past-exams/${pe.id}`,
            lastModified: (pe.createdAt ?? "").slice(0, 10) || DYNAMIC_LAST_MOD,
            changeFrequency: "monthly",
            priority: 0.75,
          });
        }
      } catch {
        /* 스킵 */
      }
    }

    return [
      ...staticEntries,
      ...certEntries,
      ...categoryEntries,
      ...questionEntries,
      ...pastExamCertEntries,
      ...pastExamEntries,
      ...pastExamBlogEntries,
    ];
  } catch {
    return staticEntries;
  }
}
