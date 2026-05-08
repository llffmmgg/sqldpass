"use client";

import { useState, useSyncExternalStore } from "react";
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
    label: "오류",
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
const DISMISS_EVENT = "feedback-rail-dismissed-changed";

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

function persistDismissed() {
  try {
    const until = Date.now() + 1000 * 60 * 60 * 24; // 24h
    window.localStorage.setItem(DISMISS_KEY, String(until));
    // 같은 탭에선 storage 이벤트가 안 뜨므로 커스텀 이벤트로 useSyncExternalStore 구독자에게 알림.
    window.dispatchEvent(new Event(DISMISS_EVENT));
  } catch {
    // ignore
  }
}

function subscribeDismissed(callback: () => void) {
  if (typeof window === "undefined") return () => {};
  window.addEventListener("storage", callback);
  window.addEventListener(DISMISS_EVENT, callback);
  return () => {
    window.removeEventListener("storage", callback);
    window.removeEventListener(DISMISS_EVENT, callback);
  };
}

function subscribeNoop() {
  return () => {};
}

export default function FeedbackRail() {
  const pathname = usePathname();
  // SSR에선 false → 클라이언트 hydration 직후 true. mounted 플래그 대신 사용.
  const isClient = useSyncExternalStore(
    subscribeNoop,
    () => true,
    () => false,
  );
  // localStorage(외부 store) 구독 — set-state-in-effect 회피.
  const dismissed = useSyncExternalStore(
    subscribeDismissed,
    () => isDismissed(),
    () => false,
  );
  const [modalOpen, setModalOpen] = useState(false);
  const [modalType, setModalType] = useState<FeedbackType>("OTHER");
  const [modalHint, setModalHint] = useState<string | undefined>(undefined);

  if (!isClient) return null;
  if (EXCLUDED_PREFIXES.some((prefix) => pathname?.startsWith(prefix))) {
    return null;
  }
  if (dismissed) return null;

  function handleReaction(r: Reaction) {
    const isQuestionPage = QUESTION_PATH_PREFIXES.some((p) => pathname?.startsWith(p));
    const resolvedType: FeedbackType =
      r.type === "QUESTION_OR_BUG" ? (isQuestionPage ? "QUESTION_ERROR" : "BUG") : r.type;
    setModalType(resolvedType);
    setModalHint(r.hint || undefined);
    setModalOpen(true);
  }

  function handleOpenFull() {
    setModalType("OTHER");
    setModalHint(undefined);
    setModalOpen(true);
  }

  function handleDismiss(e: React.MouseEvent) {
    e.stopPropagation();
    persistDismissed();
  }

  return (
    <>
      <aside
        role="complementary"
        aria-label="피드백"
        className="fixed left-4 top-28 z-30 hidden w-[136px] rounded-2xl border border-border bg-surface/95 p-3 shadow-xl backdrop-blur xl:block"
      >
        <div className="flex items-center justify-between">
          <p className="text-[11px] font-bold tracking-wide text-text">💬 의견</p>
          <button
            type="button"
            onClick={handleDismiss}
            aria-label="피드백 위젯 숨기기 (24시간)"
            className="rounded-md p-0.5 text-[10px] text-text-muted transition-colors hover:bg-border/40 hover:text-text"
          >
            ✕
          </button>
        </div>

        <div className="mt-2 grid grid-cols-2 gap-1.5">
          {REACTIONS.map((r) => (
            <button
              key={r.label}
              type="button"
              onClick={() => handleReaction(r)}
              aria-label={r.ariaLabel}
              className="group flex flex-col items-center gap-0.5 rounded-lg border border-border/60 bg-background/60 py-2 transition-all hover:-translate-y-0.5 hover:border-primary/60 hover:bg-primary/10 active:translate-y-0 active:scale-[0.97] focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/60"
            >
              <span className="text-lg leading-none transition-transform group-hover:scale-110">
                {r.emoji}
              </span>
              <span className="text-[10px] font-medium text-text-muted group-hover:text-text">
                {r.label}
              </span>
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={handleOpenFull}
          className="mt-2 flex w-full items-center justify-center gap-1 rounded-lg border border-primary/40 bg-primary/10 px-2 py-1.5 text-[11px] font-semibold text-primary transition-all hover:bg-primary/20 active:scale-[0.97] focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/60"
        >
          ✍ 자세히 쓰기
        </button>
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
