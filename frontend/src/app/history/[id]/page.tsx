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

        {/* Answers — Supabase 스타일: 일관 카드 + 헤더 분리, 상태는 칩/시맨틱 컬러로 */}
        <div className="mt-8 space-y-3">
          {solve.answers.map((answer, idx) => {
            const detail = details[answer.questionId];
            const parsed = detail ? parseQuestion(detail.content) : null;
            const myMarker =
              answer.selectedOption != null
                ? OPTION_MARKERS[answer.selectedOption - 1]
                : null;
            const correctMarker =
              answer.correctOption != null
                ? OPTION_MARKERS[answer.correctOption - 1]
                : null;

            return (
              <div
                key={answer.questionId}
                className="overflow-hidden rounded-lg border border-border bg-surface transition-colors hover:border-border-strong"
              >
                {/* 헤더 */}
                <div className="flex items-center justify-between gap-3 border-b border-border bg-bg-elevated px-5 py-2.5">
                  <div className="flex items-center gap-2.5">
                    <span
                      className={`inline-flex items-center rounded-md px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                        answer.correct
                          ? "bg-success/15 text-success"
                          : "bg-danger/15 text-danger"
                      }`}
                    >
                      {answer.correct ? "정답" : "오답"}
                    </span>
                    <span className="font-mono text-xs text-text-muted tabular-nums">
                      Q{String(idx + 1).padStart(2, "0")}
                    </span>
                  </div>
                  {!answer.correct && myMarker && correctMarker && (
                    <span className="font-mono text-[11px] tabular-nums">
                      <span className="text-danger">{myMarker}</span>
                      <span className="mx-1 text-text-subtle">{"→"}</span>
                      <span className="text-success">{correctMarker}</span>
                    </span>
                  )}
                </div>

                {/* 본문 */}
                <div className="px-5 py-4">
                  {parsed && <QuestionContent content={parsed.body} />}

                  {parsed && parsed.options.length > 0 && (
                    <ul className="mt-4 space-y-1.5">
                      {parsed.options.map((opt, optIdx) => {
                        const optNum = optIdx + 1;
                        const isSelected = optNum === answer.selectedOption;
                        const isCorrect = optNum === answer.correctOption;
                        const tone = isCorrect
                          ? "border-success/40 bg-success/[0.06] text-text"
                          : isSelected
                            ? "border-danger/40 bg-danger/[0.06] text-text"
                            : "border-border bg-bg text-text-muted";
                        const markerTone = isCorrect
                          ? "text-success"
                          : isSelected
                            ? "text-danger"
                            : "text-text-subtle";
                        return (
                          <li
                            key={optIdx}
                            className={`flex items-start gap-3 rounded-md border px-3 py-2 text-sm ${tone}`}
                          >
                            <span
                              className={`shrink-0 font-mono text-xs tabular-nums ${markerTone}`}
                            >
                              {OPTION_MARKERS[optIdx]}
                            </span>
                            <span className="min-w-0 flex-1">
                              <QuestionContent
                                content={opt}
                                className="mcq-option"
                              />
                            </span>
                            {isCorrect && (
                              <span className="shrink-0 rounded bg-success/15 px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-success">
                                정답
                              </span>
                            )}
                            {isSelected && !isCorrect && (
                              <span className="shrink-0 rounded bg-danger/15 px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-danger">
                                내 답
                              </span>
                            )}
                          </li>
                        );
                      })}
                    </ul>
                  )}
                </div>

                {/* 해설 */}
                {detail && detail.explanation && (
                  <details className="group border-t border-border">
                    <summary className="flex cursor-pointer list-none items-center justify-between gap-2 px-5 py-2.5 text-xs font-semibold text-text-muted transition-colors hover:bg-surface-hover">
                      <span className="flex items-center gap-2">
                        <span className="font-mono text-primary transition-transform duration-150 group-open:rotate-90">
                          {"▸"}
                        </span>
                        해설 보기
                      </span>
                      <span className="text-[10px] font-medium uppercase tracking-wider text-text-subtle">
                        <span className="group-open:hidden">펼치기</span>
                        <span className="hidden group-open:inline">접기</span>
                      </span>
                    </summary>
                    <div className="border-t border-border bg-bg-elevated px-5 py-4 text-sm leading-relaxed text-text">
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
