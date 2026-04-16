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
 * 카테고리 페이지 최상단의 per-category 히어로 섹션.
 * 카테고리별 이모지·색상·설명을 강조하고 스캐너블한 stats 라인 제공.
 */
export default function CategoryHero({ meta, posts, totalViews }: Props) {
  const latestDate = posts[0]?.date ?? "";
  const totalMinutes = sumReadingMinutes(posts);

  return (
    <section
      className={`relative overflow-hidden rounded-3xl border bg-gradient-to-br ${meta.gradient} ${meta.border} p-6 sm:p-8`}
    >
      <div className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-white/5 blur-3xl" />
      <div className="pointer-events-none absolute -left-8 bottom-0 h-40 w-40 rounded-full bg-white/[0.03] blur-2xl" />

      <div className="relative flex flex-col gap-5 sm:flex-row sm:items-center sm:gap-7">
        <div
          className={`flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl ${meta.iconBg} shadow-inner sm:h-20 sm:w-20`}
          aria-hidden="true"
        >
          <span className="text-3xl sm:text-4xl">{meta.emoji}</span>
        </div>
        <div className="min-w-0 flex-1">
          <p className={`text-xs font-semibold uppercase tracking-widest ${meta.text}`}>
            시험 준비 팁
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight sm:text-[32px] sm:leading-tight">
            {meta.label || meta.name}
          </h1>
          <p className="mt-2 max-w-xl text-sm leading-relaxed text-muted sm:text-base">
            {meta.description}
          </p>

          <dl className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs tabular-nums text-muted sm:text-sm">
            <div className="flex items-baseline gap-1.5">
              <dt className="text-muted/70">글</dt>
              <dd className="font-semibold text-foreground">{posts.length}편</dd>
            </div>
            {totalViews > 0 && (
              <>
                <span className="h-3 w-px bg-border/60" aria-hidden />
                <div className="flex items-baseline gap-1.5">
                  <dt className="text-muted/70">총 조회</dt>
                  <dd className="font-semibold text-foreground">
                    {totalViews.toLocaleString()}
                  </dd>
                </div>
              </>
            )}
            {totalMinutes > 0 && (
              <>
                <span className="h-3 w-px bg-border/60" aria-hidden />
                <div className="flex items-baseline gap-1.5">
                  <dt className="text-muted/70">전체 읽기</dt>
                  <dd className="font-semibold text-foreground">약 {totalMinutes}분</dd>
                </div>
              </>
            )}
            {latestDate && (
              <>
                <span className="h-3 w-px bg-border/60" aria-hidden />
                <div className="flex items-baseline gap-1.5">
                  <dt className="text-muted/70">최근 업데이트</dt>
                  <dd className="font-semibold text-foreground">
                    {formatKoreanDate(latestDate)}
                  </dd>
                </div>
              </>
            )}
          </dl>
        </div>
      </div>
    </section>
  );
}
