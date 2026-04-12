"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import type { BlogPostMeta } from "@/lib/blog";

const CATEGORY_COLORS: Record<string, string> = {
  SQLD: "bg-primary/10 text-primary border-primary/30",
  정보처리기사: "bg-accent/10 text-accent border-accent/30",
  컴퓨터활용능력: "bg-blue-600/10 text-blue-600 border-blue-600/30",
};

function getCategoryStyle(category: string) {
  return CATEGORY_COLORS[category] ?? "bg-muted/10 text-muted border-muted/30";
}

export default function BlogList({
  posts,
  categories,
  viewCounts,
}: {
  posts: BlogPostMeta[];
  categories: { category: string; count: number }[];
  viewCounts: Record<string, number>;
}) {
  const [activeTab, setActiveTab] = useState("전체");

  const filtered = useMemo(() => {
    if (activeTab === "전체") return posts;
    return posts.filter((p) => p.category === activeTab);
  }, [posts, activeTab]);

  const [featured, ...rest] = filtered;

  return (
    <main className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8">
      {/* 헤더 */}
      <header className="mb-10">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">시험 준비 팁</h1>
        <p className="mt-2 max-w-lg text-base text-muted">
          자격증 시험 준비에 도움이 되는 학습 전략과 팁을 공유합니다.
        </p>
      </header>

      {/* 카테고리 탭 — 클라이언트 필터 */}
      <div className="mb-8 flex flex-wrap gap-2">
        <button
          onClick={() => setActiveTab("전체")}
          className={`rounded-full border px-3.5 py-1.5 text-xs font-medium transition-colors ${
            activeTab === "전체"
              ? "border-primary/50 bg-primary/10 text-primary"
              : "border-border text-muted hover:border-primary/40 hover:text-foreground"
          }`}
        >
          전체
          <span className="ml-1 text-[10px] opacity-60">{posts.length}</span>
        </button>
        {categories.map(({ category, count }) => (
          <button
            key={category}
            onClick={() => setActiveTab(category)}
            className={`rounded-full border px-3.5 py-1.5 text-xs font-medium transition-colors ${
              activeTab === category
                ? "border-primary/50 bg-primary/10 text-primary"
                : "border-border text-muted hover:border-primary/40 hover:text-foreground"
            }`}
          >
            {category}
            <span className="ml-1 text-[10px] opacity-60">{count}</span>
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <p className="py-20 text-center text-muted">아직 작성된 글이 없습니다.</p>
      ) : (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          {/* Featured — 첫 번째 글 크게 */}
          {featured && (
            <Link
              key={featured.slug}
              href={`/blog/${featured.slug}`}
              className="group col-span-1 rounded-2xl border border-border bg-surface p-7 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 md:col-span-2"
            >
              <div className="flex flex-wrap items-center gap-2 text-xs">
                <span
                  className={`inline-flex items-center rounded-md border px-2 py-0.5 font-medium ${getCategoryStyle(featured.category)}`}
                >
                  {featured.category}
                </span>
                <span className="text-muted">
                  {new Date(featured.date).toLocaleDateString("ko-KR", {
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                  })}
                </span>
                <span className="text-muted/60">{featured.readingTime}</span>
                {(viewCounts[featured.slug] ?? 0) > 0 && (
                  <span className="text-muted/60">
                    조회 {viewCounts[featured.slug].toLocaleString()}
                  </span>
                )}
              </div>
              <h2 className="mt-4 text-2xl font-bold leading-tight group-hover:text-primary sm:text-3xl">
                {featured.title}
              </h2>
              <p className="mt-3 max-w-2xl text-base leading-relaxed text-muted">
                {featured.description}
              </p>
              <span className="mt-4 inline-block text-sm font-medium text-primary">
                읽어보기 →
              </span>
            </Link>
          )}

          {/* 나머지 글 — 2열 그리드 */}
          {rest.map((post) => (
            <Link
              key={post.slug}
              href={`/blog/${post.slug}`}
              className="group flex flex-col rounded-xl border border-border bg-surface p-6 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5"
            >
              <div className="flex flex-wrap items-center gap-2 text-xs">
                <span
                  className={`inline-flex items-center rounded-md border px-2 py-0.5 font-medium ${getCategoryStyle(post.category)}`}
                >
                  {post.category}
                </span>
                <span className="text-muted">
                  {new Date(post.date).toLocaleDateString("ko-KR", {
                    month: "long",
                    day: "numeric",
                  })}
                </span>
                <span className="text-muted/60">{post.readingTime}</span>
              </div>
              <h2 className="mt-3 text-lg font-bold leading-snug group-hover:text-primary">
                {post.title}
              </h2>
              <p className="mt-2 flex-1 text-sm leading-relaxed text-muted line-clamp-2">
                {post.description}
              </p>
            </Link>
          ))}
        </div>
      )}
    </main>
  );
}
