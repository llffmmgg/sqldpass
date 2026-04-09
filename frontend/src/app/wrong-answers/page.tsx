"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
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
import { formatRelativeDate } from "@/lib/format";
import { parseQuestion } from "@/lib/parseQuestion";
import QuestionContent from "@/components/QuestionContent";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";
import Link from "next/link";
import { trackEvent } from "@/lib/gtag";

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

interface RetryState {
  selectedOption?: number;
  answerText?: string;
  result?: WrongAnswerRetryResponse;
  submitting?: boolean;
  error?: string;
}

type SortMode = "priority" | "recent";

export default function WrongAnswersPage() {
  return (
    <AuthGuard>
      <Suspense fallback={<main className="min-h-screen bg-background flex items-center justify-center"><Spinner /></main>}>
        <WrongAnswersPageContent />
      </Suspense>
    </AuthGuard>
  );
}

function WrongAnswersPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // GA4 — 오답노트 페이지 진입
  useEffect(() => {
    trackEvent("review_wrong");
  }, []);

  const [stats, setStats] = useState<WrongAnswerStatsResponse[]>([]);
  const [wrongAnswers, setWrongAnswers] = useState<WrongAnswerResponse[]>([]);
  const [subjects, setSubjects] = useState<{ id: number; name: string }[]>([]);
  const initialSubject = (() => {
    const v = searchParams?.get("subjectId");
    const n = v ? Number(v) : NaN;
    return Number.isFinite(n) && n > 0 ? n : null;
  })();
  const [selectedSubject, setSelectedSubject] = useState<number | null>(initialSubject);
  const [sortMode, setSortMode] = useState<SortMode>("priority");
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
      getWrongAnswers(initialSubject ?? undefined),
      getSubjects(),
    ])
      .then(([statsData, wrongData, subjectsData]) => {
        setStats(statsData);
        setWrongAnswers(wrongData);
        setSubjects(getLeafSubjects(subjectsData));
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleSubjectFilter(subjectId: number | null) {
    setSelectedSubject(subjectId);
    setLoading(true);
    setExpandedId(null);
    // URL 동기화 (딥링크)
    const next = subjectId ? `/wrong-answers?subjectId=${subjectId}` : "/wrong-answers";
    router.replace(next);
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
        setMasteredIds((prev) => new Set(prev).add(questionId));
        setTimeout(() => {
          reloadList().then(() => {
            setMasteredIds((prev) => {
              const next = new Set(prev);
              next.delete(questionId);
              return next;
            });
            setExpandedId(null);
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
    setRetryState((prev) => ({
      ...prev,
      [questionId]: { submitting: false },
    }));
  }

  // 오답이 있는 leaf 과목만 필터에 노출, 카운트 뱃지 계산
  const wrongCountBySubject = useMemo(() => {
    const map = new Map<number, number>();
    for (const s of stats) {
      if (s.wrongCount > 0) map.set(s.subjectId, s.wrongCount);
    }
    return map;
  }, [stats]);

  const visibleSubjectFilters = useMemo(
    () => subjects.filter((s) => wrongCountBySubject.has(s.id)),
    [subjects, wrongCountBySubject]
  );

  // 정렬 적용
  const sortedAnswers = useMemo(() => {
    const arr = [...wrongAnswers];
    if (sortMode === "priority") {
      arr.sort((a, b) => {
        if (b.wrongCount !== a.wrongCount) return b.wrongCount - a.wrongCount;
        return new Date(b.lastWrongAt).getTime() - new Date(a.lastWrongAt).getTime();
      });
    } else {
      arr.sort(
        (a, b) => new Date(b.lastWrongAt).getTime() - new Date(a.lastWrongAt).getTime()
      );
    }
    return arr;
  }, [wrongAnswers, sortMode]);

  // 상단 한 줄 요약용
  const totalUnresolved = wrongAnswers.length;
  const topWeak = useMemo(
    () => [...stats].filter((s) => s.wrongCount > 0).sort((a, b) => b.wrongRate - a.wrongRate)[0] ?? null,
    [stats]
  );

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        <h1 className="text-2xl font-bold sm:text-3xl">오답 노트</h1>
        <p className="mt-2 text-sm text-muted">
          틀린 문제를 다시 풀어 마스터하세요. 다시 맞히면 목록에서 자동으로 사라집니다.
        </p>

        {/* 한 줄 요약 — 기존 「취약 영역 분석」 카드 대체 */}
        {totalUnresolved > 0 && (
          <p className="mt-3 text-xs text-muted/80">
            총 <span className="font-semibold text-foreground">{totalUnresolved}개</span> 미해결
            {topWeak && (
              <>
                {" "}· 가장 취약:{" "}
                <span className="font-semibold text-red-300">
                  {topWeak.subjectName} ({topWeak.wrongRate}%)
                </span>
              </>
            )}
          </p>
        )}

        {/* Wrong answer list */}
        <section className="mt-8">
          {/* 필터 + 정렬 */}
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => handleSubjectFilter(null)}
              className={`rounded-full px-3 py-1 text-sm font-medium transition-colors ${
                selectedSubject === null
                  ? "bg-primary text-zinc-900"
                  : "border border-border text-muted hover:border-amber-500/40"
              }`}
            >
              전체 <span className="ml-1 text-[11px] tabular-nums opacity-70">{totalUnresolved}</span>
            </button>
            {visibleSubjectFilters.map((s) => {
              const count = wrongCountBySubject.get(s.id) ?? 0;
              const isActive = selectedSubject === s.id;
              return (
                <button
                  key={s.id}
                  onClick={() => handleSubjectFilter(s.id)}
                  className={`rounded-full px-3 py-1 text-sm font-medium transition-colors ${
                    isActive
                      ? "bg-primary text-zinc-900"
                      : "border border-border text-muted hover:border-amber-500/40"
                  }`}
                >
                  {s.name}{" "}
                  <span className={`ml-1 text-[11px] tabular-nums ${isActive ? "opacity-80" : "opacity-70"}`}>
                    {count}
                  </span>
                </button>
              );
            })}

            {/* 정렬 토글 */}
            <div className="ml-auto flex items-center gap-1 text-xs">
              <button
                onClick={() => setSortMode("priority")}
                className={`rounded px-2 py-1 transition-colors ${
                  sortMode === "priority"
                    ? "text-foreground font-semibold"
                    : "text-muted hover:text-foreground"
                }`}
              >
                오답 많은 순
              </button>
              <span className="text-muted/40">·</span>
              <button
                onClick={() => setSortMode("recent")}
                className={`rounded px-2 py-1 transition-colors ${
                  sortMode === "recent"
                    ? "text-foreground font-semibold"
                    : "text-muted hover:text-foreground"
                }`}
              >
                최근순
              </button>
            </div>
          </div>

          <div className="mt-4 space-y-3">
            {loading && <Spinner />}

            {!loading && sortedAnswers.length === 0 && (
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
              sortedAnswers.map((wa) => {
                const isExpanded = expandedId === wa.questionId;
                const isMastered = masteredIds.has(wa.questionId);
                const detail = details[wa.questionId];
                const state = retryState[wa.questionId] ?? {};
                const parsed = detail ? parseQuestion(detail.content) : null;

                // 우선순위에 따른 시각 차별
                const priority: "high" | "mid" | "low" =
                  wa.wrongCount >= 3 ? "high" : wa.wrongCount === 2 ? "mid" : "low";

                const borderClass = isMastered
                  ? "border-green-500/60 bg-green-500/10 opacity-60 scale-95 border"
                  : priority === "high"
                  ? "border border-border border-l-4 border-l-red-500/70"
                  : priority === "mid"
                  ? "border border-border border-l-4 border-l-amber-500/70"
                  : "border border-border";

                const wrongCountBadge = (() => {
                  if (priority === "high") return { dot: "🔴", color: "text-red-400" };
                  if (priority === "mid") return { dot: "🟡", color: "text-amber-400" };
                  return { dot: "⚪", color: "text-muted" };
                })();

                return (
                  <div
                    key={wa.questionId}
                    className={`rounded-lg bg-surface px-5 py-4 transition-all duration-500 ${borderClass}`}
                  >
                    <div className="cursor-pointer" onClick={() => handleExpand(wa.questionId)}>
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
                        <span className={`inline-flex items-center gap-1 text-xs ${wrongCountBadge.color}`}>
                          <span aria-hidden="true">{wrongCountBadge.dot}</span>
                          {wa.wrongCount}회 틀림
                        </span>
                        <span className="text-xs text-muted">{formatRelativeDate(wa.lastWrongAt)}</span>
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
                            <div className="rounded-lg border border-border px-3 py-3">
                              <QuestionContent content={parsed?.body ?? ""} />
                            </div>

                            {isMastered && (
                              <div className="rounded-lg border border-green-500/40 bg-green-500/10 px-4 py-3 text-center">
                                <p className="text-base font-semibold text-green-300">🎉 정답! 마스터 완료</p>
                                <p className="mt-1 text-xs text-green-400/80">잠시 후 목록에서 사라집니다</p>
                              </div>
                            )}

                            {!isMastered && !state.result?.correct && (
                              <RetrySolverInline
                                detail={detail}
                                parsedOptions={parsed?.options ?? []}
                                state={state}
                                onChange={(patch) => setRetryField(wa.questionId, patch)}
                                onSubmit={() => handleSubmitRetry(wa.questionId)}
                              />
                            )}

                            {state.result && !state.result.correct && (
                              <div className="space-y-3">
                                <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3">
                                  <p className="text-sm font-semibold text-red-300">❌ 다시 도전!</p>
                                  {detail.questionType === "MCQ" ? (
                                    <p className="mt-1 text-sm text-red-200/90">
                                      정답: <span className="font-bold">{state.result.correctOption}번</span>
                                    </p>
                                  ) : (
                                    <p className="mt-1 text-sm text-red-200/90">
                                      모범답안:{" "}
                                      <span className="font-mono">{state.result.correctAnswer}</span>
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
                    isSelected ? "bg-amber-500/30 text-amber-100" : "bg-border text-muted"
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

      {state.error && <p className="text-xs text-red-400">{state.error}</p>}

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
