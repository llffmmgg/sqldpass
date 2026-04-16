import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getAllCategories, getPostsByCategory } from "@/lib/blog";
import { getPublicBlogViews } from "@/lib/publicApi";
import { getCategoryMeta, groupPostsByMeta } from "@/lib/blogGroups";
import CategoryHero from "@/components/blog/CategoryHero";
import CategoryPostGroup from "@/components/blog/CategoryPostGroup";

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

  const meta = getCategoryMeta(decoded);
  const sections = groupPostsByMeta(decoded, posts);
  const isSingleSection = sections.length === 1 && sections[0].key === "all";

  // 해당 카테고리 posts 기준 총 조회수
  const totalViews = posts.reduce(
    (sum, p) => sum + (viewCounts[p.slug] ?? 0),
    0,
  );

  return (
    <main className="mx-auto max-w-6xl px-4 py-10 sm:px-6 sm:py-12 lg:px-8">
      {/* 브레드크럼 */}
      <nav aria-label="Breadcrumb" className="mb-5 text-xs text-muted">
        <ol className="flex flex-wrap items-center gap-1.5">
          <li>
            <Link href="/blog" className="hover:text-foreground transition-colors">
              시험 준비 팁
            </Link>
          </li>
          <li className="text-muted/40" aria-hidden="true">/</li>
          <li className="text-foreground/80">{meta.label || decoded}</li>
        </ol>
      </nav>

      <CategoryHero meta={meta} posts={posts} totalViews={totalViews} />

      <div className="mt-8 space-y-10">
        {sections.map((section) => (
          <CategoryPostGroup
            key={section.key}
            section={section}
            meta={meta}
            viewCounts={viewCounts}
            showHeader={!isSingleSection}
          />
        ))}
      </div>
    </main>
  );
}
