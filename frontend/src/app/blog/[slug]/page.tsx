import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { MDXRemote } from "next-mdx-remote/rsc";
import remarkGfm from "remark-gfm";
import { getAllPosts, getAllSlugs, getPostBySlug } from "@/lib/blog";
import { getPublicBlogViews } from "@/lib/publicApi";
import BlogViewCounter from "@/components/BlogViewCounter";
import PassRateBar from "@/components/blog/PassRateBar";
import StatBar from "@/components/blog/StatBar";
import DistributionBar from "@/components/blog/DistributionBar";
import Highlight from "@/components/blog/Highlight";
import { Badge, ButtonLink, Card, Container } from "@/components/ui";
import { certFromBlogCategory } from "@/lib/cert-tokens";

const mdxComponents = { PassRateBar, StatBar, DistributionBar, Highlight };

type Params = { slug: string };

export function generateStaticParams() {
  return getAllSlugs().map((slug) => ({ slug }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<Params>;
}): Promise<Metadata> {
  const { slug } = await params;
  const post = getPostBySlug(slug);
  if (!post) return { title: "글을 찾을 수 없습니다" };

  return {
    title: post.title,
    description: post.description,
    alternates: { canonical: `https://www.sqldpass.com/blog/${slug}` },
    openGraph: {
      type: "article",
      title: `${post.title} | SQLD Pass`,
      description: post.description,
      url: `https://www.sqldpass.com/blog/${slug}`,
      publishedTime: post.date,
    },
    twitter: {
      card: "summary_large_image",
      title: post.title,
      description: post.description,
    },
  };
}

export default async function BlogPostPage({
  params,
}: {
  params: Promise<Params>;
}) {
  const { slug } = await params;
  const post = getPostBySlug(slug);
  if (!post) notFound();

  const allPosts = getAllPosts();
  const currentIdx = allPosts.findIndex((p) => p.slug === slug);
  const prevPost = currentIdx < allPosts.length - 1 ? allPosts[currentIdx + 1] : null;
  const nextPost = currentIdx > 0 ? allPosts[currentIdx - 1] : null;

  let viewCount = 0;
  try {
    const views = await getPublicBlogViews();
    viewCount = views[slug] ?? 0;
  } catch {
    /* 백엔드 미연결 시 무시 */
  }

  const articleLd = {
    "@context": "https://schema.org",
    "@type": "Article",
    headline: post.title,
    description: post.description,
    datePublished: post.date,
    author: { "@type": "Organization", name: "SQLD Pass" },
    publisher: { "@type": "Organization", name: "SQLD Pass" },
    mainEntityOfPage: `https://www.sqldpass.com/blog/${slug}`,
  };

  const cert = certFromBlogCategory(post.category);

  return (
    <Container size="narrow" className="py-12">
      <BlogViewCounter slug={slug} />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(articleLd) }}
      />

      <nav className="text-sm text-text-muted">
        <Link href="/blog" className="hover:text-text">
          시험 준비 팁
        </Link>
        <span className="mx-2">/</span>
        <span>{post.category}</span>
      </nav>

      <header className="mx-auto mt-6 max-w-[44rem]">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          {cert ? (
            <Badge cert={cert} variant="soft" size="sm">
              {post.category}
            </Badge>
          ) : (
            <Badge variant="soft" tone="neutral" size="sm">
              {post.category}
            </Badge>
          )}
          <span className="text-text-muted">
            {new Date(post.date).toLocaleDateString("ko-KR", {
              year: "numeric",
              month: "long",
              day: "numeric",
            })}
          </span>
          <span className="text-text-subtle">· {post.readingTime}</span>
          {viewCount > 0 && (
            <span className="text-text-subtle">· 조회 {viewCount.toLocaleString()}</span>
          )}
        </div>
        <h1 className="mt-5 text-[1.875rem] font-bold leading-[1.25] tracking-tight sm:text-[2.25rem] md:text-[2.5rem]">
          {post.title}
        </h1>
        <p className="mt-5 text-base leading-relaxed text-text-muted sm:text-lg">
          {post.description}
        </p>
        <p className="mt-3 text-sm text-text-subtle">
          안녕하세요. 문어입니다 🐙
        </p>
        <hr className="mt-8 border-border" />
      </header>

      <article className="prose-custom mt-10">
        <MDXRemote
          source={post.content}
          options={{ mdxOptions: { remarkPlugins: [remarkGfm] } }}
          components={mdxComponents}
        />
      </article>

      <Card padding="lg" className="mt-12 text-center">
        <h2 className="text-lg font-semibold tracking-tight">직접 문제를 풀어보세요</h2>
        <p className="mt-2 text-sm text-text-muted">
          매번 새로운 모의고사와 무한 풀이 모드로 실전 감각을 키울 수 있습니다.
        </p>
        <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
          <ButtonLink href="/mock-exams" variant="primary" size="md" glow>
            모의고사 풀기
          </ButtonLink>
          <ButtonLink href="/solve" variant="outline" size="md">
            카테고리별 풀기
          </ButtonLink>
        </div>
      </Card>

      <nav className="mt-10 grid gap-4 sm:grid-cols-2">
        {prevPost && (
          <Link
            href={`/blog/${prevPost.slug}`}
            className="rounded-xl border border-border bg-surface p-4 transition-all hover:border-primary/40 hover:shadow-[var(--shadow-md)]"
          >
            <span className="text-xs text-text-muted">이전 글</span>
            <p className="mt-1 text-sm font-semibold">{prevPost.title}</p>
          </Link>
        )}
        {nextPost && (
          <Link
            href={`/blog/${nextPost.slug}`}
            className="rounded-xl border border-border bg-surface p-4 text-right transition-all hover:border-primary/40 hover:shadow-[var(--shadow-md)] sm:col-start-2"
          >
            <span className="text-xs text-text-muted">다음 글</span>
            <p className="mt-1 text-sm font-semibold">{nextPost.title}</p>
          </Link>
        )}
      </nav>
    </Container>
  );
}
