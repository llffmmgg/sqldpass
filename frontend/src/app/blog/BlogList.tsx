"use client";

import Image from "next/image";
import Link from "next/link";
import type { BlogPostMeta } from "@/lib/blog";
import { Badge, Card, Container } from "@/components/ui";
import { certFromBlogCategory } from "@/lib/cert-tokens";

type CategoryMeta = {
  name: string;
  slug: string;
  label: string;
  description: string;
  iconBg: string;
  emoji: string;
};

const CATEGORIES: CategoryMeta[] = [
  {
    name: "SQLD",
    slug: "SQLD",
    label: "SQLD",
    description: "SQL 개발자 자격증 공부법, 핵심 개념, 합격률, 시험일정",
    iconBg: "bg-amber-500/10",
    emoji: "🗃️",
  },
  {
    name: "정보처리기사",
    slug: "정보처리기사",
    label: "정보처리기사 실기",
    description: "정처기 실기 출제 경향, 코드 문제 풀이, 암기 과목 정리",
    iconBg: "bg-emerald-500/10",
    emoji: "💻",
  },
  {
    name: "정보처리기사 필기",
    slug: "정보처리기사 필기",
    label: "정보처리기사 필기",
    description: "정처기 필기 5과목 공부법, 핵심 개념 요약, 합격률 분석",
    iconBg: "bg-rose-500/10",
    emoji: "📝",
  },
  {
    name: "컴퓨터활용능력",
    slug: "컴퓨터활용능력",
    label: "컴퓨터활용능력",
    description: "컴활 1급 필기 벼락치기, 실기 대비, 합격률 분석",
    iconBg: "bg-sky-500/10",
    emoji: "📊",
  },
  {
    name: "컴퓨터활용능력 2급",
    slug: "컴퓨터활용능력 2급",
    label: "컴퓨터활용능력 2급",
    description: "컴활 2급 필기 공부법, 핵심 개념, 합격률, 시험일정",
    iconBg: "bg-indigo-500/10",
    emoji: "📋",
  },
  {
    name: "ADsP",
    slug: "ADsP",
    label: "데이터분석 준전문가(ADsP)",
    description: "ADsP 공부법, 핵심 개념 요약, 합격률, 2024 개편 대응",
    iconBg: "bg-teal-500/10",
    emoji: "📈",
  },
  {
    name: "일반",
    slug: "일반",
    label: "시험 팁",
    description: "자격증 비교, 시험 당일 꿀팁, CBT 모의고사 활용법",
    iconBg: "bg-primary/10",
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
  const latestPosts = posts.slice(0, 3);

  return (
    <Container size="default" className="py-16">
      <header className="mb-14 flex items-center gap-5">
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
          <p className="mt-2 max-w-lg text-base text-text-muted">
            자격증별 학습 전략과 합격 노하우를 정리했어요.
          </p>
          <div className="mt-2 flex items-center gap-3 text-xs text-text-subtle">
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

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {CATEGORIES.map((cat) => {
          const count = countMap[cat.name] ?? 0;
          if (count === 0) return null;
          return (
            <Link
              key={cat.name}
              href={`/blog/category/${encodeURIComponent(cat.slug)}`}
              className="group block h-full"
            >
              <Card variant="interactive" padding="md" className="h-full">
                <div className="flex items-center justify-between">
                  <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${cat.iconBg}`}>
                    <span className="text-2xl">{cat.emoji}</span>
                  </div>
                  <span className="rounded-full border border-border bg-surface px-2.5 py-0.5 text-xs font-medium text-text-muted">
                    {count}개의 글
                  </span>
                </div>
                <h2 className="mt-5 text-lg font-semibold tracking-tight group-hover:text-primary">
                  {cat.label}
                </h2>
                <p className="mt-2 text-sm leading-relaxed text-text-muted">
                  {cat.description}
                </p>
                <div className="mt-5 inline-flex items-center gap-1 text-sm font-medium text-text-muted transition-colors group-hover:text-primary">
                  글 보러가기
                  <svg
                    className="h-4 w-4 transition-transform group-hover:translate-x-0.5"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={2}
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>
                </div>
              </Card>
            </Link>
          );
        })}
      </div>

      {latestPosts.length > 0 && (
        <section className="mt-16">
          <h2 className="text-xl font-semibold tracking-tight">최신 글</h2>
          <div className="mt-5 space-y-3">
            {latestPosts.map((post) => {
              const views = viewCounts[post.slug] ?? 0;
              const cert = certFromBlogCategory(post.category);
              return (
                <Link key={post.slug} href={`/blog/${post.slug}`} className="group block">
                  <Card variant="interactive" padding="sm" className="flex items-center gap-4">
                    <div className="flex-1">
                      <div className="flex flex-wrap items-center gap-2 text-xs">
                        {cert ? (
                          <Badge cert={cert} variant="soft" size="xs">
                            {post.category}
                          </Badge>
                        ) : (
                          <Badge variant="soft" tone="neutral" size="xs">
                            {post.category}
                          </Badge>
                        )}
                        <span className="text-text-muted">
                          {new Date(post.date).toLocaleDateString("ko-KR", {
                            month: "long",
                            day: "numeric",
                          })}
                        </span>
                        <span className="text-text-subtle">{post.readingTime}</span>
                        <span className="text-text-subtle">조회 {views.toLocaleString()}</span>
                      </div>
                      <h3 className="mt-2 text-base font-semibold leading-snug group-hover:text-primary">
                        {post.title}
                      </h3>
                    </div>
                    <svg
                      className="h-5 w-5 shrink-0 text-text-subtle transition-transform group-hover:translate-x-0.5 group-hover:text-primary"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2}
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
                    </svg>
                  </Card>
                </Link>
              );
            })}
          </div>
        </section>
      )}
    </Container>
  );
}
