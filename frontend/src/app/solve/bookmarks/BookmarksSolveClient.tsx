"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import BookmarkButton from "@/components/BookmarkButton";
import QuestionContent from "@/components/QuestionContent";
import Spinner from "@/components/Spinner";
import { useToast } from "@/components/Toast";
import { Badge, Button, Card } from "@/components/ui";
import { isLoggedIn } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";
import { parseQuestion } from "@/lib/parseQuestion";
import {
  getBookmarks,
  getQuestionDetail,
  submitSolve,
  type BookmarkResponse,
  type QuestionDetail,
  type QuestionType,
} from "@/lib/api";
import { hapticError, hapticLight, hapticSuccess } from "@/lib/haptic";

type Phase = "pre" | "solve" | "done";

type PracticeQuestion = {
  questionId: number;
  questionType: QuestionType;
  body: string;
  options: string[];
  subjectName: string;
};

type PastEntry = {
  question: PracticeQuestion;
  selectedOption: number | null;
  answerText: string;
  detail: QuestionDetail;
  correct: boolean;
};

const SET_SIZE = 10;

function shuffle<T>(arr: T[]): T[] {
  const copy = arr.slice();
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

function toPracticeQuestion(b: BookmarkResponse): PracticeQuestion {
  const parsed = parseQuestion(b.questionContent);
  return {
    questionId: b.questionId,
    questionType: b.questionType,
    body: parsed.body,
    options: parsed.options,
    subjectName: b.subjectName,
  };
}

function isCorrect(
  q: PracticeQuestion,
  detail: QuestionDetail,
  selectedOption: number | null,
  answerText: string,
): boolean {
  if (q.questionType === "MCQ") {
    return selectedOption !== null && selectedOption === detail.correctOption;
  }
  const norm = (s: string) => s.trim().toLowerCase().replace(/\s+/g, " ");
  const submitted = norm(answerText);
  if (!submitted) return false;
  if (detail.answer && norm(detail.answer) === submitted) return true;
  return detail.keywords.some((k) => norm(k) === submitted);
}

export default function BookmarksSolveClient() {
  const toast = useToast();
  const [phase, setPhase] = useState<Phase>("pre");
  const [bookmarks, setBookmarks] = useState<BookmarkResponse[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [queue, setQueue] = useState<PracticeQuestion[]>([]);
  const [current, setCurrent] = useState<PracticeQuestion | null>(null);
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [answerText, setAnswerText] = useState("");
  const [detail, setDetail] = useState<QuestionDetail | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [pastEntries, setPastEntries] = useState<PastEntry[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isLoggedIn()) {
      // 즐겨찾기는 로그인 필수 — OAuth 로 보내고 복귀 경로 저장
      try {
        sessionStorage.setItem("postLoginRedirect", "/solve/bookmarks");
      } catch {
        // sessionStorage 사용 불가 환경 — 홈으로 복귀
      }
      window.location.href = getGoogleLoginUrl();
      return;
    }
    getBookmarks()
      .then((res) => setBookmarks(res.items))
      .catch((e) => setLoadError(e instanceof Error ? e.message : "즐겨찾기를 불러오지 못했어요."));
  }, []);

  const totalCorrect = useMemo(
    () => pastEntries.filter((e) => e.correct).length,
    [pastEntries],
  );

  function startSession() {
    if (!bookmarks || bookmarks.length === 0) return;
    const picked = shuffle(bookmarks).slice(0, SET_SIZE).map(toPracticeQuestion);
    setQueue(picked.slice(1));
    setCurrent(picked[0]);
    setSelectedOption(null);
    setAnswerText("");
    setDetail(null);
    setRevealed(false);
    setPastEntries([]);
    setPhase("solve");
  }

  function hasAnswer(): boolean {
    if (!current) return false;
    if (current.questionType === "MCQ") return selectedOption !== null;
    return answerText.trim().length > 0;
  }

  async function handleSubmit() {
    if (!current || revealed || !hasAnswer()) return;
    try {
      const d = await getQuestionDetail(current.questionId);
      setDetail(d);
      setRevealed(true);
      const ok = isCorrect(current, d, selectedOption, answerText);
      if (ok) hapticSuccess();
      else hapticError();
    } catch (e) {
      toast.show(
        e instanceof Error ? e.message : "정답 정보를 가져오지 못했어요.",
        "error",
      );
    }
  }

  async function handleNext() {
    if (!current || !detail) return;
    const ok = isCorrect(current, detail, selectedOption, answerText);
    const entry: PastEntry = {
      question: current,
      selectedOption,
      answerText,
      detail,
      correct: ok,
    };
    const nextPast = [...pastEntries, entry];
    setPastEntries(nextPast);

    if (queue.length === 0) {
      // 세션 종료 — submitSolve 로 풀이 기록 저장 (오답노트 자동 누적)
      await persistSession(nextPast);
      setPhase("done");
      return;
    }
    const [head, ...rest] = queue;
    setCurrent(head);
    setQueue(rest);
    setSelectedOption(null);
    setAnswerText("");
    setDetail(null);
    setRevealed(false);
  }

  async function persistSession(entries: PastEntry[]) {
    if (entries.length === 0) return;
    setSubmitting(true);
    try {
      const res = await submitSolve({
        source: "BOOKMARK",
        answers: entries.map((e) => ({
          questionId: e.question.questionId,
          selectedOption:
            e.question.questionType === "MCQ" ? e.selectedOption ?? undefined : undefined,
          answerText:
            e.question.questionType !== "MCQ" ? e.answerText || undefined : undefined,
        })),
      });
      if (res.milestoneReached) {
        toast.show(`🎉 ${res.milestoneReached}일 연속 학습! 잘하고 있어요`, "success");
      }
    } catch (e) {
      toast.show(
        e instanceof Error ? e.message : "풀이 기록 저장에 실패했어요.",
        "error",
      );
    } finally {
      setSubmitting(false);
    }
  }

  function newSession() {
    startSession();
  }

  // ──────────── 렌더링 ────────────

  if (loadError) {
    return (
      <Card className="mx-auto max-w-lg p-8 text-center">
        <h1 className="text-xl font-semibold">즐겨찾기를 불러오지 못했어요</h1>
        <p className="mt-3 text-sm text-text-muted">{loadError}</p>
        <Link
          href="/wrong-answers?tab=bookmark"
          className="mt-6 inline-block rounded-lg border border-border px-4 py-2 text-sm"
        >
          즐겨찾기로 →
        </Link>
      </Card>
    );
  }

  if (bookmarks === null) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <Spinner message="즐겨찾기 불러오는 중..." />
      </div>
    );
  }

  if (phase === "pre") {
    const count = bookmarks.length;
    const sessionSize = Math.min(count, SET_SIZE);
    return (
      <div className="mx-auto max-w-xl space-y-6">
        <div className="text-center">
          <Badge variant="soft" tone="info" size="sm">
            즐겨찾기 모아 풀기
          </Badge>
          <h1 className="mt-4 text-3xl font-bold tracking-tight">
            즐겨찾기 {count}개에서 {sessionSize}문제 랜덤
          </h1>
          <p className="mt-3 text-sm text-text-muted">
            매번 다른 순서·다른 조합으로 풀어요. 오답은 자동으로 오답노트에 들어갑니다.
          </p>
        </div>

        {count === 0 ? (
          <Card className="p-8 text-center">
            <p className="text-sm text-text-muted">
              아직 즐겨찾기 한 문제가 없어요. 풀이 중 별표를 눌러 즐겨찾기를 추가하면 여기서
              모아서 풀 수 있어요.
            </p>
            <div className="mt-6 flex flex-col items-center gap-3">
              <Link
                href="/solve"
                className="rounded-lg border border-border bg-surface px-5 py-2.5 text-sm hover:border-primary/40"
              >
                문제 풀기로 →
              </Link>
            </div>
          </Card>
        ) : (
          <div className="flex flex-col items-center gap-3">
            <Button onClick={startSession}>시작하기</Button>
            <Link
              href="/wrong-answers?tab=bookmark"
              className="text-xs text-text-subtle underline-offset-2 hover:underline"
            >
              즐겨찾기 목록 보기
            </Link>
          </div>
        )}
      </div>
    );
  }

  if (phase === "solve" && current) {
    const totalIndex = pastEntries.length + 1; // 1-based
    const totalSet = pastEntries.length + 1 + queue.length;
    const correctChoice = revealed && detail && current.questionType === "MCQ" ? detail.correctOption : null;
    const ok = revealed && detail ? isCorrect(current, detail, selectedOption, answerText) : null;

    return (
      <div className="mx-auto max-w-3xl space-y-6">
        <div className="flex items-center justify-between text-sm text-text-muted">
          <div className="flex items-center gap-2">
            <Badge variant="soft" tone="info" size="sm">
              {current.subjectName}
            </Badge>
            <span>
              {totalIndex} / {totalSet}
            </span>
          </div>
          <BookmarkButton questionId={current.questionId} initialBookmarked={true} />
        </div>

        <Card className="space-y-5 p-6">
          <QuestionContent content={current.body} />

          {current.questionType === "MCQ" ? (
            <div className="grid gap-2">
              {current.options.map((opt, idx) => {
                const optionNum = idx + 1;
                const isSelected = selectedOption === optionNum;
                const isCorrectChoice = correctChoice === optionNum;
                const showWrong = revealed && isSelected && !isCorrectChoice;
                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      if (revealed) return;
                      setSelectedOption(optionNum);
                      hapticLight();
                    }}
                    disabled={revealed}
                    className={[
                      "rounded-lg border px-4 py-3 text-left text-sm transition-colors",
                      isCorrectChoice
                        ? "border-emerald-500 bg-emerald-500/10"
                        : showWrong
                        ? "border-red-500 bg-red-500/10"
                        : isSelected
                        ? "border-primary bg-primary/10"
                        : "border-border bg-surface hover:border-primary/40",
                    ].join(" ")}
                  >
                    <span className="mr-2 font-semibold">{optionNum}.</span>
                    <span className="whitespace-pre-line">{opt}</span>
                  </button>
                );
              })}
            </div>
          ) : (
            <textarea
              value={answerText}
              onChange={(e) => setAnswerText(e.target.value)}
              disabled={revealed}
              placeholder="답을 입력하세요"
              className="min-h-[100px] w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm"
            />
          )}

          {revealed && detail && (
            <div className="rounded-lg border border-border bg-surface/40 p-4 text-sm">
              <div className="flex items-center gap-2">
                <Badge tone={ok ? "success" : "danger"} size="sm">
                  {ok ? "정답" : "오답"}
                </Badge>
                {current.questionType !== "MCQ" && detail.answer && (
                  <span className="text-text-muted">정답: {detail.answer}</span>
                )}
              </div>
              {detail.explanation && (
                <div className="mt-3 whitespace-pre-line leading-relaxed">
                  {detail.explanation}
                </div>
              )}
            </div>
          )}

          <div className="flex justify-end gap-2">
            {!revealed ? (
              <Button onClick={handleSubmit} disabled={!hasAnswer()}>
                제출
              </Button>
            ) : (
              <Button onClick={handleNext}>
                {queue.length === 0 ? "결과 보기" : "다음 문제"}
              </Button>
            )}
          </div>
        </Card>
      </div>
    );
  }

  // phase === "done"
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="text-center">
        <Badge tone="success" size="sm">
          세션 완료
        </Badge>
        <h1 className="mt-4 text-3xl font-bold tracking-tight">
          {totalCorrect} / {pastEntries.length} 맞췄어요
        </h1>
        <p className="mt-2 text-sm text-text-muted">
          {submitting
            ? "풀이 기록 저장 중..."
            : "오답은 오답노트에 자동으로 추가됐어요."}
        </p>
      </div>

      <div className="space-y-3">
        {pastEntries.map((e, idx) => (
          <Card key={`${e.question.questionId}-${idx}`} className="p-4">
            <div className="flex items-center gap-2 text-xs text-text-muted">
              <Badge tone={e.correct ? "success" : "danger"} size="sm">
                {e.correct ? "정답" : "오답"}
              </Badge>
              <span>{e.question.subjectName}</span>
              <span className="ml-auto">#{idx + 1}</span>
            </div>
            <div className="mt-3 text-sm">
              <QuestionContent content={e.question.body} />
            </div>
            {e.question.questionType === "MCQ" ? (
              <div className="mt-3 grid gap-1.5 text-sm">
                {e.question.options.map((opt, oi) => {
                  const num = oi + 1;
                  const isUser = e.selectedOption === num;
                  const isAnswer = e.detail.correctOption === num;
                  return (
                    <div
                      key={oi}
                      className={[
                        "rounded border px-3 py-2",
                        isAnswer
                          ? "border-emerald-500 bg-emerald-500/10"
                          : isUser
                          ? "border-red-500 bg-red-500/10"
                          : "border-border",
                      ].join(" ")}
                    >
                      <span className="mr-2 font-semibold">{num}.</span>
                      <span className="whitespace-pre-line">{opt}</span>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="mt-3 text-sm">
                <div>내 답: {e.answerText || <em className="text-text-muted">(미응답)</em>}</div>
                {e.detail.answer && <div>정답: {e.detail.answer}</div>}
              </div>
            )}
            {e.detail.explanation && (
              <div className="mt-3 rounded-lg border border-border bg-surface/40 p-3 text-sm whitespace-pre-line">
                {e.detail.explanation}
              </div>
            )}
          </Card>
        ))}
      </div>

      <div className="flex flex-wrap justify-center gap-3">
        <Button onClick={newSession}>새 조합으로 다시 풀기</Button>
        <Link
          href="/wrong-answers?tab=bookmark"
          className="rounded-lg border border-border bg-surface px-5 py-2.5 text-sm hover:border-primary/40"
        >
          즐겨찾기 목록
        </Link>
        <Link
          href="/wrong-answers"
          className="rounded-lg border border-border bg-surface px-5 py-2.5 text-sm hover:border-primary/40"
        >
          오답노트로
        </Link>
      </div>
    </div>
  );
}
