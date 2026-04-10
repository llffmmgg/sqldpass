import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { getAllPosts, getAllCategories } from "@/lib/blog";
import { getPublicBlogViews } from "@/lib/publicApi";

export const metadata: Metadata = {
  title: "자격증 시험 준비 블로그",
  description:
    "SQLD, 정보처리기사 실기, 컴퓨터활용능력 1급 시험 준비에 필요한 학습 전략, 출제 경향, CBT 활용법을 정리한 블로그입니다.",
  alternates: { canonical: "https://www.sqldpass.com/blog" },
  openGraph: {
    title: "자격증 시험 준비 블로그 | SQLD Pass",
    description:
      "SQLD, 정처기, 컴퓨터활용능력 시험 준비 꿀팁과 학습 전략을 공유합니다.",
    url: "https://www.sqldpass.com/blog",
  },
};

const CATEGORY_COLORS: Record<string, string> = {
  SQLD: "bg-primary/10 text-primary border-primary/30",
  정보처리기사: "bg-accent/10 text-accent border-accent/30",
  컴퓨터활용능력: "bg-blue-600/10 text-blue-600 border-blue-600/30",
};

function getCategoryStyle(category: string) {
  return (
    CATEGORY_COLORS[category] ??
    "bg-muted/10 text-muted border-muted/30"
  );
}

export default async function BlogPage() {
  const posts = getAllPosts();
  const allCategories = getAllCategories();
  let viewCounts: Record<string, number> = {};
  try {
    viewCounts = await getPublicBlogViews();
  } catch {
    /* 백엔드 미연결 시 무시 */
  }

  return (
    <main className="mx-auto max-w-4xl px-4 py-12 sm:px-6 lg:px-8">
      <header className="mb-10 flex items-center gap-5">
        <Image
          src="/blog-mascot.webp"
          alt="시험 준비 팁 마스코트"
          width={160}
          height={160}
          className="shrink-0"
          priority
        />
        <div>
          <h1 className="text-3xl font-bold sm:text-4xl">시험 준비 팁</h1>
          <p className="mt-2 text-muted">
            자격증 시험 준비에 도움이 되는 학습 전략과 팁을 공유합니다.
          </p>
        </div>
      </header>

      <div className="mb-8 flex flex-wrap gap-2">
        <span className="rounded-full border border-primary/50 bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
          전체
          <span className="ml-1 text-[10px] opacity-60">{posts.length}</span>
        </span>
        {allCategories.map(({ category, count }) => (
          <Link
            key={category}
            href={`/blog/category/${encodeURIComponent(category)}`}
            className="rounded-full border border-border px-3 py-1 text-xs font-medium text-muted transition-colors hover:border-primary/40 hover:text-foreground"
          >
            {category}
            <span className="ml-1 text-[10px] opacity-60">{count}</span>
          </Link>
        ))}
      </div>

      {posts.length === 0 ? (
        <p className="text-muted">아직 작성된 글이 없습니다.</p>
      ) : (
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
            </Link>
          ))}
        </div>
      )}
    </main>
  );
}
