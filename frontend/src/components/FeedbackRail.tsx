"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import FeedbackModal from "@/components/FeedbackModal";
import type { FeedbackType } from "@/lib/feedbackApi";

const EXCLUDED_PREFIXES = [
  "/admin",
  "/profile",
  "/mypage/feedback",
  "/auth/callback",
];

const QUESTION_PATH_PREFIXES = ["/solve", "/mock-exams", "/past-exams"];

type Reaction = {
  emoji: string;
  label: string;
  ariaLabel: string;
  type: FeedbackType | "QUESTION_OR_BUG";
  hint: string;
};

const REACTIONS: Reaction[] = [
  {
    emoji: "😊",
    label: "좋아요",
    ariaLabel: "좋아요 피드백 남기기",
    type: "FEATURE",
    hint: "이런 점이 좋아요:\n",
  },
  {
    emoji: "😕",
    label: "아쉬워요",
    ariaLabel: "아쉬운 점 피드백 남기기",
    type: "OTHER",
    hint: "이런 점이 아쉬워요:\n",
  },
  {
    emoji: "🐛",
    label: "오류 신고",
    ariaLabel: "오류 신고하기",
    type: "QUESTION_OR_BUG",
    hint: "",
  },
  {
    emoji: "💡",
    label: "아이디어",
    ariaLabel: "기능 아이디어 제안하기",
    type: "FEATURE",
    hint: "이런 기능이 있으면 좋겠어요:\n",
  },
];

const DISMISS_KEY = "feedback_rail_dismissed_until";

function isDismissed(): boolean {
  if (typeof window === "undefined") return false;
  try {
    const until = window.localStorage.getItem(DISMISS_KEY);
    if (!until) return false;
    const ts = Number(until);
    if (Number.isNaN(ts)) return false;
    return Date.now() < ts;
  } catch {
    return false;
  }
}

function setDismissed() {
  try {
    const until = Date.now() + 1000 * 60 * 60 * 24; // 24h
    window.localStorage.setItem(DISMISS_KEY, String(until));
  } catch {
    // ignore
  }
}

export default function FeedbackRail() {
  const pathname = usePathname();
  const [mounted, setMounted] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const [pinned, setPinned] = useState(false);
  const [dismissed, setDismissedState] = useState(false);
  const [pulsed, setPulsed] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [modalType, setModalType] = useState<FeedbackType>("OTHER");
  const [modalHint, setModalHint] = useState<string | undefined>(undefined);
  const hoverCloseTimer = useRef<number | null>(null);

  // 마운트 후 dismissed 상태 로드 (SSR/하이드레이션 안전)
  useEffect(() => {
    setMounted(true);
    setDismissedState(isDismissed());
  }, []);

  // ESC 로 확장 접기
  useEffect(() => {
    if (!expanded) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setExpanded(false);
        setPinned(false);
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [expanded]);

  // 핀 상태일 때 바깥 클릭으로 접기
  useEffect(() => {
    if (!pinned) return;
    function onDown(e: MouseEvent) {
      const target = e.target as HTMLElement | null;
      if (!target) return;
      if (target.closest("[data-feedback-rail]")) return;
      setPinned(false);
      setExpanded(false);
    }
    window.addEventListener("mousedown", onDown);
    return () => window.removeEventListener("mousedown", onDown);
  }, [pinned]);

  if (!mounted) return null;
  if (EXCLUDED_PREFIXES.some((prefix) => pathname?.startsWith(prefix))) {
    return null;
  }
  if (dismissed) return null;

  function handleEnter() {
    if (hoverCloseTimer.current) {
      window.clearTimeout(hoverCloseTimer.current);
      hoverCloseTimer.current = null;
    }
    setExpanded(true);
    setPulsed(false);
  }

  function handleLeave() {
    if (pinned) return;
    // 약간의 딜레이로 마우스가 이모지 사이를 이동할 때 깜빡이지 않도록
    hoverCloseTimer.current = window.setTimeout(() => {
      setExpanded(false);
    }, 180);
  }

  function handleToggleClick() {
    setPulsed(false);
    if (pinned) {
      setPinned(false);
      setExpanded(false);
    } else {
      setPinned(true);
      setExpanded(true);
    }
  }

  function handleReaction(r: Reaction) {
    const isQuestionPage = QUESTION_PATH_PREFIXES.some((p) => pathname?.startsWith(p));
    const resolvedType: FeedbackType =
      r.type === "QUESTION_OR_BUG" ? (isQuestionPage ? "QUESTION_ERROR" : "BUG") : r.type;
    setModalType(resolvedType);
    setModalHint(r.hint || undefined);
    setModalOpen(true);
    setPinned(false);
    setExpanded(false);
  }

  function handleOpenFull() {
    setModalType("OTHER");
    setModalHint(undefined);
    setModalOpen(true);
    setPinned(false);
    setExpanded(false);
  }

  function handleDismiss(e: React.MouseEvent) {
    e.stopPropagation();
    setDismissed();
    setDismissedState(true);
  }

  return (
    <>
      <aside
        data-feedback-rail
        role="complementary"
        aria-label="피드백"
        className="fixed left-4 top-1/2 z-30 hidden -translate-y-1/2 xl:block"
        onMouseEnter={handleEnter}
        onMouseLeave={handleLeave}
      >
        <div className="relative flex items-start gap-2">
          {/* 세로 필 (항상 보임) */}
          <button
            type="button"
            onClick={handleToggleClick}
            aria-expanded={expanded}
            aria-controls="feedback-rail-card"
            aria-label="피드백 패널 열기"
            className={`group relative flex h-[104px] w-10 flex-col items-center justify-center gap-1.5 rounded-full border border-border bg-surface/90 text-text-muted shadow-lg backdrop-blur transition-all hover:border-primary/60 hover:bg-primary/10 hover:text-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 ${
              pulsed ? "btn-glow" : ""
            }`}
          >
            <span aria-hidden className="text-base leading-none">💬</span>
            <span
              aria-hidden
              className="text-[10px] font-bold leading-none tracking-[0.2em]"
              style={{
                writingMode: "vertical-rl",
                textOrientation: "upright",
              }}
            >
              의견
            </span>
          </button>

          {/* 확장 카드 */}
          {expanded && (
            <div
              id="feedback-rail-card"
              className="w-64 rounded-2xl border border-border bg-surface/95 p-4 shadow-2xl backdrop-blur"
              style={{ animation: "dropdown-enter 0.18s ease-out" }}
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="text-sm font-bold text-text">이 페이지 어떠세요?</p>
                  <p className="mt-0.5 text-[11px] text-text-muted">
                    한 번에 기록해 주시면 큰 힘이 돼요 💛
                  </p>
                </div>
                <button
                  type="button"
                  onClick={handleDismiss}
                  className="shrink-0 rounded-md p-1 text-text-muted transition-colors hover:bg-border/40 hover:text-text"
                  aria-label="피드백 위젯 닫기 (24시간)"
                >
                  ✕
                </button>
              </div>

              <div className="mt-3 grid grid-cols-4 gap-1.5">
                {REACTIONS.map((r) => (
                  <button
                    key={r.label}
                    type="button"
                    onClick={() => handleReaction(r)}
                    aria-label={r.ariaLabel}
                    className="flex flex-col items-center gap-1 rounded-xl border border-border/60 bg-background/60 px-1 py-2 transition-all hover:-translate-y-0.5 hover:border-primary/60 hover:bg-primary/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/60"
                  >
                    <span className="text-xl leading-none">{r.emoji}</span>
                    <span className="text-[10px] font-medium text-text-muted">{r.label}</span>
                  </button>
                ))}
              </div>

              <button
                type="button"
                onClick={handleOpenFull}
                className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-lg border border-primary/40 bg-primary/10 px-3 py-2 text-xs font-semibold text-primary transition-colors hover:bg-primary/20 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/60"
              >
                ✍ 자세한 의견 남기기
              </button>
            </div>
          )}
        </div>
      </aside>

      <FeedbackModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        defaultType={modalType}
        defaultContentHint={modalHint}
      />
    </>
  );
}
