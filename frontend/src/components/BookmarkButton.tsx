"use client";

import { useEffect, useState } from "react";
import { isLoggedIn } from "@/lib/auth";
import { addBookmark, removeBookmark, checkBookmark } from "@/lib/api";

interface Props {
  questionId: number;
  size?: "sm" | "md";
  /** 상위에서 이미 상태를 알고 있으면 초기 fetch 건너뜀 */
  initialBookmarked?: boolean;
  /** 토글이 서버에 반영된 직후 호출. 부모 리스트 동기화용. */
  onChange?: (next: boolean) => void;
}

/**
 * 문제 즐겨찾기 토글 버튼.
 * - 마운트 시 checkBookmark 1회 호출로 상태 로드 (initialBookmarked가 있으면 생략).
 * - 비로그인 상태에서 클릭 시 alert 안내 (별도 모달 없이 간단히).
 */
export default function BookmarkButton({
  questionId,
  size = "sm",
  initialBookmarked,
  onChange,
}: Props) {
  const [bookmarked, setBookmarked] = useState<boolean | null>(
    initialBookmarked ?? null,
  );
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (initialBookmarked !== undefined) {
      setBookmarked(initialBookmarked);
      return;
    }
    if (!isLoggedIn()) {
      setBookmarked(false);
      return;
    }
    let cancelled = false;
    checkBookmark(questionId)
      .then((r) => {
        if (!cancelled) setBookmarked(r.bookmarked);
      })
      .catch(() => {
        if (!cancelled) setBookmarked(false);
      });
    return () => {
      cancelled = true;
    };
  }, [questionId, initialBookmarked]);

  async function onToggle() {
    if (!isLoggedIn()) {
      alert("로그인 후 이용 가능합니다.");
      return;
    }
    if (loading || bookmarked === null) return;
    setLoading(true);
    const next = !bookmarked;
    // optimistic update
    setBookmarked(next);
    try {
      if (next) {
        await addBookmark(questionId);
      } else {
        await removeBookmark(questionId);
      }
      onChange?.(next);
    } catch (e) {
      setBookmarked(!next);
      console.error("bookmark toggle failed", e);
    } finally {
      setLoading(false);
    }
  }

  const isActive = bookmarked === true;
  const iconSize = size === "sm" ? "h-4 w-4" : "h-4 w-4";
  const baseCls = "text-text-subtle hover:text-primary";
  const activeCls = "text-primary hover:text-primary-hover";

  return (
    <button
      type="button"
      onClick={onToggle}
      disabled={loading || bookmarked === null}
      aria-pressed={isActive}
      aria-label={isActive ? "즐겨찾기 해제" : "즐겨찾기"}
      className={`inline-flex h-7 w-7 items-center justify-center rounded-md transition-colors hover:bg-surface-hover disabled:opacity-60 ${
        isActive ? activeCls : baseCls
      }`}
      title={isActive ? "즐겨찾기 해제" : "즐겨찾기"}
    >
      {/* Star icon: filled when active, outline when not */}
      {isActive ? (
        <svg className={iconSize} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M12 2.5l2.9 5.88 6.48.94-4.69 4.57 1.11 6.46L12 17.3l-5.8 3.05 1.11-6.46L2.62 9.32l6.48-.94L12 2.5z" />
        </svg>
      ) : (
        <svg
          className={iconSize}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth={1.8}
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M12 2.5l2.9 5.88 6.48.94-4.69 4.57 1.11 6.46L12 17.3l-5.8 3.05 1.11-6.46L2.62 9.32l6.48-.94L12 2.5z" />
        </svg>
      )}
    </button>
  );
}
