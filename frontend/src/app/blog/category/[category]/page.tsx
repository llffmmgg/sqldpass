import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getAllCategories, getPostsByCategory } from "@/lib/blog";
import { getPublicBlogViews } from "@/lib/publicApi";
import { Badge, Card, Container } from "@/components/ui";
import { certFromBlogCategory, CERT_TOKENS } from "@/lib/cert-tokens";

type Params = { category: string };

export function generateStaticParams() {
  return getAllCategories().map(({ category }) => ({ category }));
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

  const cert = certFromBlogCategory(decoded);
  const barClass = cert ? CERT_TOKENS[cert].tailwind.bg : "bg-primary";
  const [featured, ...rest] = posts;

  return (
    <Container size="default" className="py-12">
      <div className="mb-10">
        <Link href="/blog" className="text-sm text-text-muted transition-colors hover:text-text">
          ← 블로그
        </Link>
        <h1 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">{decoded}</h1>
        <p className="mt-2 text-base text-text-muted">{posts.length}개의 글</p>
      </div>

      <div className="space-y-4">
        {featured && (
          <Link href={`/blog/${featured.slug}`} className="group block">
            <Card variant="interactive" padding="none" className="flex flex-col overflow-hidden">
              <div className={`h-1 w-full ${barClass}`} />
              <div className="flex flex-1 flex-col p-7">
                <div className="flex flex-wrap items-center gap-2 text-xs">
                  {cert ? (
                    <Badge cert={cert} variant="soft" size="sm">
                      {featured.category}
                    </Badge>
                  ) : (
                    <Badge variant="soft" tone="neutral" size="sm">
                      {featured.category}
                    </Badge>
                  )}
                  <span className="text-text-muted">
                    {new Date(featured.date).toLocaleDateString("ko-KR", {
                      year: "numeric",
                      month: "long",
                      day: "numeric",
                    })}
                  </span>
                  <span className="text-text-subtle">{featured.readingTime}</span>
                  <span className="text-text-subtle">
                    조회 {(viewCounts[featured.slug] ?? 0).toLocaleString()}
                  </span>
                </div>
                <h2 className="mt-4 text-2xl font-bold leading-tight tracking-tight group-hover:text-primary sm:text-3xl">
                  {featured.title}
                </h2>
                <p className="mt-3 max-w-2xl text-base leading-relaxed text-text-muted">
                  {featured.description}
                </p>
                {featured.tags && featured.tags.length > 0 && (
                  <div className="mt-4 flex flex-wrap gap-1.5">
                    {featured.tags.slice(0, 4).map((tag) => (
                      <span
                        key={tag}
                        className="rounded-md border border-border bg-surface px-2 py-0.5 text-[11px] text-text-muted"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                )}
                <span className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-primary">
                  읽어보기
                  <svg
                    className="h-3.5 w-3.5 transition-transform group-hover:translate-x-1"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={2.5}
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>
                </span>
              </div>
            </Card>
          </Link>
        )}

        {rest.length > 0 && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {rest.map((post) => {
              const views = viewCounts[post.slug] ?? 0;
              return (
                <Link key={post.slug} href={`/blog/${post.slug}`} className="group block h-full">
                  <Card variant="interactive" padding="none" className="flex h-full flex-col overflow-hidden">
                    <div className={`h-0.5 w-full ${barClass}`} />
                    <div className="flex flex-1 flex-col p-5">
                      <div className="text-[11px] text-text-subtle">{post.readingTime}</div>
                      <h2 className="mt-2 text-base font-semibold leading-snug group-hover:text-primary">
                        {post.title}
                      </h2>
                      <p className="mt-2 flex-1 text-sm leading-relaxed text-text-muted line-clamp-2">
                        {post.description}
                      </p>
                      <div className="mt-3 flex items-center justify-between text-[11px] text-text-subtle">
                        <span>
                          {new Date(post.date).toLocaleDateString("ko-KR", { month: "long", day: "numeric" })}
                        </span>
                        <span>조회 {views.toLocaleString()}</span>
                      </div>
                    </div>
                  </Card>
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </Container>
  );
}
