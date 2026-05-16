import type { Metadata } from "next";
import Link from "next/link";
import ScrollReveal from "@/components/ScrollReveal";
import HeroCta from "@/components/HeroCta";
import HeroStats from "@/components/HeroStats";
import ExamCountdownStrip from "@/components/ExamCountdownStrip";
import { SiteNoticeModal } from "@/components/SiteNoticeModal";
import RankingSection from "@/components/RankingSection";
import { Badge, ButtonLink, Card, Container, Section } from "@/components/ui";
import { getAllPosts } from "@/lib/blog";
import { certFromBlogCategory } from "@/lib/cert-tokens";

const HOME_TITLE = "SQLD·정처기·컴활·ADsP 무료 CBT 모의고사";
const HOME_DESCRIPTION =
  "SQLD·정처기 필기/실기·컴활 1·2급·ADsP 무료 CBT 모의고사. 기출 변형 문제, 자동 채점, 회차별 실력 추적까지 가입 없이 바로 풀어보세요.";

const HOME_OG_IMAGE = {
  url: "/opengraph-image",
  width: 1200,
  height: 630,
  alt: "문어CBT 무료 CBT 모의고사",
};

export const metadata: Metadata = {
  title: { absolute: `${HOME_TITLE} | 문어CBT` },
  description: HOME_DESCRIPTION,
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    url: "/",
    title: HOME_TITLE,
    description: HOME_DESCRIPTION,
    siteName: "문어CBT",
    locale: "ko_KR",
    images: [HOME_OG_IMAGE],
  },
  twitter: {
    card: "summary_large_image",
    title: HOME_TITLE,
    description: HOME_DESCRIPTION,
    images: [HOME_OG_IMAGE.url],
  },
};

export default function Home() {
  const allPosts = getAllPosts();
  const recentPosts = ["SQLD", "정보처리기사", "컴퓨터활용능력"]
    .map((cat) => allPosts.find((p) => p.category === cat))
    .filter((p): p is NonNullable<typeof p> => p != null);

  return (
    <main className="min-h-screen bg-bg text-text">
      <SiteNoticeModal />

      {/* ── Hero ───────────────────────────────────────────── */}
      <section className="relative overflow-hidden">
        <div className="hero-ambient" aria-hidden="true" />
        <div className="absolute inset-0 grid-pattern opacity-60" />
        <div className="hero-orb hero-orb-1" aria-hidden="true" />
        <div className="hero-orb hero-orb-3" aria-hidden="true" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_50%_at_50%_50%,transparent_0%,var(--bg)_100%)]" />

        <Container size="default" className="relative py-24 text-center sm:py-32 md:py-40">
          <ScrollReveal>
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-bg px-5 py-1.5 text-xs font-medium text-text">
              <span className="inline-flex h-1.5 w-1.5 rounded-full bg-primary" />
              CBT 모의고사 · 기출 복원 · 문제 풀기
            </span>
          </ScrollReveal>

          <ScrollReveal delay={1}>
            <h1 className="mt-6 text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl lg:text-7xl">
              온라인 CBT로 IT 자격증
              <br />
              <span className="text-primary">한 번에 합격하세요</span>
            </h1>
          </ScrollReveal>

          <ScrollReveal delay={2}>
            <p className="mx-auto mt-6 max-w-2xl text-base leading-relaxed text-text-muted sm:text-lg">
              문어CBT는 SQLD, 정보처리기사, 컴활, ADsP를 한 곳에서 연습할 수 있는 온라인 CBT
              학습 플랫폼입니다. 가입 없이 바로 문제를 풀어보고, 모의고사·기출 복원·오답
              노트·즐겨찾기·점수 기록 기능으로 시험 전까지 부족한 부분을 반복해서
              점검해보세요.
            </p>
          </ScrollReveal>

          <ScrollReveal delay={3}>
            <div className="mt-10">
              <HeroCta />
            </div>
          </ScrollReveal>

          <ScrollReveal delay={4}>
            <div className="mt-10">
              <ExamCountdownStrip />
            </div>
          </ScrollReveal>

          <ScrollReveal delay={5}>
            <HeroStats />
          </ScrollReveal>
        </Container>
      </section>

      {/* ── Features ───────────────────────────────────────── */}
      <Section>
        <Container size="default">
          <ScrollReveal>
            <h2 className="text-center text-2xl font-bold tracking-tight sm:text-3xl md:text-4xl">
              기출 풀이부터 오답 복습까지, 시험 전 루틴을 한 번에
            </h2>
            <p className="mx-auto mt-3 max-w-2xl text-center text-sm leading-relaxed text-text-muted sm:text-base">
              SQLD, 정보처리기사, 컴퓨터활용능력, ADsP를 실제 CBT처럼 풀고 부족한 부분만
              반복해서 점검하세요.
            </p>
          </ScrollReveal>

          <div className="mt-14 grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-4">
            <ScrollReveal delay={1}>
              <Link href="/mock-exams" className="block h-full">
                <Card variant="interactive" padding="md" className="group relative h-full">
                  <span className="pointer-events-none absolute right-4 top-4 select-none text-[64px] font-bold leading-none text-text/[0.03]">
                    01
                  </span>
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <svg className="h-5 w-5 text-primary" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15a2.25 2.25 0 0 1 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25Z" />
                    </svg>
                  </div>
                  <h3 className="mt-5 text-lg font-semibold tracking-tight">실전처럼 푸는 모의고사</h3>
                  <p className="mt-2 text-sm leading-relaxed text-text-muted">
                    자격증별 시험 시간과 문제 구성에 맞춰 실제 CBT처럼 풀고, 제출 즉시 점수와
                    해설을 확인할 수 있습니다.
                  </p>
                </Card>
              </Link>
            </ScrollReveal>

            <ScrollReveal delay={2}>
              <Link href="/wrong-answers" className="block h-full xl:-translate-y-2">
                <Card variant="interactive" padding="md" className="group relative h-full">
                  <span className="pointer-events-none absolute right-4 top-4 select-none text-[64px] font-bold leading-none text-text/[0.03]">
                    02
                  </span>
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <svg className="h-5 w-5 text-primary" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" />
                    </svg>
                  </div>
                  <h3 className="mt-5 text-lg font-semibold tracking-tight">틀린 문제만 자동 복습</h3>
                  <p className="mt-2 text-sm leading-relaxed text-text-muted">
                    틀린 문제는 오답노트에 자동 저장됩니다. 시험 전 다시 봐야 할 문제만 모아
                    빠르게 복습해보세요.
                  </p>
                </Card>
              </Link>
            </ScrollReveal>

            <ScrollReveal delay={3}>
              <Link href="/dashboard" className="block h-full">
                <Card variant="interactive" padding="md" className="group relative h-full">
                  <span className="pointer-events-none absolute right-4 top-4 select-none text-[64px] font-bold leading-none text-text/[0.03]">
                    03
                  </span>
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <svg className="h-5 w-5 text-primary" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                    </svg>
                  </div>
                  <h3 className="mt-5 text-lg font-semibold tracking-tight">회차별 실력 추적</h3>
                  <p className="mt-2 text-sm leading-relaxed text-text-muted">
                    풀었던 회차, 점수, 풀이 시간을 기록해 시험 전까지 내 실력이 어떻게 변하는지
                    확인할 수 있습니다.
                  </p>
                </Card>
              </Link>
            </ScrollReveal>

            <ScrollReveal delay={4}>
              <Link href="/past-exams" className="block h-full">
                <Card variant="interactive" padding="md" className="group relative h-full">
                  <span className="pointer-events-none absolute right-4 top-4 select-none text-[64px] font-bold leading-none text-text/[0.03]">
                    04
                  </span>
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <svg className="h-5 w-5 text-primary" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                    </svg>
                  </div>
                  <h3 className="mt-5 text-lg font-semibold tracking-tight">실제 시험 그대로, 회차별 복원</h3>
                  <p className="mt-2 text-sm leading-relaxed text-text-muted">
                    최근 회차를 연도별로 모아 타이머·해설과 함께 풀어볼 수 있습니다. 로그인
                    없이도 결과를 바로 확인하세요.
                  </p>
                </Card>
              </Link>
            </ScrollReveal>
          </div>
        </Container>
      </Section>

      {/* ── Blog ─────────────────────────────────────────── */}
      {recentPosts.length > 0 && (
        <Section>
          <Container size="default">
            <ScrollReveal>
              <h2 className="text-center text-2xl font-bold tracking-tight sm:text-3xl md:text-4xl">
                시험 준비 팁
              </h2>
              <p className="mt-3 text-center text-text-muted">
                과목별 핵심 개념 정리와 합격 전략을 확인하세요
              </p>
            </ScrollReveal>

            <div className="mt-14 grid grid-cols-1 gap-5 md:grid-cols-3">
              {recentPosts.map((post, i) => {
                const cert = certFromBlogCategory(post.category);
                return (
                  <ScrollReveal key={post.slug} delay={i + 1}>
                    <Link href={`/blog/${post.slug}`} className="group block h-full">
                      <Card variant="interactive" padding="md" className="flex h-full flex-col">
                        <div className="flex items-center gap-2">
                          {cert ? (
                            <Badge cert={cert} variant="soft" size="xs">
                              {post.category}
                            </Badge>
                          ) : (
                            <Badge variant="soft" tone="neutral" size="xs">
                              {post.category}
                            </Badge>
                          )}
                          <span className="text-xs text-text-muted">
                            {new Date(post.date).toLocaleDateString("ko-KR", {
                              month: "long",
                              day: "numeric",
                            })}
                          </span>
                        </div>
                        <h3 className="mt-3 text-base font-semibold leading-snug group-hover:text-primary">
                          {post.title}
                        </h3>
                        <p className="mt-2 flex-1 text-sm leading-relaxed text-text-muted line-clamp-2">
                          {post.description}
                        </p>
                        <span className="mt-4 text-xs font-medium text-primary">읽어보기 →</span>
                      </Card>
                    </Link>
                  </ScrollReveal>
                );
              })}
            </div>

            <ScrollReveal delay={4}>
              <div className="mt-12 text-center">
                <ButtonLink href="/blog" variant="outline" size="md">
                  전체 글 보기
                </ButtonLink>
              </div>
            </ScrollReveal>
          </Container>
        </Section>
      )}

      {/* ── Ranking ────────────────────────────────────────── */}
      <ScrollReveal>
        <RankingSection />
      </ScrollReveal>

      {/* ── CTA ────────────────────────────────────────────── */}
      <Section>
        <Container size="default" className="text-center">
          <div className="shimmer-line mx-auto mb-14 max-w-md" />
          <ScrollReveal>
            <h2 className="text-2xl font-bold tracking-tight sm:text-3xl md:text-4xl">
              오늘의 한 세트, 지금 시작
            </h2>
            <p className="mt-3 text-text-muted">회원가입 없이 바로 풀어볼 수 있습니다.</p>
            <div className="mt-8">
              <ButtonLink href="/solve" variant="primary" size="lg" glow>
                문제 풀러 가기
              </ButtonLink>
            </div>
          </ScrollReveal>
        </Container>
      </Section>
    </main>
  );
}

