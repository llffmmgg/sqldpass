import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { MDXRemote } from "next-mdx-remote/rsc";
import remarkGfm from "remark-gfm";
import { getAllPosts, getAllSlugs, getPostBySlug } from "@/lib/blog";
import BlogViewCounter from "@/components/BlogViewCounter";
import PassRateBar from "@/components/blog/PassRateBar";
import StatBar from "@/components/blog/StatBar";
import DistributionBar from "@/components/blog/DistributionBar";
import Highlight from "@/components/blog/Highlight";

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

const CATEGORY_COLORS: Record<string, string> = {
  SQLD: "border-primary/30 bg-primary/10 text-primary",
  정보처리기사: "border-accent/30 bg-accent/10 text-accent",
  컴퓨터활용능력: "border-blue-600/30 bg-blue-600/10 text-blue-600",
};

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

  const categoryStyle =
    CATEGORY_COLORS[post.category] ??
    "border-violet-500/30 bg-violet-500/10 text-violet-300";

  return (
    <main className="mx-auto max-w-3xl px-4 py-12 sm:px-6 lg:px-8">
      <BlogViewCounter slug={slug} />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(articleLd) }}
      />

      <nav className="text-sm text-muted">
        <Link href="/blog" className="hover:text-foreground">
          시험 준비 팁
        </Link>
        <span className="mx-2">/</span>
        <span>{post.category}</span>
      </nav>

      <header className="mt-6">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <span
            className={`inline-flex items-center rounded-md border px-2 py-0.5 font-medium ${categoryStyle}`}
          >
            {post.category}
          </span>
          <span className="text-muted">
            {new Date(post.date).toLocaleDateString("ko-KR", {
              year: "numeric",
              month: "long",
              day: "numeric",
            })}
          </span>
          <span className="text-muted/60">{post.readingTime}</span>
        </div>
        <h1 className="mt-4 text-3xl font-bold leading-tight sm:text-4xl">
          {post.title}
        </h1>
        <p className="mt-3 text-lg text-muted">안녕하세요. 문어입니다.🐙</p>
        <p className="mt-2 text-lg text-muted">{post.description}</p>
      </header>

      <article className="prose-custom mt-10">
        <MDXRemote source={post.content} options={{ mdxOptions: { remarkPlugins: [remarkGfm] } }} components={mdxComponents} />
      </article>

      <section className="mt-12 rounded-xl border border-border bg-surface/50 p-6 text-center">
        <h2 className="text-lg font-semibold">
          직접 문제를 풀어보세요
        </h2>
        <p className="mt-2 text-sm text-muted">
          매번 새로운 모의고사와 무한 풀이 모드로 실전 감각을 키울 수 있습니다.
        </p>
        <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
          <Link
            href="/mock-exams"
            className="btn-glow inline-flex items-center rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-zinc-900 transition-all hover:bg-primary-hover"
          >
            모의고사 풀기
          </Link>
          <Link
            href="/solve"
            className="inline-flex items-center rounded-lg border border-border px-5 py-2.5 text-sm font-semibold text-foreground transition-all hover:border-violet-500/50"
          >
            카테고리별 풀기
          </Link>
        </div>
      </section>

      <nav className="mt-10 grid gap-4 sm:grid-cols-2">
        {prevPost && (
          <Link
            href={`/blog/${prevPost.slug}`}
            className="rounded-xl border border-border p-4 transition-all hover:border-primary/40"
          >
            <span className="text-xs text-muted">이전 글</span>
            <p className="mt-1 text-sm font-semibold">{prevPost.title}</p>
          </Link>
        )}
        {nextPost && (
          <Link
            href={`/blog/${nextPost.slug}`}
            className="rounded-xl border border-border p-4 text-right transition-all hover:border-primary/40 sm:col-start-2"
          >
            <span className="text-xs text-muted">다음 글</span>
            <p className="mt-1 text-sm font-semibold">{nextPost.title}</p>
          </Link>
        )}
      </nav>
    </main>
  );
}
