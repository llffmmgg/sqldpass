import type { Metadata } from "next";
import Link from "next/link";
import { getAllPosts } from "@/lib/blog";

export const metadata: Metadata = {
  title: "자격증 시험 준비 블로그",
  description:
    "SQLD, 정보처리기사 실기, 컴퓨터활용능력 1급 시험 준비에 필요한 학습 전략, 출제 경향, CBT 활용법을 정리한 블로그입니다.",
  alternates: { canonical: "https://www.sqldpass.com/blog" },
  openGraph: {
    title: "자격증 시험 준비 블로그 | SQLD Pass",
    description:
      "SQLD, 정처기, 컴활 시험 준비 꿀팁과 학습 전략을 공유합니다.",
    url: "https://www.sqldpass.com/blog",
  },
};

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

export default function BlogPage() {
  const posts = getAllPosts();

  return (
    <main className="mx-auto max-w-4xl px-4 py-12 sm:px-6 lg:px-8">
      <header className="mb-10">
        <h1 className="text-3xl font-bold sm:text-4xl">블로그</h1>
        <p className="mt-3 text-muted">
          자격증 시험 준비에 도움이 되는 학습 전략과 팁을 공유합니다.
        </p>
      </header>

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
              </div>

              <h2 className="mt-3 text-xl font-bold group-hover:text-primary sm:text-2xl">
                {post.title}
              </h2>

              <p className="mt-2 text-sm leading-relaxed text-muted">
                {post.description}
              </p>

              <div className="mt-4 flex flex-wrap gap-1.5">
                {post.tags.map((tag) => (
                  <span
                    key={tag}
                    className="rounded bg-surface px-2 py-0.5 text-[11px] text-muted"
                  >
                    #{tag}
                  </span>
                ))}
              </div>
            </Link>
          ))}
        </div>
      )}
    </main>
  );
}
