import { getAllPosts } from "@/lib/blog";
import { CERT_TOKENS, certFromExamType, CERT_LIST, slugFromCert } from "@/lib/cert-tokens";
import { getPublicPastExamsByCert } from "@/lib/publicApi";
import {
  pastExamBlogDescription,
  pastExamBlogSlug,
  pastExamBlogTitle,
} from "@/lib/pastExamBlog";

// 매 요청 시 신선하게 — 빌드타임 backend 미연결 시 빈 RSS 캐시되는 문제 방지.
export const dynamic = "force-dynamic";

const SITE_URL = "https://www.sqldpass.com";
const FEED_TITLE = "문어CBT 블로그";
const FEED_DESC =
  "SQLD·정처기·컴활·ADsP 자격증 학습 가이드와 시험 전략. 문어CBT 블로그.";

interface FeedItem {
  url: string;
  title: string;
  description: string;
  date: string;
  category: string;
}

function escapeXml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

function toRfc822(date: string): string {
  const d = new Date(date);
  if (Number.isNaN(d.getTime())) return new Date().toUTCString();
  return d.toUTCString();
}

export async function GET() {
  const mdxItems: FeedItem[] = getAllPosts().map((post) => ({
    url: `${SITE_URL}/blog/${post.slug}`,
    title: post.title,
    description: post.description,
    date: post.date,
    category: post.category,
  }));

  // 기출 복원 SEO 블로그 글도 RSS 에 함께 노출
  const pastExamItems: FeedItem[] = [];
  for (const cert of CERT_LIST) {
    try {
      const list = await getPublicPastExamsByCert(slugFromCert(cert.key));
      for (const exam of list) {
        const certKey = certFromExamType(exam.examType);
        const category = certKey ? CERT_TOKENS[certKey].blogCategory : "일반";
        pastExamItems.push({
          url: `${SITE_URL}/blog/past-exam/${pastExamBlogSlug(exam)}`,
          title: pastExamBlogTitle(exam),
          description: pastExamBlogDescription(exam),
          date: exam.createdAt.slice(0, 10),
          category,
        });
      }
    } catch {
      /* 스킵 */
    }
  }

  const allItems = [...mdxItems, ...pastExamItems].sort((a, b) =>
    a.date > b.date ? -1 : 1,
  );

  const buildDate = new Date().toUTCString();

  const items = allItems
    .map((post) => `    <item>
      <title>${escapeXml(post.title)}</title>
      <link>${post.url}</link>
      <guid isPermaLink="true">${post.url}</guid>
      <pubDate>${toRfc822(post.date)}</pubDate>
      <category>${escapeXml(post.category)}</category>
      <description><![CDATA[${post.description}]]></description>
    </item>`)
    .join("\n");

  const xml = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
    <title>${escapeXml(FEED_TITLE)}</title>
    <link>${SITE_URL}/blog</link>
    <description>${escapeXml(FEED_DESC)}</description>
    <language>ko</language>
    <lastBuildDate>${buildDate}</lastBuildDate>
    <atom:link href="${SITE_URL}/rss.xml" rel="self" type="application/rss+xml" />
${items}
  </channel>
</rss>
`;

  return new Response(xml, {
    headers: {
      "Content-Type": "application/rss+xml; charset=utf-8",
      "Cache-Control": "public, max-age=3600, s-maxage=3600",
    },
  });
}
