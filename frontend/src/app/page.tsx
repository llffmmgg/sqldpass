import Link from "next/link";
import ScrollReveal from "@/components/ScrollReveal";
import HeroCta from "@/components/HeroCta";
import HeroStats from "@/components/HeroStats";
import EngineerExamCountdown from "@/components/EngineerExamCountdown";
import { SiteNoticeModal } from "@/components/SiteNoticeModal";
import CertChips from "@/components/CertChips";
import PreviewTabs from "@/components/PreviewTabs";
import RankingSection from "@/components/RankingSection";
import { getAllPosts } from "@/lib/blog";

const CATEGORY_COLORS: Record<string, string> = {
  SQLD: "bg-primary/10 text-primary border-primary/30",
  정보처리기사: "bg-accent/10 text-accent border-accent/30",
  컴퓨터활용능력: "bg-blue-600/10 text-blue-600 border-blue-600/30",
};

export default function Home() {
  // 과목별 최신 글 1개씩 선택
  const allPosts = getAllPosts();
  const recentPosts = ["SQLD", "정보처리기사", "컴퓨터활용능력"]
    .map((cat) => allPosts.find((p) => p.category === cat))
    .filter((p): p is NonNullable<typeof p> => p != null);
  return (
    <main className="min-h-screen bg-background text-foreground">
      <SiteNoticeModal />

      {/* ── Hero ───────────────────────────────────────────── */}
      <section className="relative overflow-hidden">
        {/* 단일 ambient gradient — 노이즈 최소화로 가독성 우선 */}
        <div className="hero-ambient" aria-hidden="true" />
        <div className="absolute inset-0 grid-pattern opacity-50" />
        <div className="hero-orb hero-orb-1" aria-hidden="true" />
        <div className="hero-orb hero-orb-3" aria-hidden="true" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_50%_at_50%_50%,transparent_0%,var(--background)_100%)]" />

        <div className="relative mx-auto max-w-4xl px-4 py-28 text-center sm:px-6 sm:py-36 lg:px-8">
          <ScrollReveal>
            <span className="inline-flex items-center gap-1.5 rounded-full border border-accent/30 bg-accent/10 px-4 py-1.5 text-sm font-medium text-accent">
              <span className="relative flex h-1.5 w-1.5">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent opacity-60" />
                <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-accent" />
              </span>
              매번 새로 추가되는 기출 · 실전 타이머 제공
            </span>
          </ScrollReveal>

          <ScrollReveal delay={1}>
            <h1 className="mt-7 text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl lg:text-7xl">
              매일 한 세트,
              <br />
              <span className="text-primary">
                합격까지
              </span>
            </h1>
          </ScrollReveal>

          <ScrollReveal delay={2}>
            <p className="mx-auto mt-7 max-w-xl text-lg leading-relaxed text-muted sm:text-xl">
              실전 모의고사를 풀고, 무한 문제 디펜스로 합격을 만들어 가세요.
            </p>
          </ScrollReveal>

          <ScrollReveal delay={3}>
            <div className="mt-8">
              <CertChips />
            </div>
          </ScrollReveal>

          <ScrollReveal delay={4}>
            <div className="mt-10">
              <HeroCta />
            </div>
          </ScrollReveal>

          <ScrollReveal delay={4}>
            <div className="mt-6">
              <EngineerExamCountdown />
            </div>
          </ScrollReveal>

          <ScrollReveal delay={5}>
            <HeroStats />
          </ScrollReveal>
        </div>
      </section>

      {/* ── Features ───────────────────────────────────────── */}
      <section className="mx-auto max-w-5xl px-4 py-24 sm:px-6 lg:px-8">
        <ScrollReveal>
          <h2 className="text-center text-2xl font-bold sm:text-3xl">합격에 필요한 모든 것</h2>
          <p className="mt-3 text-center text-muted">한 곳에서, 매일 조금씩</p>
        </ScrollReveal>

        <div className="mt-12 grid grid-cols-1 gap-6 lg:grid-cols-3">
          <ScrollReveal delay={1}>
            <div className="group relative h-full rounded-xl border border-border bg-surface p-6 transition-all duration-300 hover:-translate-y-1.5 hover:border-primary/30 hover:shadow-[0_0_24px_var(--glow)]">
              <span className="pointer-events-none absolute right-4 top-4 select-none text-[64px] font-bold leading-none text-foreground/[0.03]">
                01
              </span>
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                <svg className="h-5 w-5 text-primary" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15a2.25 2.25 0 0 1 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25Z" />
                </svg>
              </div>
              <h3 className="mt-4 text-xl font-semibold">즉석 랜덤 모의고사</h3>
              <p className="mt-2 text-base leading-relaxed text-muted">
                누를 때마다 새로 구성된 실전 세트. 자격증별 회차 분포 그대로,
                실제 시험과 동일한 환경에서.
              </p>
            </div>
          </ScrollReveal>

          <ScrollReveal delay={2}>
            <div className="group relative h-full rounded-xl border border-border bg-surface p-6 transition-all duration-300 hover:-translate-y-1.5 hover:border-accent/30 hover:shadow-[0_0_24px_var(--accent-glow)] lg:-translate-y-2">
              <span className="pointer-events-none absolute right-4 top-4 select-none text-[64px] font-bold leading-none text-foreground/[0.03]">
                02
              </span>
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent/10">
                <svg className="h-5 w-5 text-accent" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" />
                </svg>
              </div>
              <h3 className="mt-4 text-xl font-semibold">오답 자동 복습</h3>
              <p className="mt-2 text-base leading-relaxed text-muted">
                자격증별로 틀린 문제만 모아 취약 영역을 분석합니다.
                해설과 함께 약점만 골라 다시 풀어보세요.
              </p>
            </div>
          </ScrollReveal>

          <ScrollReveal delay={3}>
            <div className="group relative h-full rounded-xl border border-border bg-surface p-6 transition-all duration-300 hover:-translate-y-1.5 hover:border-primary/30 hover:shadow-[0_0_24px_var(--glow)]">
              <span className="pointer-events-none absolute right-4 top-4 select-none text-[64px] font-bold leading-none text-foreground/[0.03]">
                03
              </span>
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-primary/10 to-accent/10">
                <svg className="h-5 w-5 text-primary" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                </svg>
              </div>
              <h3 className="mt-4 text-xl font-semibold">회차별 실력 추적</h3>
              <p className="mt-2 text-base leading-relaxed text-muted">
                점수 추이와 풀이 시간을 자동 기록.
                합격 준비도를 한눈에 확인합니다.
              </p>
            </div>
          </ScrollReveal>
        </div>
      </section>

      {/* ── Ranking ────────────────────────────────────────── */}
      <ScrollReveal>
        <RankingSection />
      </ScrollReveal>

      {/* ── Preview ────────────────────────────────────────── */}
      <section id="preview" className="relative border-t border-border bg-surface/50 py-24">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,var(--accent-glow),transparent_60%)] pointer-events-none" />

        <div className="relative mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
          <ScrollReveal>
            <h2 className="text-center text-2xl font-bold sm:text-3xl">자격증별 문제 미리보기</h2>
            <p className="mt-3 text-center text-muted">
              지원 자격증의 실제 출제 유형을 가볍게 풀어보세요
            </p>
          </ScrollReveal>

          <ScrollReveal delay={1}>
            <PreviewTabs />
          </ScrollReveal>
        </div>
      </section>

      {/* ── Blog ─────────────────────────────────────────── */}
      {recentPosts.length > 0 && (
        <section className="mx-auto max-w-5xl px-4 py-24 sm:px-6 lg:px-8">
          <ScrollReveal>
            <h2 className="text-center text-2xl font-bold sm:text-3xl">시험 준비 팁</h2>
            <p className="mt-3 text-center text-muted">
              과목별 핵심 개념 정리와 합격 전략을 확인하세요
            </p>
          </ScrollReveal>

          <div className="mt-12 grid grid-cols-1 gap-6 md:grid-cols-3">
            {recentPosts.map((post, i) => (
              <ScrollReveal key={post.slug} delay={i + 1}>
                <Link
                  href={`/blog/${post.slug}`}
                  className="group flex h-full flex-col rounded-xl border border-border bg-surface p-6 transition-all duration-300 hover:-translate-y-1.5 hover:border-primary/30 hover:shadow-lg hover:shadow-primary/5"
                >
                  <div className="flex items-center gap-2 text-xs">
                    <span
                      className={`inline-flex items-center rounded-md border px-2 py-0.5 font-medium ${
                        CATEGORY_COLORS[post.category] ?? "bg-muted/10 text-muted border-muted/30"
                      }`}
                    >
                      {post.category}
                    </span>
                    <span className="text-muted">
                      {new Date(post.date).toLocaleDateString("ko-KR", {
                        month: "long",
                        day: "numeric",
                      })}
                    </span>
                  </div>
                  <h3 className="mt-3 text-lg font-semibold leading-snug group-hover:text-primary">
                    {post.title}
                  </h3>
                  <p className="mt-2 flex-1 text-sm leading-relaxed text-muted line-clamp-2">
                    {post.description}
                  </p>
                  <span className="mt-4 text-xs font-medium text-primary">
                    읽어보기 →
                  </span>
                </Link>
              </ScrollReveal>
            ))}
          </div>

          <ScrollReveal delay={4}>
            <div className="mt-10 text-center">
              <Link
                href="/blog"
                className="inline-flex items-center rounded-lg border border-border px-5 py-2.5 text-sm font-semibold text-foreground transition-all hover:border-primary/40 hover:bg-primary/5"
              >
                전체 글 보기
              </Link>
            </div>
          </ScrollReveal>
        </section>
      )}

      {/* ── CTA ────────────────────────────────────────────── */}
      <section className="relative mx-auto max-w-5xl px-4 py-24 text-center sm:px-6 lg:px-8">
        <div className="shimmer-line mx-auto mb-16 max-w-md" />

        <ScrollReveal>
          <h2 className="text-2xl font-bold sm:text-3xl">오늘의 한 세트, 지금 시작</h2>
          <p className="mt-3 text-muted">회원가입 없이 바로 풀어볼 수 있습니다.</p>
          <a
            href="/solve"
            className="btn-glow mt-8 inline-flex items-center rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-zinc-900 transition-all duration-300 hover:bg-primary-hover hover:scale-[1.03]"
          >
            문제 풀러 가기
          </a>
        </ScrollReveal>
      </section>
    </main>
  );
}
