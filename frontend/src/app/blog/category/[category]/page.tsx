import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getAllCategories, getPostsByCategory } from "@/lib/blog";
import { getPublicBlogViews } from "@/lib/publicApi";

type Params = { category: string };

export function generateStaticParams() {
  return getAllCategories().map(({ category }) => ({
    category,
  }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<Params>;
}): Promise<Metadata> {
  const { category } = await params;
  const decoded = decodeURIComponent(category);
  return {
    title: `${decoded} 시험 준비 글 모음`,
    description: `${decoded} 관련 시험 준비 팁과 학습 전략을 정리한 블로그 글 모음입니다.`,
    alternates: {
      canonical: `https://www.sqldpass.com/blog/category/${category}`,
    },
  };
}

const CATEGORY_ACCENT: Record<string, { badge: string; bar: string }> = {
  SQLD: { badge: "bg-primary/10 text-primary border-primary/30", bar: "bg-primary" },
  정보처리기사: { badge: "bg-accent/10 text-accent border-accent/30", bar: "bg-accent" },
  "정보처리기사 필기": { badge: "bg-purple-500/10 text-purple-500 border-purple-500/30", bar: "bg-purple-500" },
  컴퓨터활용능력: { badge: "bg-blue-600/10 text-blue-600 border-blue-600/30", bar: "bg-blue-600" },
  "컴퓨터활용능력 2급": { badge: "bg-indigo-500/10 text-indigo-500 border-indigo-500/30", bar: "bg-indigo-500" },
};

function getAccent(category: string) {
  return CATEGORY_ACCENT[category] ?? { badge: "bg-muted/10 text-muted border-muted/30", bar: "bg-muted" };
}

export default async function BlogCategoryPage({
  params,
}: {
  params: Promise<Params>;
}) {
  const { category } = await params;
  const decoded = decodeURIComponent(category);
  const posts = getPostsByCategory(decoded);

  if (posts.length === 0) notFound();

  let viewCounts: Record<string, number> = {};
  try {
    viewCounts = await getPublicBlogViews();
  } catch {
    /* 백엔드 미연결 시 무시 */
  }

  const accent = getAccent(decoded);
  const [featured, ...rest] = posts;

  return (
    <main className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8">
      {/* 헤더 */}
      <div className="mb-10">
        <Link href="/blog" className="text-sm text-muted transition-colors hover:text-foreground">
          ← 블로그
        </Link>
        <h1 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">{decoded}</h1>
        <p className="mt-2 text-base text-muted">{posts.length}개의 글</p>
      </div>

      <div className="space-y-4">
        {/* Featured */}
        {featured && (
          <Link
            href={`/blog/${featured.slug}`}
            className="group relative flex flex-col overflow-hidden rounded-2xl border border-border bg-surface transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-primary/5"
          >
            <div className={`h-1 w-full ${accent.bar}`} />
            <div className="flex flex-1 flex-col p-7">
              <div className="flex flex-wrap items-center gap-2 text-xs">
                <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 font-semibold ${accent.badge}`}>
                  {featured.category}
                </span>
                <span className="text-muted">
                  {new Date(featured.date).toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric" })}
                </span>
                <span className="text-muted/60">{featured.readingTime}</span>
                {(viewCounts[featured.slug] ?? 0) > 0 && (
                  <span className="text-muted/60">조회 {viewCounts[featured.slug].toLocaleString()}</span>
                )}
              </div>
              <h2 className="mt-4 text-2xl font-bold leading-tight tracking-tight group-hover:text-primary sm:text-3xl">
                {featured.title}
              </h2>
              <p className="mt-3 max-w-2xl text-base leading-relaxed text-muted">
                {featured.description}
              </p>
              {featured.tags && featured.tags.length > 0 && (
                <div className="mt-4 flex flex-wrap gap-1.5">
                  {featured.tags.slice(0, 4).map((tag) => (
                    <span key={tag} className="rounded-md bg-border/50 px-2 py-0.5 text-[11px] text-muted">{tag}</span>
                  ))}
                </div>
              )}
              <span className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-primary">
                읽어보기
                <svg className="h-3.5 w-3.5 transition-transform group-hover:translate-x-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                </svg>
              </span>
            </div>
          </Link>
        )}

        {/* 나머지 글 */}
        {rest.length > 0 && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {rest.map((post) => {
              const views = viewCounts[post.slug] ?? 0;
              return (
                <Link
                  key={post.slug}
                  href={`/blog/${post.slug}`}
                  className="group relative flex flex-col overflow-hidden rounded-xl border border-border bg-surface transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-primary/5"
                >
                  <div className={`h-0.5 w-full ${accent.bar}`} />
                  <div className="flex flex-1 flex-col p-5">
                    <div className="flex items-center gap-2 text-[11px]">
                      <span className="text-muted/60">{post.readingTime}</span>
                    </div>
                    <h2 className="mt-2 text-base font-bold leading-snug group-hover:text-primary">
                      {post.title}
                    </h2>
                    <p className="mt-2 flex-1 text-sm leading-relaxed text-muted line-clamp-2">
                      {post.description}
                    </p>
                    <div className="mt-3 flex items-center justify-between text-[11px] text-muted/60">
                      <span>{new Date(post.date).toLocaleDateString("ko-KR", { month: "long", day: "numeric" })}</span>
                      {views > 0 && <span>조회 {views.toLocaleString()}</span>}
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </main>
  );
}
