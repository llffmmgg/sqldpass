"use client";

import Image from "next/image";
import Link from "next/link";
import type { BlogPostMeta } from "@/lib/blog";
import { Badge, Card, Container } from "@/components/ui";
import { certFromBlogCategory } from "@/lib/cert-tokens";

type CategoryMeta = {
  name: string;
  slug: string;
  label: string;
  iconBg: string;
  emoji: string;
};

const CATEGORIES: CategoryMeta[] = [
  { name: "SQLD", slug: "SQLD", label: "SQLD", iconBg: "bg-amber-500/10", emoji: "🗃️" },
  { name: "정보처리기사", slug: "정보처리기사", label: "정보처리기사 실기", iconBg: "bg-emerald-500/10", emoji: "💻" },
  { name: "정보처리기사 필기", slug: "정보처리기사 필기", label: "정보처리기사 필기", iconBg: "bg-rose-500/10", emoji: "📝" },
  { name: "컴퓨터활용능력", slug: "컴퓨터활용능력", label: "컴퓨터활용능력", iconBg: "bg-sky-500/10", emoji: "📊" },
  { name: "컴퓨터활용능력 2급", slug: "컴퓨터활용능력 2급", label: "컴퓨터활용능력 2급", iconBg: "bg-indigo-500/10", emoji: "📋" },
  { name: "ADsP", slug: "ADsP", label: "ADsP", iconBg: "bg-teal-500/10", emoji: "📈" },
  { name: "일반", slug: "일반", label: "시험 팁", iconBg: "bg-primary/10", emoji: "🎯" },
];

const CATEGORY_BY_NAME: Record<string, CategoryMeta> = Object.fromEntries(
  CATEGORIES.map((c) => [c.name, c]),
);

function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
}

export default function BlogList({
  posts,
  categories,
  viewCounts,
}: {
  posts: BlogPostMeta[];
  categories: { category: string; count: number }[];
  viewCounts: Record<string, number>;
  recommendedPosts: BlogPostMeta[];
}) {
  const totalViews = Object.values(viewCounts).reduce((s, v) => s + v, 0);
  const countMap = Object.fromEntries(categories.map((c) => [c.category, c.count]));
  const orderedCategories = CATEGORIES.filter((c) => (countMap[c.name] ?? 0) > 0);

  const popularPosts = [...posts]
    .map((p) => ({ post: p, views: viewCounts[p.slug] ?? 0 }))
    .sort((a, b) => b.views - a.views)
    .filter((x) => x.views > 0)
    .slice(0, 5)
    .map((x) => x.post);

  const recentPosts = posts.slice(0, 5);

  return (
    <Container size="default" className="py-12">
      <header className="mb-10 flex items-center gap-5">
        <Image
          src="/blog-mascot.webp"
          alt="시험 준비 팁 마스코트"
          width={96}
          height={96}
          className="shrink-0"
          priority
        />
        <div>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">시험 준비 팁</h1>
          <p className="mt-2 max-w-lg text-sm text-text-muted">
            자격증별 학습 전략과 합격 노하우를 정리했어요.
          </p>
          <div className="mt-2 flex items-center gap-3 text-xs text-text-subtle">
            <span>{posts.length}개의 글</span>
            {totalViews > 0 && (
              <>
                <span className="h-3 w-px bg-border" />
                <span>총 조회 {totalViews.toLocaleString()}</span>
              </>
            )}
          </div>
        </div>
      </header>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_280px]">
        <main>
          <h2 className="mb-4 text-xs font-semibold uppercase tracking-wider text-text-muted">
            전체 글 ({posts.length})
          </h2>
          <ul className="divide-y divide-border border-y border-border">
            {posts.map((post) => (
              <li key={post.slug}>
                <PostListItem post={post} views={viewCounts[post.slug] ?? 0} />
              </li>
            ))}
          </ul>
        </main>

        <aside className="lg:sticky lg:top-20 lg:self-start lg:max-h-[calc(100vh-6rem)] lg:overflow-y-auto">
          <SidebarSection title="카테고리">
            <ul className="space-y-1">
              {orderedCategories.map((cat) => (
                <li key={cat.name}>
                  <Link
                    href={`/blog/category/${encodeURIComponent(cat.slug)}`}
                    className="flex items-center justify-between rounded-md px-3 py-2 text-sm transition-colors hover:bg-surface-hover"
                  >
                    <span className="flex items-center gap-2 text-text">
                      <span className={`flex h-6 w-6 items-center justify-center rounded ${cat.iconBg} text-sm`}>
                        {cat.emoji}
                      </span>
                      {cat.label}
                    </span>
                    <span className="text-xs tabular-nums text-text-muted">
                      {countMap[cat.name]}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </SidebarSection>

          {popularPosts.length > 0 && (
            <SidebarSection title="인기 글" className="mt-8">
              <ol className="space-y-2.5">
                {popularPosts.map((post, i) => (
                  <li key={post.slug}>
                    <Link
                      href={`/blog/${post.slug}`}
                      className="group flex gap-3 rounded-md p-2 text-sm transition-colors hover:bg-surface-hover"
                    >
                      <span className="shrink-0 text-base font-bold tabular-nums text-text-muted">
                        {i + 1}
                      </span>
                      <span className="flex-1">
                        <span className="line-clamp-2 font-medium leading-snug group-hover:text-primary">
                          {post.title}
                        </span>
                        <span className="mt-0.5 block text-xs text-text-subtle">
                          조회 {(viewCounts[post.slug] ?? 0).toLocaleString()}
                        </span>
                      </span>
                    </Link>
                  </li>
                ))}
              </ol>
            </SidebarSection>
          )}

          <SidebarSection title="최근 글" className="mt-8">
            <ul className="space-y-2.5">
              {recentPosts.map((post) => (
                <li key={post.slug}>
                  <Link
                    href={`/blog/${post.slug}`}
                    className="group block rounded-md p-2 text-sm transition-colors hover:bg-surface-hover"
                  >
                    <span className="line-clamp-2 font-medium leading-snug group-hover:text-primary">
                      {post.title}
                    </span>
                    <span className="mt-0.5 block text-xs text-text-subtle">
                      {formatDate(post.date)}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </SidebarSection>
        </aside>
      </div>
    </Container>
  );
}

function PostListItem({ post, views }: { post: BlogPostMeta; views: number }) {
  const cert = certFromBlogCategory(post.category);
  const meta = CATEGORY_BY_NAME[post.category];

  return (
    <Link href={`/blog/${post.slug}`} className="group flex items-start gap-4 py-5 transition-colors">
      <div
        className={`flex h-20 w-20 shrink-0 items-center justify-center rounded-lg ${
          meta?.iconBg ?? "bg-surface-hover"
        } sm:h-24 sm:w-24`}
        aria-hidden
      >
        <span className="text-3xl sm:text-4xl">{meta?.emoji ?? "📚"}</span>
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          {cert ? (
            <Badge cert={cert} variant="soft" size="xs">
              {post.category}
            </Badge>
          ) : (
            <Badge variant="soft" tone="neutral" size="xs">
              {post.category}
            </Badge>
          )}
          <span className="text-text-muted">{formatDate(post.date)}</span>
          <span className="text-text-subtle">·</span>
          <span className="text-text-subtle">{post.readingTime}</span>
          {views > 0 && (
            <>
              <span className="text-text-subtle">·</span>
              <span className="text-text-subtle">조회 {views.toLocaleString()}</span>
            </>
          )}
        </div>
        <h3 className="mt-2 text-base font-semibold leading-snug text-text group-hover:text-primary sm:text-lg">
          {post.title}
        </h3>
        {post.description && (
          <p className="mt-1.5 line-clamp-2 text-sm leading-relaxed text-text-muted">
            {post.description}
          </p>
        )}
      </div>
    </Link>
  );
}

function SidebarSection({
  title,
  className,
  children,
}: {
  title: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <Card padding="md" className={className}>
      <h3 className="mb-3 text-xs font-semibold uppercase tracking-wider text-text-muted">
        {title}
      </h3>
      {children}
    </Card>
  );
}
