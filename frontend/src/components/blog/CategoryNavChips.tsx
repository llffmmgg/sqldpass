import Link from "next/link";
import { CATEGORIES, getCategoryMeta } from "@/lib/blogGroups";

type Props = {
  current: string;
  /** 카테고리별 글 수 맵 */
  counts: Record<string, number>;
};

/**
 * 카테고리 페이지 상단에 붙는 수평 스크롤 chip 네비게이션.
 * - NavBar(h-16 근방) 아래에 sticky로 따라오도록 top-16
 * - 현재 카테고리는 테마 색상으로 강조
 * - 카운트 0인 카테고리는 숨김
 */
export default function CategoryNavChips({ current, counts }: Props) {
  return (
    <nav
      aria-label="카테고리 전환"
      className="sticky top-16 z-20 -mx-4 border-b border-border/60 bg-background/80 px-4 py-3 backdrop-blur sm:-mx-6 sm:px-6 lg:-mx-8 lg:px-8"
    >
      <div className="flex gap-2 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        <Link
          href="/blog"
          className="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-border bg-surface/70 px-3 py-1.5 text-xs font-medium text-muted transition-colors hover:border-foreground/30 hover:text-foreground"
        >
          <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
          전체 블로그
        </Link>

        <span className="h-6 w-px shrink-0 self-center bg-border/60" aria-hidden />

        {CATEGORIES.map((cat) => {
          const count = counts[cat.name] ?? 0;
          if (count === 0) return null;
          const isCurrent = cat.name === current;
          const meta = getCategoryMeta(cat.name);
          return (
            <Link
              key={cat.name}
              href={`/blog/category/${encodeURIComponent(cat.slug)}`}
              aria-current={isCurrent ? "page" : undefined}
              className={
                isCurrent
                  ? `inline-flex shrink-0 items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-semibold ring-1 ring-inset ${meta.badge} ${meta.border}`
                  : "inline-flex shrink-0 items-center gap-1.5 rounded-full border border-border bg-surface/50 px-3 py-1.5 text-xs font-medium text-muted transition-colors hover:border-foreground/30 hover:text-foreground"
              }
            >
              <span aria-hidden="true">{cat.emoji}</span>
              <span>{cat.label}</span>
              <span
                className={
                  isCurrent
                    ? "ml-0.5 rounded-full bg-background/50 px-1.5 text-[10px] tabular-nums"
                    : "ml-0.5 rounded-full bg-foreground/[0.06] px-1.5 text-[10px] tabular-nums text-muted/80"
                }
              >
                {count}
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
