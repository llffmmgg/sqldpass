import Image from "next/image";
import HeroCta from "@/components/HeroCta";
import EngineerExamCountdown from "@/components/EngineerExamCountdown";
import { SiteNoticeModal } from "@/components/SiteNoticeModal";
import CertChips from "@/components/CertChips";
import PreviewTabs from "@/components/PreviewTabs";
import RankingSection from "@/components/RankingSection";
import {
  getPublicStats,
  getPublicRanking,
  type PublicStats,
  type PublicRanking,
} from "@/lib/publicApi";

export const revalidate = 3600;

async function fetchStatsSafe(): Promise<PublicStats> {
  try {
    return await getPublicStats();
  } catch (e) {
    console.warn("[stats] fallback to zero:", e);
    return { totalMembers: 0, totalSolves: 0 };
  }
}

async function fetchRankingSafe(): Promise<PublicRanking> {
  try {
    return await getPublicRanking();
  } catch (e) {
    console.warn("[ranking] fallback to empty:", e);
    return { entries: [], generatedAt: new Date().toISOString() };
  }
}

export default async function Home() {
  const [stats, ranking] = await Promise.all([fetchStatsSafe(), fetchRankingSafe()]);
  return (
    <main className="min-h-screen bg-background text-foreground">
      <SiteNoticeModal />

      {/* ── Hero ── */}
      <section className="border-b border-border">
        <div className="mx-auto max-w-5xl px-4 py-20 sm:px-6 sm:py-28 lg:px-8">
          <div className="flex flex-col items-center gap-10 lg:flex-row lg:items-center lg:justify-between lg:gap-16">
            {/* Text */}
            <div className="max-w-xl text-center lg:text-left">
              <h1 className="text-3xl font-bold tracking-tight sm:text-4xl lg:text-5xl">
                자격증 시험,
                <br />
                <span className="text-primary">실전처럼 풀자</span>
              </h1>
              <p className="mt-5 text-base leading-relaxed text-muted sm:text-lg">
                SQLD &middot; 정처기 실기 &middot; 컴활 1급 필기.
                <br className="hidden sm:block" />
                매번 새로 구성되는 CBT 모의고사로 연습하고,
                <br className="hidden sm:block" />
                오답 복습과 실력 추적까지.
              </p>

              <div className="mt-6">
                <CertChips />
              </div>

              <div className="mt-8">
                <HeroCta />
              </div>

              <div className="mt-6">
                <EngineerExamCountdown />
              </div>
            </div>

            {/* Mascot */}
            <div className="shrink-0">
              <Image
                src="/logo/logo.png"
                alt="sqldpass 마스코트"
                width={280}
                height={280}
                className="opacity-90"
                priority
              />
            </div>
          </div>

          {/* Stats */}
          <div className="mt-14 flex items-center justify-center gap-8 border-t border-border pt-8 text-sm text-muted lg:justify-start">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-foreground tabular-nums">
                {stats.totalMembers.toLocaleString("ko-KR")}
              </span>
              명 학습 중
            </div>
            <span className="h-4 w-px bg-border" />
            <div className="flex items-center gap-2">
              <span className="font-semibold text-foreground tabular-nums">
                {stats.totalSolves.toLocaleString("ko-KR")}
              </span>
              문제 풀이 완료
            </div>
          </div>
        </div>
      </section>

      {/* ── Features ── */}
      <section className="border-b border-border">
        <div className="mx-auto max-w-5xl px-4 py-20 sm:px-6 lg:px-8">
          <h2 className="text-2xl font-bold sm:text-3xl">이런 걸 할 수 있어요</h2>

          <div className="mt-10 grid grid-cols-1 gap-px overflow-hidden rounded-xl border border-border bg-border sm:grid-cols-3">
            <div className="bg-background p-6">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15a2.25 2.25 0 0 1 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25Z" />
                </svg>
              </div>
              <h3 className="mt-4 font-semibold">랜덤 모의고사</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted">
                누를 때마다 새로 구성되는 실전 세트.
                실제 시험과 동일한 CBT 환경.
              </p>
            </div>

            <div className="bg-background p-6">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent/10 text-accent">
                <svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" />
                </svg>
              </div>
              <h3 className="mt-4 font-semibold">오답 자동 복습</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted">
                틀린 문제만 모아서 취약 영역 분석.
                해설과 함께 약점만 골라 다시 풀기.
              </p>
            </div>

            <div className="bg-background p-6">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                </svg>
              </div>
              <h3 className="mt-4 font-semibold">실력 추적</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted">
                점수 추이와 풀이 시간을 자동 기록.
                합격 준비도를 한눈에 확인.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ── Ranking ── */}
      <RankingSection data={ranking} />

      {/* ── Preview ── */}
      <section id="preview" className="border-t border-border">
        <div className="mx-auto max-w-5xl px-4 py-20 sm:px-6 lg:px-8">
          <h2 className="text-2xl font-bold sm:text-3xl">문제 미리보기</h2>
          <p className="mt-2 text-muted">
            각 자격증의 실제 출제 유형을 가볍게 풀어보세요
          </p>
          <div className="mt-8">
            <PreviewTabs />
          </div>
        </div>
      </section>

      {/* ── CTA ── */}
      <section className="border-t border-border">
        <div className="mx-auto max-w-5xl px-4 py-20 text-center sm:px-6 lg:px-8">
          <h2 className="text-2xl font-bold sm:text-3xl">준비됐으면, 바로 풀어보세요</h2>
          <p className="mt-3 text-muted">회원가입 없이 바로 시작할 수 있습니다.</p>
          <a
            href="/solve"
            className="mt-8 inline-flex items-center rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-zinc-900 transition-colors hover:bg-primary-hover"
          >
            문제 풀러 가기
          </a>
        </div>
      </section>
    </main>
  );
}
