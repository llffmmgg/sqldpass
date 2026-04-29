import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getAllCategories, getPostsByCategory, type BlogPostMeta } from "@/lib/blog";
import { getPublicBlogViews } from "@/lib/publicApi";
import { groupPostsByMeta } from "@/lib/blogGroups";
import { Badge, Card, Container } from "@/components/ui";
import { certFromBlogCategory, certFromExamType, CERT_TOKENS } from "@/lib/cert-tokens";
import {
  pastExamBlogDescription,
  pastExamBlogSlug,
  pastExamBlogTitle,
} from "@/lib/pastExamBlog";
import { flattenPastExamLists, loadPastExamListsByCert } from "@/lib/pastExamCatalog";

export const revalidate = 1800;

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
  const mdxPosts = getPostsByCategory(decoded);

  // 기출 복원 회차 → 가상 BlogPostMeta 합류 (decoded 카테고리만 필터)
  const listsByCert = await loadPastExamListsByCert().catch(() => null);
  const pastExamPosts: BlogPostMeta[] = listsByCert
    ? flattenPastExamLists(listsByCert)
        .map((exam) => {
          const cert = certFromExamType(exam.examType);
          const examCategory = cert ? CERT_TOKENS[cert].blogCategory : "일반";
          return {
            slug: `past-exam/${pastExamBlogSlug(exam)}`,
            title: pastExamBlogTitle(exam),
            description: pastExamBlogDescription(exam),
            date: exam.createdAt.slice(0, 10),
            category: examCategory,
            tags: ["기출 복원", examCategory],
            readingTime: `${Math.max(5, Math.round(exam.totalQuestions / 4))}분`,
            group: "기출 복원",
          } satisfies BlogPostMeta;
        })
        .filter((p) => p.category === decoded)
    : [];

  const posts: BlogPostMeta[] = [...mdxPosts, ...pastExamPosts].sort((a, b) =>
    a.date > b.date ? -1 : 1,
  );

  if (posts.length === 0) notFound();

  let viewCounts: Record<string, number> = {};
  try {
    viewCounts = await getPublicBlogViews();
  } catch {
    /* 백엔드 미연결 시 무시 */
  }

  const viewKeyOf = (slug: string) =>
    slug.startsWith("past-exam/") ? `past-exam-${slug.slice("past-exam/".length)}` : slug;

  const cert = certFromBlogCategory(decoded);
  const barClass = cert ? CERT_TOKENS[cert].tailwind.bg : "bg-primary";
  const [featured, ...rest] = posts;
  const groupedSections = groupPostsByMeta(decoded, rest);
  const showGroupHeaders =
    groupedSections.length > 1 || (groupedSections[0]?.label ?? "") !== "";

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
                    조회 {(viewCounts[viewKeyOf(featured.slug)] ?? 0).toLocaleString()}
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

        {groupedSections.map((section) => (
          <section key={section.key} className="mt-8 first:mt-6">
            {showGroupHeaders && section.label && (
              <div className="mb-4 flex items-baseline justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span className="text-lg" aria-hidden>
                    {section.icon}
                  </span>
                  <h2 className="text-lg font-semibold tracking-tight sm:text-xl">
                    {section.label}
                  </h2>
                  <span className="text-xs text-text-muted">
                    {section.posts.length}편
                  </span>
                </div>
                {section.description && (
                  <p className="hidden text-xs text-text-muted sm:block">
                    {section.description}
                  </p>
                )}
              </div>
            )}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {section.posts.map((post) => {
                const views = viewCounts[viewKeyOf(post.slug)] ?? 0;
                return (
                  <Link key={post.slug} href={`/blog/${post.slug}`} className="group block h-full">
                    <Card variant="interactive" padding="none" className="flex h-full flex-col overflow-hidden">
                      <div className={`h-0.5 w-full ${barClass}`} />
                      <div className="flex flex-1 flex-col gap-2 p-4">
                        <h3 className="text-[15px] font-semibold leading-snug group-hover:text-primary line-clamp-2">
                          {post.title}
                        </h3>
                        <div className="mt-auto flex items-center gap-2 text-[11px] text-text-subtle">
                          <span>
                            {new Date(post.date).toLocaleDateString("ko-KR", { month: "long", day: "numeric" })}
                          </span>
                          <span>·</span>
                          <span>{post.readingTime}</span>
                          <span>·</span>
                          <span>조회 {views.toLocaleString()}</span>
                        </div>
                      </div>
                    </Card>
                  </Link>
                );
              })}
            </div>
          </section>
        ))}
      </div>
    </Container>
  );
}
