import Link from "next/link";
import type { BlogPostMeta } from "@/lib/blog";
import type { CategoryMeta } from "@/lib/blogGroups";

type Props = {
  post: BlogPostMeta;
  meta: CategoryMeta;
  views: number;
  /** 초기엔 grid 만 사용. list는 향후 확장용. */
  variant?: "grid" | "list";
};

function formatDate(iso: string): string {
  if (!iso) return "";
  return new Date(iso).toLocaleDateString("ko-KR", {
    month: "long",
    day: "numeric",
  });
}

/**
 * 카테고리 페이지에서 사용하는 공통 포스트 카드.
 * - 상단 2px 색상 바, 제목·설명·태그·메타 라인
 * - hover 시 살짝 떠오르고 색상 링 강조
 */
export default function CategoryPostCard({
  post,
  meta,
  views,
  variant = "grid",
}: Props) {
  if (variant === "list") {
    return (
      <Link
        href={`/blog/${post.slug}`}
        className="group flex items-start gap-4 rounded-xl border border-border bg-surface p-4 transition-all hover:border-foreground/25 hover:bg-surface/80"
      >
        <span className={`mt-1 h-8 w-0.5 rounded ${meta.bar}`} aria-hidden />
        <div className="min-w-0 flex-1">
          <h3 className="truncate text-sm font-semibold leading-snug group-hover:text-primary">
            {post.title}
          </h3>
          <p className="mt-1 line-clamp-1 text-xs leading-relaxed text-muted">
            {post.description}
          </p>
          <div className="mt-1.5 flex items-center gap-2 text-[11px] text-muted/60">
            <span>{post.readingTime}</span>
            <span>·</span>
            <span>{formatDate(post.date)}</span>
            {views > 0 && (
              <>
                <span>·</span>
                <span>조회 {views.toLocaleString()}</span>
              </>
            )}
          </div>
        </div>
      </Link>
    );
  }

  return (
    <Link
      href={`/blog/${post.slug}`}
      className="group relative flex flex-col overflow-hidden rounded-xl border border-border bg-surface transition-all duration-200 hover:-translate-y-0.5 hover:border-foreground/25 hover:shadow-lg"
    >
      <div className={`h-0.5 w-full ${meta.bar}`} aria-hidden />
      <div className="flex flex-1 flex-col p-5">
        <div className="flex items-center gap-2 text-[11px] text-muted/70">
          <span>{post.readingTime}</span>
          {views > 0 && (
            <>
              <span className="h-2.5 w-px bg-border/60" aria-hidden />
              <span>조회 {views.toLocaleString()}</span>
            </>
          )}
        </div>
        <h3 className="mt-2 text-base font-bold leading-snug tracking-tight group-hover:text-primary">
          {post.title}
        </h3>
        <p className="mt-2 flex-1 text-sm leading-relaxed text-muted line-clamp-2">
          {post.description}
        </p>
        {post.tags && post.tags.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1">
            {post.tags.slice(0, 3).map((tag) => (
              <span
                key={tag}
                className="rounded-md bg-foreground/[0.04] px-1.5 py-0.5 text-[10px] text-muted/80"
              >
                #{tag}
              </span>
            ))}
          </div>
        )}
        <div className="mt-3 flex items-center justify-between text-[11px] text-muted/60">
          <span>{formatDate(post.date)}</span>
          <span className="inline-flex items-center gap-1 text-primary opacity-0 transition-opacity group-hover:opacity-100">
            읽어보기
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
            </svg>
          </span>
        </div>
      </div>
    </Link>
  );
}
