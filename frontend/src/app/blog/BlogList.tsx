"use client";

import Image from "next/image";
import Link from "next/link";
import type { BlogPostMeta } from "@/lib/blog";

const CATEGORIES = [
  {
    name: "SQLD",
    slug: "SQLD",
    label: "SQLD",
    description: "SQL 개발자 자격증 공부법, 핵심 개념, 합격률, 시험일정",
    gradient: "from-amber-500/20 to-orange-500/10",
    border: "border-amber-500/30 hover:border-amber-400/60",
    iconBg: "bg-amber-500/15",
    emoji: "🗃️",
  },
  {
    name: "정보처리기사",
    slug: "정보처리기사",
    label: "정보처리기사 실기",
    description: "정처기 실기 출제 경향, 코드 문제 풀이, 암기 과목 정리",
    gradient: "from-violet-500/20 to-purple-500/10",
    border: "border-violet-500/30 hover:border-violet-400/60",
    iconBg: "bg-violet-500/15",
    emoji: "💻",
  },
  {
    name: "정보처리기사 필기",
    slug: "정보처리기사 필기",
    label: "정보처리기사 필기",
    description: "정처기 필기 5과목 공부법, 핵심 개념 요약, 합격률 분석",
    gradient: "from-purple-500/20 to-fuchsia-500/10",
    border: "border-purple-500/30 hover:border-purple-400/60",
    iconBg: "bg-purple-500/15",
    emoji: "📝",
  },
  {
    name: "컴퓨터활용능력",
    slug: "컴퓨터활용능력",
    label: "컴퓨터활용능력",
    description: "컴활 1급 필기 벼락치기, 실기 대비, 합격률 분석",
    gradient: "from-sky-500/20 to-blue-500/10",
    border: "border-sky-500/30 hover:border-sky-400/60",
    iconBg: "bg-sky-500/15",
    emoji: "📊",
  },
  {
    name: "컴퓨터활용능력 2급",
    slug: "컴퓨터활용능력 2급",
    label: "컴퓨터활용능력 2급",
    description: "컴활 2급 필기 공부법, 핵심 개념, 합격률, 시험일정",
    gradient: "from-indigo-500/20 to-blue-500/10",
    border: "border-indigo-500/30 hover:border-indigo-400/60",
    iconBg: "bg-indigo-500/15",
    emoji: "📋",
  },
  {
    name: "ADsP",
    slug: "ADsP",
    label: "데이터분석 준전문가(ADsP)",
    description: "ADsP 공부법, 핵심 개념 요약, 합격률, 2024 개편 대응",
    gradient: "from-teal-500/20 to-cyan-500/10",
    border: "border-teal-500/30 hover:border-teal-400/60",
    iconBg: "bg-teal-500/15",
    emoji: "📈",
  },
  {
    name: "일반",
    slug: "일반",
    label: "시험 팁",
    description: "자격증 비교, 시험 당일 꿀팁, CBT 모의고사 활용법",
    gradient: "from-emerald-500/20 to-teal-500/10",
    border: "border-emerald-500/30 hover:border-emerald-400/60",
    iconBg: "bg-emerald-500/15",
    emoji: "🎯",
  },
];

export default function BlogList({
  posts,
  categories,
  viewCounts,
}: {
  posts: BlogPostMeta[];
  categories: { category: string; count: number }[];
  viewCounts: Record<string, number>;
}) {
  const countMap = Object.fromEntries(categories.map((c) => [c.category, c.count]));
  const totalViews = Object.values(viewCounts).reduce((s, v) => s + v, 0);

  // 최신 글 3개
  const latestPosts = posts.slice(0, 3);

  return (
    <main className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8">
      {/* ── 헤더 ── */}
      <header className="mb-12 flex items-center gap-5">
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
            자격증별 학습 전략과 합격 노하우를 정리했어요.
          </p>
          <div className="mt-2 flex items-center gap-3 text-xs text-muted/70">
            <span>{posts.length}개의 글</span>
            {totalViews > 0 && (
              <>
                <span className="h-3 w-px bg-border" />
                <span>총 조회 {totalViews.toLocaleString()}</span>
              </>
            )}
          </div>
        </div>
      </header>

      {/* ── 카테고리 카드 그리드 ── */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {CATEGORIES.map((cat) => {
          const count = countMap[cat.name] ?? 0;
          if (count === 0) return null;
          return (
            <Link
              key={cat.name}
              href={`/blog/category/${encodeURIComponent(cat.slug)}`}
              className={`group relative overflow-hidden rounded-2xl border bg-gradient-to-br ${cat.gradient} ${cat.border} p-6 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl`}
            >
              <div className="pointer-events-none absolute -right-6 -top-6 h-28 w-28 rounded-full bg-white/5 blur-2xl" />
              <div className="relative">
                <div className="flex items-center justify-between">
                  <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${cat.iconBg}`}>
                    <span className="text-2xl">{cat.emoji}</span>
                  </div>
                  <span className="rounded-full bg-foreground/5 px-2.5 py-1 text-xs font-medium text-muted">
                    {count}개의 글
                  </span>
                </div>
                <h2 className="mt-4 text-xl font-bold tracking-tight group-hover:text-foreground">
                  {cat.label}
                </h2>
                <p className="mt-2 text-sm leading-relaxed text-muted">
                  {cat.description}
                </p>
                <div className="mt-4 flex items-center gap-1 text-sm font-medium text-muted transition-colors group-hover:text-foreground">
                  글 보러가기
                  <svg className="h-4 w-4 transition-transform group-hover:translate-x-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>
                </div>
              </div>
            </Link>
          );
        })}
      </div>

      {/* ── 최신 글 ── */}
      {latestPosts.length > 0 && (
        <section className="mt-14">
          <h2 className="text-xl font-bold">최신 글</h2>
          <div className="mt-5 space-y-3">
            {latestPosts.map((post) => {
              const views = viewCounts[post.slug] ?? 0;
              return (
                <Link
                  key={post.slug}
                  href={`/blog/${post.slug}`}
                  className="group flex items-center gap-4 rounded-xl border border-border bg-surface p-4 transition-all hover:border-primary/30 hover:shadow-md"
                >
                  <div className="flex-1">
                    <div className="flex items-center gap-2 text-xs">
                      <span className={`inline-flex items-center rounded-full border px-2 py-0.5 font-semibold ${getCategoryBadge(post.category)}`}>
                        {post.category}
                      </span>
                      <span className="text-muted">
                        {new Date(post.date).toLocaleDateString("ko-KR", { month: "long", day: "numeric" })}
                      </span>
                      <span className="text-muted/60">{post.readingTime}</span>
                      {views > 0 && (
                        <span className="text-muted/60">조회 {views.toLocaleString()}</span>
                      )}
                    </div>
                    <h3 className="mt-1.5 text-base font-bold leading-snug group-hover:text-primary">
                      {post.title}
                    </h3>
                  </div>
                  <svg className="h-5 w-5 shrink-0 text-muted/40 transition-transform group-hover:translate-x-0.5 group-hover:text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
                  </svg>
                </Link>
              );
            })}
          </div>
        </section>
      )}
    </main>
  );
}

function getCategoryBadge(category: string) {
  const map: Record<string, string> = {
    SQLD: "bg-primary/10 text-primary border-primary/30",
    정보처리기사: "bg-accent/10 text-accent border-accent/30",
    "정보처리기사 필기": "bg-purple-500/10 text-purple-500 border-purple-500/30",
    컴퓨터활용능력: "bg-blue-600/10 text-blue-600 border-blue-600/30",
    "컴퓨터활용능력 2급": "bg-indigo-500/10 text-indigo-500 border-indigo-500/30",
    ADsP: "bg-teal-500/10 text-teal-500 border-teal-500/30",
  };
  return map[category] ?? "bg-muted/10 text-muted border-muted/30";
}
