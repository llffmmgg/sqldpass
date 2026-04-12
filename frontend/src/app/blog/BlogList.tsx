"use client";

import Image from "next/image";
import Link from "next/link";
import { useMemo, useState } from "react";
import type { BlogPostMeta } from "@/lib/blog";

const CATEGORY_ACCENT: Record<string, { badge: string; bar: string; hover: string }> = {
  SQLD: {
    badge: "bg-primary/10 text-primary border-primary/30",
    bar: "bg-primary",
    hover: "hover:border-primary/40",
  },
  정보처리기사: {
    badge: "bg-accent/10 text-accent border-accent/30",
    bar: "bg-accent",
    hover: "hover:border-accent/40",
  },
  컴퓨터활용능력: {
    badge: "bg-blue-600/10 text-blue-600 border-blue-600/30",
    bar: "bg-blue-600",
    hover: "hover:border-blue-600/40",
  },
};

const DEFAULT_ACCENT = {
  badge: "bg-muted/10 text-muted border-muted/30",
  bar: "bg-muted",
  hover: "hover:border-muted/40",
};

function getAccent(category: string) {
  return CATEGORY_ACCENT[category] ?? DEFAULT_ACCENT;
}

function formatDate(dateStr: string, long = false) {
  return new Date(dateStr).toLocaleDateString("ko-KR", {
    year: long ? "numeric" : undefined,
    month: "long",
    day: "numeric",
  });
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
      {/* ── 헤더 ── */}
      <header className="mb-10 flex items-center gap-5">
        <Image
          src="/blog-mascot.webp"
          alt="시험 준비 팁 마스코트"
          width={120}
          height={120}
          className="shrink-0"
          priority
        />
        <div>
          <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">시험 준비 팁</h1>
          <p className="mt-2 max-w-lg text-base text-muted">
            자격증 시험 준비에 도움이 되는 학습 전략과 팁을 공유합니다.
          </p>
        </div>
      </header>

      {/* ── 카테고리 탭 ── */}
      <div className="mb-8 flex flex-wrap gap-2">
        <TabButton active={activeTab === "전체"} onClick={() => setActiveTab("전체")}>
          전체 <span className="ml-1 opacity-50">{posts.length}</span>
        </TabButton>
        {categories.map(({ category, count }) => (
          <TabButton key={category} active={activeTab === category} onClick={() => setActiveTab(category)}>
            {category} <span className="ml-1 opacity-50">{count}</span>
          </TabButton>
        ))}
      </div>

      {filtered.length === 0 ? (
        <p className="py-20 text-center text-muted">아직 작성된 글이 없습니다.</p>
      ) : (
        <div className="space-y-5">
          {/* ── Featured ── */}
          {featured && <FeaturedCard post={featured} views={viewCounts[featured.slug] ?? 0} />}

          {/* ── 그리드 ── */}
          {rest.length > 0 && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {rest.map((post) => (
                <PostCard key={post.slug} post={post} views={viewCounts[post.slug] ?? 0} />
              ))}
            </div>
          )}
        </div>
      )}
    </main>
  );
}

/* ── 탭 버튼 ── */
function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`rounded-full border px-4 py-1.5 text-xs font-semibold transition-all ${
        active
          ? "border-primary bg-primary/10 text-primary"
          : "border-border text-muted hover:border-foreground/20 hover:text-foreground"
      }`}
    >
      {children}
    </button>
  );
}

/* ── Featured 카드 ── */
function FeaturedCard({ post, views }: { post: BlogPostMeta; views: number }) {
  const accent = getAccent(post.category);
  return (
    <Link
      href={`/blog/${post.slug}`}
      className={`group relative flex flex-col overflow-hidden rounded-2xl border border-border bg-surface transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-primary/5 ${accent.hover}`}
    >
      {/* 상단 accent bar */}
      <div className={`h-1 w-full ${accent.bar}`} />
      <div className="flex flex-1 flex-col p-7 sm:p-8">
        <div className="flex flex-wrap items-center gap-2.5 text-xs">
          <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 font-semibold ${accent.badge}`}>
            {post.category}
          </span>
          <span className="text-muted">{formatDate(post.date, true)}</span>
          <span className="flex items-center gap-1 text-muted/70">
            <ClockIcon />
            {post.readingTime}
          </span>
          {views > 0 && (
            <span className="flex items-center gap-1 text-muted/70">
              <EyeIcon />
              {views.toLocaleString()}
            </span>
          )}
        </div>
        <h2 className="mt-4 text-2xl font-bold leading-tight tracking-tight group-hover:text-primary sm:text-3xl">
          {post.title}
        </h2>
        <p className="mt-3 max-w-2xl text-base leading-relaxed text-muted">
          {post.description}
        </p>
        {post.tags && post.tags.length > 0 && (
          <div className="mt-4 flex flex-wrap gap-1.5">
            {post.tags.slice(0, 4).map((tag) => (
              <span key={tag} className="rounded-md bg-border/50 px-2 py-0.5 text-[11px] text-muted">
                {tag}
              </span>
            ))}
          </div>
        )}
        <span className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-primary">
          읽어보기
          <svg className="h-3.5 w-3.5 transition-transform group-hover:translate-x-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
          </svg>
        </span>
      </div>
    </Link>
  );
}

/* ── 일반 카드 ── */
function PostCard({ post, views }: { post: BlogPostMeta; views: number }) {
  const accent = getAccent(post.category);
  return (
    <Link
      href={`/blog/${post.slug}`}
      className={`group relative flex flex-col overflow-hidden rounded-xl border border-border bg-surface transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-primary/5 ${accent.hover}`}
    >
      {/* 상단 accent bar */}
      <div className={`h-0.5 w-full ${accent.bar}`} />
      <div className="flex flex-1 flex-col p-5">
        <div className="flex items-center gap-2 text-[11px]">
          <span className={`inline-flex items-center rounded-full border px-2 py-0.5 font-semibold ${accent.badge}`}>
            {post.category}
          </span>
          <span className="flex items-center gap-1 text-muted/70">
            <ClockIcon />
            {post.readingTime}
          </span>
        </div>
        <h2 className="mt-3 text-base font-bold leading-snug group-hover:text-primary">
          {post.title}
        </h2>
        <p className="mt-2 flex-1 text-sm leading-relaxed text-muted line-clamp-2">
          {post.description}
        </p>
        <div className="mt-3 flex items-center justify-between text-[11px] text-muted/60">
          <span>{formatDate(post.date)}</span>
          {views > 0 && (
            <span className="flex items-center gap-1">
              <EyeIcon />
              {views.toLocaleString()}
            </span>
          )}
        </div>
      </div>
    </Link>
  );
}

/* ── 아이콘 ── */
function ClockIcon() {
  return (
    <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
    </svg>
  );
}

function EyeIcon() {
  return (
    <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
    </svg>
  );
}
