import type { Metadata } from "next";

import { CERT_TOKENS, certFromExamType, type CertKey } from "@/lib/cert-tokens";
import { getPublicPost, type PublicPostDetail } from "@/lib/publicApi";

const SITE_URL = "https://www.sqldpass.com";

function stripMarkdown(md: string): string {
  return md
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/!\[[^\]]*\]\([^)]*\)/g, " ")
    .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
    .replace(/[#>*_~\-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function buildDescription(post: PublicPostDetail): string {
  const body = stripMarkdown(post.content);
  const cert: CertKey | null = certFromExamType(post.cert);
  const certLabel = cert ? CERT_TOKENS[cert].labelLong : "자격증";
  const preview = body.length > 140 ? body.slice(0, 140) + "..." : body;
  return preview || `${certLabel} 합격 후기 — ${post.title}`;
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const postId = Number(id);

  if (!Number.isFinite(postId)) {
    return { title: "합격 후기 | 문어CBT" };
  }

  try {
    const post = await getPublicPost(postId);
    const cert: CertKey | null = certFromExamType(post.cert);
    const certLabel = cert ? CERT_TOKENS[cert].label : null;
    const titleSuffix = certLabel
      ? ` | ${certLabel} 합격 후기 | 문어CBT`
      : " | 합격 후기 | 문어CBT";
    const title = `${post.title}${titleSuffix}`;
    const description = buildDescription(post);
    const canonical = `${SITE_URL}/board/${postId}`;

    return {
      title,
      description,
      alternates: { canonical },
      openGraph: {
        title,
        description,
        url: canonical,
        type: "article",
        publishedTime: post.createdAt,
        modifiedTime: post.updatedAt,
        authors: [post.authorNickname],
      },
      twitter: {
        card: "summary_large_image",
        title,
        description,
      },
    };
  } catch {
    return {
      title: "합격 후기 | 문어CBT",
      description: "문어CBT 합격 후기 게시판 — SQLD, 정보처리기사, 컴퓨터활용능력 등 자격증 합격자들의 학습 후기와 팁.",
    };
  }
}

function buildJsonLd(post: PublicPostDetail) {
  const cert: CertKey | null = certFromExamType(post.cert);
  const certLabel = cert ? CERT_TOKENS[cert].labelLong : "자격증";
  const canonical = `${SITE_URL}/board/${post.id}`;
  const description = buildDescription(post);

  const article = {
    "@context": "https://schema.org",
    "@type": "Article",
    headline: post.title,
    description,
    datePublished: post.createdAt,
    dateModified: post.updatedAt,
    author: {
      "@type": "Person",
      name: post.authorNickname,
    },
    publisher: {
      "@type": "Organization",
      name: "문어CBT",
      url: SITE_URL,
    },
    mainEntityOfPage: {
      "@type": "WebPage",
      "@id": canonical,
    },
    articleSection: `${certLabel} 합격 후기`,
    inLanguage: "ko",
    interactionStatistic: {
      "@type": "InteractionCounter",
      interactionType: "https://schema.org/ReadAction",
      userInteractionCount: post.viewCount,
    },
  };

  const breadcrumb = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "홈", item: `${SITE_URL}/` },
      {
        "@type": "ListItem",
        position: 2,
        name: "합격 후기",
        item: `${SITE_URL}/board`,
      },
      {
        "@type": "ListItem",
        position: 3,
        name: post.title,
        item: canonical,
      },
    ],
  };

  return [article, breadcrumb];
}

export default async function BoardPostLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const postId = Number(id);

  let ldBlocks: ReturnType<typeof buildJsonLd> | null = null;
  if (Number.isFinite(postId)) {
    try {
      const post = await getPublicPost(postId);
      ldBlocks = buildJsonLd(post);
    } catch {
      ldBlocks = null;
    }
  }

  return (
    <>
      {ldBlocks?.map((ld, index) => (
        <script
          key={index}
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(ld) }}
        />
      ))}
      {children}
    </>
  );
}
