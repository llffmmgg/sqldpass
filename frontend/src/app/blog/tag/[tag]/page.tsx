import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getAllTags, getPostsByTag } from "@/lib/blog";
import { getPublicBlogViews } from "@/lib/publicApi";

type Params = { tag: string };

export function generateStaticParams() {
  return getAllTags().map(({ tag }) => ({ tag: encodeURIComponent(tag) }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<Params>;
}): Promise<Metadata> {
  const { tag } = await params;
  const decoded = decodeURIComponent(tag);
  return {
    title: `#${decoded} 관련 글`,
    description: `${decoded} 태그가 포함된 블로그 글 모음`,
  };
}

const CATEGORY_COLORS: Record<string, string> = {
  SQLD: "bg-amber-500/10 text-amber-300 border-amber-500/30",
  정처기: "bg-emerald-500/10 text-emerald-300 border-emerald-500/30",
  컴활: "bg-blue-500/10 text-blue-300 border-blue-500/30",
};

function getCategoryStyle(category: string) {
  return (
    CATEGORY_COLORS[category] ??
    "bg-violet-500/10 text-violet-300 border-violet-500/30"
  );
}

export default async function BlogTagPage({
  params,
}: {
  params: Promise<Params>;
}) {
  const { tag } = await params;
  const decoded = decodeURIComponent(tag);
  const posts = getPostsByTag(decoded);
  const allTags = getAllTags();

  if (posts.length === 0) notFound();

  let viewCounts: Record<string, number> = {};
  try {
    viewCounts = await getPublicBlogViews();
  } catch {
    /* 백엔드 미연결 시 무시 */
  }

  return (
    <main className="mx-auto max-w-4xl px-4 py-12 sm:px-6 lg:px-8">
      <header className="mb-8">
        <nav className="text-sm text-muted">
          <Link href="/blog" className="hover:text-foreground">
            시험 준비 팁
          </Link>
          <span className="mx-2">/</span>
          <span>#{decoded}</span>
        </nav>
        <h1 className="mt-4 text-3xl font-bold sm:text-4xl">
          #{decoded}
        </h1>
        <p className="mt-2 text-muted">
          {posts.length}개의 글
        </p>
      </header>

      <div className="mb-8 flex flex-wrap gap-2">
        <Link
          href="/blog"
          className="rounded-full border border-border px-3 py-1 text-xs font-medium text-muted transition-colors hover:border-primary/40 hover:text-foreground"
        >
          전체
        </Link>
        {allTags.map(({ tag: t, count }) => (
          <Link
            key={t}
            href={`/blog/tag/${encodeURIComponent(t)}`}
            className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
              t === decoded
                ? "border-primary/50 bg-primary/10 text-primary"
                : "border-border text-muted hover:border-primary/40 hover:text-foreground"
            }`}
          >
            #{t}
            <span className="ml-1 text-[10px] opacity-60">{count}</span>
          </Link>
        ))}
      </div>

      <div className="grid gap-6">
        {posts.map((post) => (
          <Link
            key={post.slug}
            href={`/blog/${post.slug}`}
            className="group rounded-xl border border-border bg-surface p-6 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5"
          >
            <div className="flex flex-wrap items-center gap-2 text-xs">
              <span
                className={`inline-flex items-center rounded-md border px-2 py-0.5 font-medium ${getCategoryStyle(post.category)}`}
              >
                {post.category}
              </span>
              <span className="text-muted">
                {new Date(post.date).toLocaleDateString("ko-KR", {
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                })}
              </span>
              <span className="text-muted/60">{post.readingTime}</span>
              {(viewCounts[post.slug] ?? 0) > 0 && (
                <span className="text-muted/60">
                  조회 {viewCounts[post.slug].toLocaleString()}
                </span>
              )}
            </div>

            <h2 className="mt-3 text-xl font-bold group-hover:text-primary sm:text-2xl">
              {post.title}
            </h2>

            <p className="mt-2 text-sm leading-relaxed text-muted">
              {post.description}
            </p>

            <div className="mt-4 flex flex-wrap gap-1.5">
              {post.tags.map((t) => (
                <span
                  key={t}
                  className={`rounded px-2 py-0.5 text-[11px] ${
                    t === decoded
                      ? "bg-primary/10 text-primary"
                      : "bg-surface text-muted"
                  }`}
                >
                  #{t}
                </span>
              ))}
            </div>
          </Link>
        ))}
      </div>
    </main>
  );
}
