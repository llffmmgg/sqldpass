import type { BlogPostMeta } from "@/lib/blog";
import type { CategoryMeta, GroupedPosts } from "@/lib/blogGroups";
import CategoryPostCard from "./CategoryPostCard";

type Props = {
  section: GroupedPosts;
  meta: CategoryMeta;
  viewCounts: Record<string, number>;
  /** 단일 섹션일 때는 타이틀을 감춘다 */
  showHeader?: boolean;
};

/**
 * 한 섹션(그룹)을 렌더. 헤더 + 카드 그리드.
 */
export default function CategoryPostGroup({
  section,
  meta,
  viewCounts,
  showHeader = true,
}: Props) {
  return (
    <section aria-label={section.label || meta.label}>
      {showHeader && section.label && (
        <header className="mb-4 flex flex-wrap items-baseline gap-x-3 gap-y-1">
          <div className="flex items-center gap-2">
            <span className="text-lg" aria-hidden="true">
              {section.icon}
            </span>
            <h2 className="text-lg font-bold tracking-tight sm:text-xl">
              {section.label}
            </h2>
            <span className="inline-flex items-center rounded-full bg-foreground/[0.05] px-2 py-0.5 text-[11px] font-medium text-muted tabular-nums">
              {section.posts.length}
            </span>
          </div>
          {section.description && (
            <p className="text-xs text-muted sm:text-sm">{section.description}</p>
          )}
        </header>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {section.posts.map((post: BlogPostMeta) => (
          <CategoryPostCard
            key={post.slug}
            post={post}
            meta={meta}
            views={viewCounts[post.slug] ?? 0}
          />
        ))}
      </div>
    </section>
  );
}
