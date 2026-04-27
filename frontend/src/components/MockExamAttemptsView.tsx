"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { Button, Card, Container } from "@/components/ui";
import { CERT_TOKENS, certFromExamType, type CertKey } from "@/lib/cert-tokens";
import { formatRelativeDate } from "@/lib/format";
import type { SolveSummaryResponse } from "@/lib/api";

/**
 * 모의고사·기출복원 진입 시 노출되는 인터스티셜.
 * 이전 시도가 1개 이상일 때만 부모가 렌더한다.
 *
 * - 시도 카드: 점수·정답수·푼 일시. 클릭 시 /history/{id} (읽기 전용 리뷰)
 * - "새로 풀기" CTA: onStartNew 호출 → 부모가 풀이 UI 로 전환
 */
export interface MockExamAttemptsViewProps {
  attempts: SolveSummaryResponse[];
  examTitle: string;
  /** 자격증 컬러를 입히고 싶을 때. PAST_EXAM/AI 모두 examType 으로 결정 */
  examType?: string | null;
  /** 헤더 보조 메타 (예: 문항수, 시험일) */
  meta?: ReactNode;
  /** 새로 풀기 시작 */
  onStartNew: () => void;
}

export default function MockExamAttemptsView({
  attempts,
  examTitle,
  examType,
  meta,
  onStartNew,
}: MockExamAttemptsViewProps) {
  const cert: CertKey | null = certFromExamType(examType);
  const token = cert ? CERT_TOKENS[cert] : null;

  return (
    <main className="min-h-screen bg-background text-foreground">
      <Container size="narrow" className="py-12">
        <header className="mb-6">
          <div className="flex flex-wrap items-center gap-2 text-xs text-text-muted">
            {token && (
              <span
                className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 text-[11px] font-semibold ${token.tailwind.border} ${token.tailwind.bgSoft} ${token.tailwind.text}`}
              >
                <span className={`h-1.5 w-1.5 rounded-full ${token.tailwind.dot}`} aria-hidden />
                {token.label}
              </span>
            )}
            {meta}
          </div>
          <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">
            {examTitle}
          </h1>
          <p className="mt-2 text-sm text-text-muted">
            이전에 푼 기록이 {attempts.length}개 있어요. 이어서 새로 풀거나 결과를 다시 확인해보세요.
          </p>
        </header>

        <div className="mb-6 flex flex-wrap items-center gap-2">
          <Button
            variant="primary"
            size="lg"
            glow
            onClick={onStartNew}
            rightIcon={
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5} aria-hidden>
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
              </svg>
            }
          >
            새로 풀기
          </Button>
          <p className="text-xs text-text-subtle">
            새 시도는 별도로 기록되며 통계에 반영됩니다.
          </p>
        </div>

        <section>
          <div className="mb-3 flex items-baseline justify-between">
            <h2 className="text-sm font-semibold">이전 풀이 기록</h2>
            <p className="text-xs text-text-muted">최신순 · 클릭하면 상세 보기</p>
          </div>

          <ul className="space-y-2">
            {attempts.map((a) => {
              const accuracy =
                a.totalCount > 0 ? Math.round((a.correctCount / a.totalCount) * 100) : 0;
              return (
                <li key={a.id}>
                  <Link
                    href={`/history/${a.id}`}
                    className="group block"
                  >
                    <Card
                      variant="interactive"
                      padding="none"
                      className="flex items-center gap-4 p-4"
                    >
                      <ScoreRing score={accuracy} />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-baseline gap-2">
                          <span className="text-sm font-semibold tabular-nums">
                            {a.correctCount}
                            <span className="text-text-muted">/{a.totalCount}</span>
                            <span className="ml-1 text-xs font-normal text-text-muted">정답</span>
                          </span>
                          <span className="text-xs text-text-subtle tabular-nums">
                            점수 {a.score}
                          </span>
                        </div>
                        <p className="mt-1 text-xs text-text-muted">
                          {formatRelativeDate(a.solvedAt)}
                        </p>
                      </div>
                      <span className="hidden text-xs font-medium text-text-muted group-hover:text-text sm:inline-flex sm:items-center sm:gap-1">
                        상세 보기
                        <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5} aria-hidden>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                        </svg>
                      </span>
                    </Card>
                  </Link>
                </li>
              );
            })}
          </ul>
        </section>
      </Container>
    </main>
  );
}

function scoreColor(score: number) {
  if (score >= 80) return "text-green-400";
  if (score >= 60) return "text-amber-400";
  return "text-red-400";
}

function ScoreRing({ score }: { score: number }) {
  const clamped = Math.max(0, Math.min(100, score));
  return (
    <div
      className={`relative h-12 w-12 shrink-0 ${scoreColor(score)}`}
      aria-label={`정답률 ${score}%`}
    >
      <svg viewBox="0 0 36 36" className="h-full w-full -rotate-90">
        <circle
          cx="18"
          cy="18"
          r="15.9155"
          fill="none"
          stroke="currentColor"
          strokeOpacity="0.15"
          strokeWidth="3"
        />
        <circle
          cx="18"
          cy="18"
          r="15.9155"
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray={`${clamped} 100`}
        />
      </svg>
      <span className="absolute inset-0 flex items-center justify-center text-[11px] font-bold tabular-nums">
        {score}%
      </span>
    </div>
  );
}
