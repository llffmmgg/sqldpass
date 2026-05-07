import type { MetadataRoute } from "next";
import fs from "fs";
import path from "path";
import matter from "gray-matter";
import {
  getPublicAllQuestionIds,
  getPublicCategoriesByCert,
  getPublicCerts,
  getPublicPastExamsByCert,
  getPublicPostSeoList,
} from "@/lib/publicApi";
import { getAllSlugs } from "@/lib/blog";
import { CERT_LIST, slugFromCert } from "@/lib/cert-tokens";
import { pastExamBlogSlug } from "@/lib/pastExamBlog";

// 빌드타임 생성 시 백엔드가 안 닿아 빈 sitemap 이 캐시되는 문제 방지.
// 매 요청 시 신선하게 생성한다.
export const dynamic = "force-dynamic";

const SITE_URL = "https://www.sqldpass.com";
const BLOG_DIR = path.join(process.cwd(), "content", "blog");

// force-dynamic 이라 매 요청마다 sitemap 이 재생성되므로,
// 콘텐츠가 자주 갱신되는 페이지는 today 로 두어 구글에 신선도 신호를 정확히 전달한다.
// 정책·변경이력처럼 거의 정지된 문서만 고정 날짜로 보수적으로 유지.
const TODAY = new Date().toISOString().slice(0, 10);

const STATIC_LAST_MOD: Record<string, string> = {
  "/": TODAY,
  "/learn": TODAY,
  "/blog": TODAY,
  "/solve": TODAY,
  "/mock-exams": TODAY,
  "/past-exams": TODAY,
  "/cbt-mock-exam": TODAY,
  "/board": TODAY,
  "/about": TODAY,
  "/changelog": TODAY,
  "/privacy": "2026-04-09",
  "/terms": "2026-04-09",
};

// 동적 DB 페이지의 폴백. DB createdAt 이 없을 때만 사용된다.
const DYNAMIC_LAST_MOD = TODAY;

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
      url: `${SITE_URL}/board`,
      lastModified: STATIC_LAST_MOD["/board"],
      changeFrequency: "daily",
      priority: 0.7,
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

  // cert 목록은 우선 백엔드에서 받아오고 실패 시 하드코딩 fallback.
  // 둘 다 같은 6개 cert 를 반환하므로 결과가 빈 sitemap 이 되는 일이 없다.
  let certs: { slug: string }[];
  try {
    certs = await getPublicCerts();
  } catch {
    certs = CERT_LIST.map((c) => ({ slug: slugFromCert(c.key) }));
  }

  const certEntries: MetadataRoute.Sitemap = certs.map((c) => ({
    url: `${SITE_URL}/learn/${c.slug}`,
    lastModified: DYNAMIC_LAST_MOD,
    changeFrequency: "weekly",
    priority: 0.8,
  }));

  // 자격증별 CBT 랜딩 페이지 (/cbt-mock-exam/[cert])
  const cbtLandingEntries: MetadataRoute.Sitemap = certs.map((c) => ({
    url: `${SITE_URL}/cbt-mock-exam/${c.slug}`,
    lastModified: DYNAMIC_LAST_MOD,
    changeFrequency: "weekly",
    priority: 0.85,
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

  const pastExamCertEntries: MetadataRoute.Sitemap = certs.map((cert) => ({
    url: `${SITE_URL}/past-exams/${cert.slug}`,
    lastModified: DYNAMIC_LAST_MOD,
    changeFrequency: "weekly",
    priority: 0.8,
  }));

  // 기출 복원 회차별 풀이 페이지 + SEO 블로그 글 (한 번의 API 호출로 둘 다 만든다)
  const pastExamEntries: MetadataRoute.Sitemap = [];
  const pastExamBlogEntries: MetadataRoute.Sitemap = [];
  for (const cert of certs) {
    try {
      const pastExams = await getPublicPastExamsByCert(cert.slug);
      for (const pe of pastExams) {
        const lastMod = (pe.createdAt ?? "").slice(0, 10) || DYNAMIC_LAST_MOD;
        pastExamEntries.push({
          url: `${SITE_URL}/past-exams/${pe.id}`,
          lastModified: lastMod,
          changeFrequency: "monthly",
          priority: 0.75,
        });
        pastExamBlogEntries.push({
          url: `${SITE_URL}/blog/past-exam/${pastExamBlogSlug(pe)}`,
          lastModified: lastMod,
          changeFrequency: "monthly",
          priority: 0.7,
        });
      }
    } catch {
      /* 스킵 */
    }
  }

  let postEntries: MetadataRoute.Sitemap = [];
  try {
    const posts = await getPublicPostSeoList();
    postEntries = posts.map((p) => ({
      url: `${SITE_URL}/board/${p.id}`,
      lastModified: (p.updatedAt ?? "").slice(0, 10) || DYNAMIC_LAST_MOD,
      changeFrequency: "weekly" as const,
      priority: 0.65,
    }));
  } catch {
    /* 스킵 */
  }

  return [
    ...staticEntries,
    ...certEntries,
    ...cbtLandingEntries,
    ...categoryEntries,
    ...questionEntries,
    ...pastExamCertEntries,
    ...pastExamEntries,
    ...pastExamBlogEntries,
    ...postEntries,
  ];
}
