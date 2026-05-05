"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 시 풀이 상세 fetch 후 state 갱신 패턴이 필요. 외부 시스템(API) 동기화이므로 setState 호출이 자연스러움 */

import Link from "next/link";
import { useEffect, useState, use } from "react";
import { getSolve, getQuestionDetail, getSubjects, type SolveResponse, type QuestionDetail, type Subject } from "@/lib/api";
import { getPublicPastExam, type PublicPastExamDetail } from "@/lib/publicApi";
import { formatDate } from "@/lib/format";
import { parseQuestion, OPTION_MARKERS } from "@/lib/parseQuestion";
import QuestionContent from "@/components/QuestionContent";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";
import AdInfeed from "@/components/AdInfeed";
import AdDisplay from "@/components/AdDisplay";
import { Container } from "@/components/ui";
import { CERT_TOKENS, certFromExamType } from "@/lib/cert-tokens";
import { trackEvent } from "@/lib/gtag";

function buildSubjectMap(subjects: Subject[]): Record<number, string> {
  const map: Record<number, string> = {};
  for (const s of subjects) {
    map[s.id] = s.name;
    for (const child of s.children) {
      map[child.id] = child.name;
    }
  }
  return map;
}

/** PAST_EXAM 회차 라벨: "[SQLD] 2025년 57회 기출 복원" */
function buildPastExamLabel(exam: PublicPastExamDetail): string {
  const cert = certFromExamType(exam.examType);
  const certLabel = cert ? CERT_TOKENS[cert].label : exam.certSlug;
  const parts: string[] = [];
  if (exam.examYear != null) parts.push(`${exam.examYear}년`);
  if (exam.examRound != null) parts.push(`${exam.examRound}회`);
  if (parts.length === 0) parts.push(exam.name);
  return `[${certLabel}] ${parts.join(" ")} 기출 복원`;
}

export default function HistoryDetailPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <AuthGuard>
      <HistoryDetailContent params={params} />
    </AuthGuard>
  );
}

function HistoryDetailContent({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [solve, setSolve] = useState<SolveResponse | null>(null);
  const [details, setDetails] = useState<Record<number, QuestionDetail>>({});
  const [subjectMap, setSubjectMap] = useState<Record<number, string>>({});
  const [pastExam, setPastExam] = useState<PublicPastExamDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setError(null);
    setLoading(true);
    Promise.all([getSolve(Number(id)), getSubjects()])
      .then(([solveData, subjects]) => {
        setSolve(solveData);
        setSubjectMap(buildSubjectMap(subjects));
        // GA4 — 결과 페이지 조회
        trackEvent("view_result", {
          solve_id: solveData.id,
          mock_exam_id: solveData.mockExamId ?? undefined,
          subject_id: solveData.subjectId ?? undefined,
          score: solveData.score,
        });
        // 기출복원이면 회차 라벨용으로 메타 추가 fetch (실패 무시)
        if (solveData.mockExamId != null) {
          getPublicPastExam(solveData.mockExamId)
            .then((meta) => setPastExam(meta))
            .catch(() => setPastExam(null));
        }
        return Promise.all(
          solveData.answers.map((a) => getQuestionDetail(a.questionId))
        );
      })
      .then((questionDetails) => {
        const map: Record<number, QuestionDetail> = {};
        for (const q of questionDetails) {
          map[q.id] = q;
        }
        setDetails(map);
      })
      .catch((e) => {
        setError(e instanceof Error ? e.message : "풀이 내역을 불러올 수 없습니다.");
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (error) {
    return (
      <main className="min-h-screen bg-background text-foreground">
        <Container size="narrow" className="py-16 text-center">
          <p className="text-red-400">{error}</p>
          <Link href="/dashboard" className="mt-4 inline-block text-sm text-muted hover:text-foreground">
            ← 대시보드로
          </Link>
        </Container>
      </main>
    );
  }

  if (loading || !solve) {
    return (
      <main className="min-h-screen bg-background text-foreground">
        <Container size="narrow" className="py-16">
          <Spinner />
        </Container>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <Container size="narrow" className="py-16">
        {/* Header */}
        <Link href="/dashboard" className="text-sm text-muted hover:text-foreground transition-colors">
          &larr; 대시보드로
        </Link>

        {/* Score summary */}
        <div className="mt-6 rounded-xl border border-border bg-surface p-6 text-center">
          <p className="text-sm text-muted">
            {pastExam
              ? buildPastExamLabel(pastExam)
              : solve.mockExamId != null
              ? `모의고사 #${solve.mockExamId}`
              : solve.subjectId != null
              ? subjectMap[solve.subjectId] || `과목 ${solve.subjectId}`
              : "풀이"}
          </p>
          <p className="mt-2">
            <span className="text-5xl font-bold bg-gradient-to-r from-amber-400 to-amber-300 bg-clip-text text-transparent">
              {solve.score}
            </span>
            <span className="ml-1 text-lg text-muted">점</span>
          </p>
          <p className="mt-2 text-sm text-muted">
            {solve.correctCount}/{solve.totalCount} 정답 &middot; {formatDate(solve.solvedAt)}
          </p>
        </div>

        {/* 상단 광고 — 점수 확인 직후, 답안 검토 진입 직전 자리 */}
        <div className="mt-6 md:hidden">
          <AdInfeed adSlot="5227022543" adLayoutKey="-h4-h+1c-4h+8p" />
        </div>
        <div className="mt-6 hidden md:block">
          <AdDisplay adSlot="3622084801" />
        </div>

        {/* Answers */}
        <div className="mt-8 space-y-4">
          {solve.answers.map((answer, idx) => {
            const detail = details[answer.questionId];
            const parsed = detail ? parseQuestion(detail.content) : null;

            return (
              <div
                key={answer.questionId}
                className={`rounded-lg border px-5 py-4 ${
                  answer.correct
                    ? "border-green-500/30 bg-green-500/5"
                    : "border-red-500/30 bg-red-500/5"
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1">
                    <p className="text-sm font-medium text-muted">문제 {idx + 1}</p>
                    {parsed && (
                      <QuestionContent content={parsed.body} className="mt-1" />
                    )}
                  </div>
                  <span
                    className={`shrink-0 rounded px-2 py-0.5 text-xs font-semibold ${
                      answer.correct
                        ? "bg-green-500/20 text-green-400"
                        : "bg-red-500/20 text-red-400"
                    }`}
                  >
                    {answer.correct ? "\u2713 정답" : "\u2717 오답"}
                  </span>
                </div>

                {parsed && parsed.options.length > 0 && (
                  <div className="mt-3 space-y-1">
                    {parsed.options.map((opt, optIdx) => {
                      const optNum = optIdx + 1;
                      const isSelected = optNum === answer.selectedOption;
                      const isCorrect = optNum === answer.correctOption;
                      return (
                        <div
                          key={optIdx}
                          className={`flex items-start gap-2 rounded px-2 py-1 text-sm ${
                            isCorrect
                              ? "bg-green-500/10 text-green-400 font-medium"
                              : isSelected && !answer.correct
                              ? "bg-red-500/10 text-red-400"
                              : "text-muted"
                          }`}
                        >
                          <span className="shrink-0">{OPTION_MARKERS[optIdx]}</span>
                          <span className="min-w-0 flex-1">
                            <QuestionContent content={opt} className="mcq-option" />
                          </span>
                          {isCorrect && <span className="shrink-0">✓</span>}
                          {isSelected && !isCorrect && <span className="shrink-0">(선택)</span>}
                        </div>
                      );
                    })}
                  </div>
                )}

                {detail && detail.explanation && (
                  <details className="mt-3 rounded-lg border border-border px-3 py-2 text-sm">
                    <summary className="cursor-pointer font-medium text-amber-400">
                      해설 보기
                    </summary>
                    <div className="mt-2 leading-relaxed text-muted">
                      <QuestionContent content={detail.explanation} />
                    </div>
                  </details>
                )}
              </div>
            );
          })}
        </div>
      </Container>
    </main>
  );
}
