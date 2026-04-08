"use client";

import { useEffect, useState } from "react";
import {
  getWrongAnswers,
  getWrongAnswerStats,
  getSubjects,
  getQuestionDetail,
  retryWrongAnswer,
  type WrongAnswerResponse,
  type WrongAnswerStatsResponse,
  type Subject,
  type QuestionDetail,
  type WrongAnswerRetryResponse,
} from "@/lib/api";
import { formatDate } from "@/lib/format";
import { parseQuestion } from "@/lib/parseQuestion";
import QuestionContent from "@/components/QuestionContent";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";
import Link from "next/link";

function getLeafSubjects(subjects: Subject[]): { id: number; name: string }[] {
  const leaves: { id: number; name: string }[] = [];
  for (const s of subjects) {
    if (s.children.length > 0) {
      for (const child of s.children) {
        leaves.push({ id: child.id, name: child.name });
      }
    }
  }
  return leaves;
}

function rateColor(rate: number) {
  if (rate > 50) return { bar: "bg-red-500", text: "text-red-400" };
  if (rate > 30) return { bar: "bg-amber-500", text: "text-amber-400" };
  return { bar: "bg-green-500", text: "text-green-400" };
}

interface RetryState {
  selectedOption?: number;
  answerText?: string;
  result?: WrongAnswerRetryResponse;
  submitting?: boolean;
  error?: string;
}

export default function WrongAnswersPage() {
  return (
    <AuthGuard>
      <WrongAnswersPageContent />
    </AuthGuard>
  );
}

function WrongAnswersPageContent() {
  const [stats, setStats] = useState<WrongAnswerStatsResponse[]>([]);
  const [wrongAnswers, setWrongAnswers] = useState<WrongAnswerResponse[]>([]);
  const [subjects, setSubjects] = useState<{ id: number; name: string }[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [details, setDetails] = useState<Record<number, QuestionDetail>>({});
  const [retryState, setRetryState] = useState<Record<number, RetryState>>({});
  const [masteredIds, setMasteredIds] = useState<Set<number>>(new Set());

  function reloadList() {
    return Promise.all([
      getWrongAnswers(selectedSubject ?? undefined),
      getWrongAnswerStats(),
    ]).then(([wrongData, statsData]) => {
      setWrongAnswers(wrongData);
      setStats(statsData);
    });
  }

  useEffect(() => {
    Promise.all([
      getWrongAnswerStats(),
      getWrongAnswers(),
      getSubjects(),
    ])
      .then(([statsData, wrongData, subjectsData]) => {
        setStats(statsData);
        setWrongAnswers(wrongData);
        setSubjects(getLeafSubjects(subjectsData));
      })
      .finally(() => setLoading(false));
  }, []);

  function handleSubjectFilter(subjectId: number | null) {
    setSelectedSubject(subjectId);
    setLoading(true);
    setExpandedId(null);
    getWrongAnswers(subjectId ?? undefined)
      .then(setWrongAnswers)
      .finally(() => setLoading(false));
  }

  function handleExpand(questionId: number) {
    if (expandedId === questionId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(questionId);
    if (!details[questionId]) {
      getQuestionDetail(questionId)
        .then((detail) => {
          setDetails((prev) => ({ ...prev, [questionId]: detail }));
        })
        .catch(() => {
          // 로딩 실패 — 상태에 에러 표시
          setRetryState((prev) => ({
            ...prev,
            [questionId]: { ...prev[questionId], error: "문제를 불러오지 못했습니다." },
          }));
        });
    }
  }

  function setRetryField(questionId: number, patch: Partial<RetryState>) {
    setRetryState((prev) => ({
      ...prev,
      [questionId]: { ...prev[questionId], ...patch },
    }));
  }

  async function handleSubmitRetry(questionId: number) {
    const state = retryState[questionId] ?? {};
    const detail = details[questionId];
    if (!detail) return;

    // 입력 검증
    if (detail.questionType === "MCQ") {
      if (!state.selectedOption) {
        setRetryField(questionId, { error: "답을 선택해주세요." });
        return;
      }
    } else {
      if (!state.answerText || state.answerText.trim().length === 0) {
        setRetryField(questionId, { error: "답을 입력해주세요." });
        return;
      }
    }

    setRetryField(questionId, { submitting: true, error: undefined });
    try {
      const res = await retryWrongAnswer(questionId, {
        selectedOption: state.selectedOption,
        answerText: state.answerText,
      });
      setRetryField(questionId, { result: res, submitting: false });

      if (res.correct) {
        // 마스터 애니메이션 → 1.5초 후 목록 갱신
        setMasteredIds((prev) => new Set(prev).add(questionId));
        setTimeout(() => {
          reloadList().then(() => {
            setMasteredIds((prev) => {
              const next = new Set(prev);
              next.delete(questionId);
              return next;
            });
            setExpandedId(null);
            // 상태 정리
            setRetryState((prev) => {
              const next = { ...prev };
              delete next[questionId];
              return next;
            });
          });
        }, 1500);
      }
    } catch (e) {
      setRetryField(questionId, {
        submitting: false,
        error: e instanceof Error ? e.message : "제출에 실패했습니다.",
      });
    }
  }

  function handleRetryAgain(questionId: number) {
    // 오답 후 다시 시도 — 입력 초기화
    setRetryState((prev) => ({
      ...prev,
      [questionId]: { submitting: false },
    }));
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        <h1 className="text-2xl font-bold sm:text-3xl">오답 노트</h1>
        <p className="mt-2 text-sm text-muted">
          틀린 문제를 다시 풀어 마스터하세요. 다시 맞히면 목록에서 자동으로 사라집니다.
        </p>

        {/* Stats */}
        {stats.length > 0 && (
          <section className="mt-8">
            <h2 className="text-lg font-semibold">취약 영역 분석</h2>
            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              {stats.map((stat) => {
                const color = rateColor(stat.wrongRate);
                return (
                  <div
                    key={stat.subjectId}
                    className="rounded-xl border border-border bg-surface p-5"
                  >
                    <p className="text-sm font-medium">{stat.subjectName}</p>
                    <div className="mt-3 h-2 rounded-full bg-border">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${color.bar}`}
                        style={{ width: `${stat.wrongRate}%` }}
                      />
                    </div>
                    <p className={`mt-2 text-sm ${color.text}`}>
                      {stat.wrongCount}문제 미해결 / {stat.totalSolved}문제 시도 ({stat.wrongRate}%)
                    </p>
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {/* Wrong answer list */}
        <section className="mt-12">
          <h2 className="text-lg font-semibold">오답 문제</h2>

          {/* Subject filter pills */}
          <div className="mt-4 flex flex-wrap gap-2">
            <button
              onClick={() => handleSubjectFilter(null)}
              className={`rounded-full px-3 py-1 text-sm font-medium transition-colors ${
                selectedSubject === null
                  ? "bg-primary text-zinc-900"
                  : "border border-border text-muted hover:border-amber-500/40"
              }`}
            >
              전체
            </button>
            {subjects.map((s) => (
              <button
                key={s.id}
                onClick={() => handleSubjectFilter(s.id)}
                className={`rounded-full px-3 py-1 text-sm font-medium transition-colors ${
                  selectedSubject === s.id
                    ? "bg-primary text-zinc-900"
                    : "border border-border text-muted hover:border-amber-500/40"
                }`}
              >
                {s.name}
              </button>
            ))}
          </div>

          <div className="mt-4 space-y-3">
            {loading && <Spinner />}

            {!loading && wrongAnswers.length === 0 && (
              <div className="py-16 text-center">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-500/10 border border-green-500/20">
                  <svg className="h-8 w-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                  </svg>
                </div>
                <p className="mt-4 text-muted">오답이 없습니다. 완벽해요!</p>
                <Link
                  href="/solve"
                  className="mt-6 inline-block rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-zinc-900 transition-colors hover:bg-primary-hover"
                >
                  문제 풀러 가기
                </Link>
              </div>
            )}

            {!loading &&
              wrongAnswers.map((wa) => {
                const isExpanded = expandedId === wa.questionId;
                const isMastered = masteredIds.has(wa.questionId);
                const detail = details[wa.questionId];
                const state = retryState[wa.questionId] ?? {};
                const parsed = detail ? parseQuestion(detail.content) : null;

                return (
                  <div
                    key={wa.questionId}
                    className={`rounded-lg border bg-surface px-5 py-4 transition-all duration-500 ${
                      isMastered
                        ? "scale-95 border-green-500/60 bg-green-500/10 opacity-60"
                        : "border-border"
                    }`}
                  >
                    <div
                      className="cursor-pointer"
                      onClick={() => handleExpand(wa.questionId)}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <p className="flex-1 text-sm leading-relaxed line-clamp-3 whitespace-pre-line">
                          {wa.questionContent}
                        </p>
                        <svg
                          className={`h-4 w-4 shrink-0 text-muted transition-transform ${
                            isExpanded ? "rotate-180" : ""
                          }`}
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                          strokeWidth={2}
                        >
                          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                        </svg>
                      </div>
                      <div className="mt-2 flex items-center gap-2">
                        <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-400">
                          {wa.subjectName}
                        </span>
                        <span className="text-xs text-red-400">
                          {wa.wrongCount}회 오답
                        </span>
                        <span className="text-xs text-muted">
                          {formatDate(wa.lastWrongAt)}
                        </span>
                      </div>
                    </div>

                    <div
                      className={`grid transition-all duration-300 ease-in-out ${
                        isExpanded ? "grid-rows-[1fr] opacity-100 mt-3" : "grid-rows-[0fr] opacity-0"
                      }`}
                    >
                      <div className="overflow-hidden">
                        {!detail ? (
                          <div className="py-4 text-center text-sm text-muted">로딩 중...</div>
                        ) : (
                          <div className="space-y-3">
                            {/* 본문 */}
                            <div className="rounded-lg border border-border px-3 py-3">
                              <QuestionContent content={parsed?.body ?? ""} />
                            </div>

                            {/* 마스터 완료 메시지 */}
                            {isMastered && (
                              <div className="rounded-lg border border-green-500/40 bg-green-500/10 px-4 py-3 text-center">
                                <p className="text-base font-semibold text-green-300">
                                  🎉 정답! 마스터 완료
                                </p>
                                <p className="mt-1 text-xs text-green-400/80">
                                  잠시 후 목록에서 사라집니다
                                </p>
                              </div>
                            )}

                            {/* 풀이 영역 */}
                            {!isMastered && !state.result?.correct && (
                              <RetrySolverInline
                                detail={detail}
                                parsedOptions={parsed?.options ?? []}
                                state={state}
                                onChange={(patch) => setRetryField(wa.questionId, patch)}
                                onSubmit={() => handleSubmitRetry(wa.questionId)}
                              />
                            )}

                            {/* 결과: 오답일 때 정답+해설 노출 */}
                            {state.result && !state.result.correct && (
                              <div className="space-y-3">
                                <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3">
                                  <p className="text-sm font-semibold text-red-300">
                                    ❌ 다시 도전!
                                  </p>
                                  {detail.questionType === "MCQ" ? (
                                    <p className="mt-1 text-sm text-red-200/90">
                                      정답: <span className="font-bold">{state.result.correctOption}번</span>
                                    </p>
                                  ) : (
                                    <p className="mt-1 text-sm text-red-200/90">
                                      모범답안: <span className="font-mono">{state.result.correctAnswer}</span>
                                    </p>
                                  )}
                                </div>
                                <div className="rounded-lg border border-border px-3 py-3 text-sm">
                                  <p className="font-medium text-amber-400">해설</p>
                                  <div className="mt-1 leading-relaxed text-muted">
                                    <QuestionContent content={state.result.explanation ?? ""} />
                                  </div>
                                </div>
                                <button
                                  onClick={() => handleRetryAgain(wa.questionId)}
                                  className="w-full rounded-lg border border-amber-500/40 bg-amber-500/10 py-2 text-sm font-semibold text-amber-300 transition hover:bg-amber-500/20"
                                >
                                  🔁 다시 풀어보기
                                </button>
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
          </div>
        </section>
      </div>
    </main>
  );
}

// ----------------------------------------------------------
// 인라인 풀이 컴포넌트
// ----------------------------------------------------------
function RetrySolverInline({
  detail,
  parsedOptions,
  state,
  onChange,
  onSubmit,
}: {
  detail: QuestionDetail;
  parsedOptions: string[];
  state: RetryState;
  onChange: (patch: Partial<RetryState>) => void;
  onSubmit: () => void;
}) {
  const isMcq = detail.questionType === "MCQ";

  return (
    <div className="space-y-3">
      {isMcq ? (
        <div className="space-y-2">
          {parsedOptions.map((opt, idx) => {
            const optionNumber = idx + 1;
            const isSelected = state.selectedOption === optionNumber;
            return (
              <button
                key={optionNumber}
                type="button"
                onClick={() => onChange({ selectedOption: optionNumber })}
                disabled={state.submitting}
                className={`flex w-full items-start gap-3 rounded-lg border px-4 py-3 text-left text-sm transition ${
                  isSelected
                    ? "border-amber-500/60 bg-amber-500/10 text-amber-200 ring-1 ring-amber-400/40"
                    : "border-border bg-background text-foreground hover:border-amber-500/30"
                } disabled:cursor-not-allowed disabled:opacity-50`}
              >
                <span
                  className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                    isSelected
                      ? "bg-amber-500/30 text-amber-100"
                      : "bg-border text-muted"
                  }`}
                >
                  {optionNumber}
                </span>
                <span className="flex-1 leading-relaxed">{opt}</span>
              </button>
            );
          })}
        </div>
      ) : (
        <textarea
          value={state.answerText ?? ""}
          onChange={(e) => onChange({ answerText: e.target.value })}
          disabled={state.submitting}
          rows={3}
          placeholder="답을 입력하세요"
          className="block w-full resize-none rounded-lg border border-border bg-background px-4 py-3 text-sm leading-relaxed text-foreground placeholder:text-muted/50 focus:outline-none focus:ring-2 focus:ring-amber-500/60 disabled:opacity-50"
        />
      )}

      {state.error && (
        <p className="text-xs text-red-400">{state.error}</p>
      )}

      <button
        onClick={onSubmit}
        disabled={state.submitting}
        className="w-full rounded-lg bg-primary py-2.5 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
      >
        {state.submitting ? "채점 중..." : "정답 제출"}
      </button>
    </div>
  );
}
