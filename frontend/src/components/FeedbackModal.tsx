"use client";

import { useEffect, useState } from "react";
import { isLoggedIn } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";
import { submitFeedback, type FeedbackType } from "@/lib/feedbackApi";
import { Button } from "@/components/ui";

interface Props {
  open: boolean;
  onClose: () => void;
  defaultQuestionId?: number;
  defaultType?: FeedbackType;
}

const TYPE_OPTIONS: { value: FeedbackType; label: string }[] = [
  { value: "QUESTION_ERROR", label: "🐞 문제 오류" },
  { value: "BUG", label: "🛠 사이트 버그" },
  { value: "FEATURE", label: "💡 기능 제안" },
  { value: "OTHER", label: "💬 기타" },
];

export default function FeedbackModal({
  open,
  onClose,
  defaultQuestionId,
  defaultType,
}: Props) {
  const [type, setType] = useState<FeedbackType>(defaultType ?? (defaultQuestionId ? "QUESTION_ERROR" : "BUG"));
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loggedIn, setLoggedIn] = useState(true);

  // open될 때 상태 리셋 + 로그인 여부 확인
  useEffect(() => {
    if (open) {
      setType(defaultType ?? (defaultQuestionId ? "QUESTION_ERROR" : "BUG"));
      setContent("");
      setSubmitting(false);
      setSuccess(false);
      setError(null);
      setLoggedIn(isLoggedIn());
    }
  }, [open, defaultType, defaultQuestionId]);

  // ESC로 닫기 + body scroll lock
  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open, onClose]);

  if (!open) return null;

  async function handleSubmit() {
    if (submitting || success) return;
    if (content.trim().length < 5) {
      setError("5자 이상 작성해주세요.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await submitFeedback({
        type,
        questionId: defaultQuestionId ?? null,
        content: content.trim(),
        pageUrl: typeof window !== "undefined" ? window.location.pathname : null,
      });
      setSuccess(true);
      setTimeout(() => onClose(), 1800);
    } catch (e) {
      setError(e instanceof Error ? e.message : "제출에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  function handleLogin() {
    window.location.href = getGoogleLoginUrl();
  }

  return (
    <div
      className="fixed inset-0 z-[60] bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <div className="flex min-h-full items-center justify-center px-4 py-6 sm:py-8">
        <div
          className="max-h-[calc(100dvh-3rem)] w-full max-w-xl overflow-y-auto rounded-2xl border border-border bg-surface p-6 shadow-xl sm:max-h-[calc(100dvh-4rem)] sm:p-8"
          onClick={(e) => e.stopPropagation()}
        >
        {/* 헤더 */}
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold">💬 피드백 보내기</h2>
          <button
            onClick={onClose}
            className="text-muted hover:text-foreground"
            aria-label="닫기"
          >
            ✕
          </button>
        </div>

        {!loggedIn ? (
          <div className="mt-6 text-center">
            <p className="text-sm text-text-muted">피드백을 보내려면 로그인이 필요합니다.</p>
            <Button
              variant="outline"
              size="md"
              onClick={handleLogin}
              className="mt-4"
              leftIcon={
                <svg className="h-4 w-4" viewBox="0 0 24 24">
                  <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
                  <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                  <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                  <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                </svg>
              }
            >
              Google로 로그인
            </Button>
          </div>
        ) : success ? (
          <div className="mt-8 text-center">
            <p className="text-3xl">✅</p>
            <p className="mt-2 text-base font-medium">감사합니다!</p>
            <p className="mt-1 text-sm text-muted">빠르게 검토하고 반영할게요.</p>
          </div>
        ) : (
          <>
            {/* 관련 문제 배지 */}
            {defaultQuestionId && (
              <div className="mt-4 inline-flex items-center gap-1.5 rounded-full border border-amber-500/40 bg-amber-500/10 px-3 py-1 text-xs text-amber-300">
                <span>관련 문제</span>
                <span className="font-semibold">#{defaultQuestionId}</span>
              </div>
            )}

            {/* 타입 선택 */}
            <div className="mt-4">
              <p className="mb-2 text-xs font-medium text-muted">분류</p>
              <div className="grid grid-cols-2 gap-2">
                {TYPE_OPTIONS.map((opt) => {
                  const isActive = type === opt.value;
                  return (
                    <button
                      type="button"
                      key={opt.value}
                      onClick={() => setType(opt.value)}
                      className={`rounded-lg border px-3 py-2 text-sm font-medium transition ${
                        isActive
                          ? "border-primary/50 bg-primary/10 text-primary"
                          : "border-border text-text-muted hover:border-primary/30 hover:text-text"
                      }`}
                    >
                      {opt.label}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 내용 */}
            <div className="mt-5">
              <p className="mb-2 text-xs font-medium text-muted">내용</p>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={5}
                maxLength={2000}
                placeholder="자세한 내용을 적어주세요. 어떤 상황에서, 어떤 문제가 있었는지 알려주시면 큰 도움이 됩니다."
                className="block w-full resize-y rounded-lg border border-border bg-background px-4 py-3 text-sm leading-relaxed text-foreground placeholder:text-muted/70 focus:outline-none focus:ring-2 focus:ring-amber-500/60"
              />
              <div className="mt-1 flex items-center justify-between">
                <p className="text-[11px] text-muted/70">함께 만들어가는 문어CBT 💛</p>
                <p className="text-[11px] tabular-nums text-muted/60">{content.length} / 2000</p>
              </div>
            </div>

            {error && (
              <div className="mt-3 rounded-lg border border-red-500/40 bg-red-500/10 px-3 py-2 text-xs text-red-300">
                {error}
              </div>
            )}

            {/* 푸터 */}
            <div className="mt-5 flex justify-end gap-2">
              <Button variant="ghost" size="md" onClick={onClose} disabled={submitting}>
                취소
              </Button>
              <Button
                variant="primary"
                size="md"
                onClick={handleSubmit}
                disabled={submitting || content.trim().length < 5}
              >
                {submitting ? "보내는 중..." : "보내기"}
              </Button>
            </div>
          </>
        )}
        </div>
      </div>
    </div>
  );
}
