import type { BlogPostMeta } from "@/lib/blog";
import type { CategoryMeta } from "@/lib/blogGroups";
import { sumReadingMinutes } from "@/lib/blogGroups";

type Props = {
  meta: CategoryMeta;
  posts: BlogPostMeta[];
  totalViews: number;
};

function formatKoreanDate(iso: string): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  const month = d.getMonth() + 1;
  const day = d.getDate();
  return `${month}.${day}`;
}

/**
 * 카테고리 페이지 상단의 슬림한 per-category 헤더.
 * 한 행에 이모지·제목·설명을 배치하고 stats를 컴팩트하게 노출.
 */
export default function CategoryHero({ meta, posts, totalViews }: Props) {
  const latestDate = posts[0]?.date ?? "";
  const totalMinutes = sumReadingMinutes(posts);

  return (
    <section
      className={`relative overflow-hidden rounded-2xl border bg-gradient-to-br ${meta.gradient} ${meta.border} p-4 sm:p-5`}
    >
      <div className="flex items-center gap-3 sm:gap-4">
        <div
          className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${meta.iconBg}`}
          aria-hidden="true"
        >
          <span className="text-xl sm:text-2xl">{meta.emoji}</span>
        </div>
        <div className="min-w-0 flex-1">
          <h1 className="text-lg font-bold tracking-tight sm:text-xl">
            {meta.label || meta.name}
          </h1>
          <p className="mt-0.5 line-clamp-1 text-xs text-muted sm:text-sm">
            {meta.description}
          </p>
        </div>
      </div>

      <dl className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] tabular-nums text-muted sm:text-xs">
        <div className="flex items-baseline gap-1">
          <dt className="text-muted/70">글</dt>
          <dd className="font-semibold text-foreground">{posts.length}편</dd>
        </div>
        {totalViews > 0 && (
          <>
            <span className="h-2.5 w-px bg-border/60" aria-hidden />
            <div className="flex items-baseline gap-1">
              <dt className="text-muted/70">조회</dt>
              <dd className="font-semibold text-foreground">
                {totalViews.toLocaleString()}
              </dd>
            </div>
          </>
        )}
        {totalMinutes > 0 && (
          <>
            <span className="h-2.5 w-px bg-border/60" aria-hidden />
            <div className="flex items-baseline gap-1">
              <dt className="text-muted/70">전체 읽기</dt>
              <dd className="font-semibold text-foreground">약 {totalMinutes}분</dd>
            </div>
          </>
        )}
        {latestDate && (
          <>
            <span className="h-2.5 w-px bg-border/60" aria-hidden />
            <div className="flex items-baseline gap-1">
              <dt className="text-muted/70">최근 업데이트</dt>
              <dd className="font-semibold text-foreground">
                {formatKoreanDate(latestDate)}
              </dd>
            </div>
          </>
        )}
      </dl>
    </section>
  );
}
