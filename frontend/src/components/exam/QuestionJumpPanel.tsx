"use client";

import { useEffect, useState } from "react";

export interface QuestionJumpGroup {
  /** 화면에 노출할 라벨 (예: "1과목 데이터 모델링") */
  label: string;
  /** 이 그룹에 속하는 문제 인덱스 (0-based). 띄엄띄엄도 가능. */
  indices: number[];
}

export interface QuestionJumpPanelProps {
  total: number;
  currentIdx: number;
  /** 응답 완료된 인덱스 집합 (0-based) */
  answered: Set<number>;
  /** 번호 클릭 시 호출. idx 는 0-based. */
  onJump: (idx: number) => void;
  /** 과목 경계 — 빈 배열이면 단일 그룹으로 표시 */
  groups: QuestionJumpGroup[];
  /** 자격증 accent — 선택된 칩 강조에 사용 */
  accent?: {
    bg: string;
    text: string;
    border: string;
  };
}

/**
 * 모의고사/기출 응시 화면용 문제 번호 빠른 이동 패널.
 *
 * - 데스크탑(lg+): 우측 sticky 사이드 패널
 * - 모바일(<lg): 우하단 floating 버튼 → 탭하면 bottom sheet drawer 로 펼침
 *   * 풀이 페이지의 "다음 문제" floating(bottom-6)과 겹치지 않게 bottom-24 위치
 */
export default function QuestionJumpPanel({
  total,
  currentIdx,
  answered,
  onJump,
  groups,
  accent,
}: QuestionJumpPanelProps) {
  const [drawerOpen, setDrawerOpen] = useState(false);

  // drawer 열렸을 때 body 스크롤 잠금
  useEffect(() => {
    if (!drawerOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [drawerOpen]);

  // groups 가 비어있으면 단일 그룹으로 처리 (모든 인덱스 한 그룹)
  const effectiveGroups: QuestionJumpGroup[] =
    groups.length > 0
      ? groups
      : [
          {
            label: "전체",
            indices: Array.from({ length: total }, (_, i) => i),
          },
        ];

  const answeredCount = answered.size;

  function handleJump(idx: number) {
    onJump(idx);
    setDrawerOpen(false);
  }

  return (
    <>
      {/* Desktop: 우측 sticky 사이드 */}
      <aside className="hidden lg:block">
        <div className="sticky top-20 w-44 max-h-[calc(100vh-6rem)] overflow-y-auto rounded-xl border border-border bg-surface/80 p-3 shadow-sm backdrop-blur">
          <p className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-text-muted">
            문제 이동 ({answeredCount}/{total})
          </p>
          <GroupedGrid
            groups={effectiveGroups}
            currentIdx={currentIdx}
            answered={answered}
            onJump={handleJump}
            accent={accent}
            single={groups.length === 0}
          />
        </div>
      </aside>

      {/* Mobile: floating 버튼 + drawer */}
      <button
        type="button"
        onClick={() => setDrawerOpen(true)}
        className={`fixed bottom-24 right-6 z-40 flex h-12 w-12 items-center justify-center rounded-full border-2 border-border bg-surface text-text shadow-lg backdrop-blur lg:hidden ${
          accent?.border ?? ""
        }`}
        aria-label={`문제 이동 (${answeredCount}/${total})`}
      >
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h7" />
        </svg>
        <span className="absolute -top-1.5 -right-1.5 rounded-full bg-primary px-1.5 py-px text-[10px] font-bold text-primary-fg">
          {answeredCount}/{total}
        </span>
      </button>

      {drawerOpen && (
        <div
          className="fixed inset-0 z-50 lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="문제 이동"
        >
          <div
            className="absolute inset-0 bg-black/40 backdrop-blur-sm"
            onClick={() => setDrawerOpen(false)}
          />
          <div className="absolute inset-x-0 bottom-0 max-h-[85vh] overflow-y-auto rounded-t-2xl border-t border-border bg-bg p-5 shadow-2xl">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-semibold text-text">
                문제 이동 ({answeredCount}/{total})
              </h2>
              <button
                type="button"
                onClick={() => setDrawerOpen(false)}
                className="rounded-md px-2 py-1 text-sm text-text-muted hover:bg-surface-hover"
                aria-label="닫기"
              >
                ✕
              </button>
            </div>
            <GroupedGrid
              groups={effectiveGroups}
              currentIdx={currentIdx}
              answered={answered}
              onJump={handleJump}
              accent={accent}
              single={groups.length === 0}
            />
          </div>
        </div>
      )}
    </>
  );
}

function GroupedGrid({
  groups,
  currentIdx,
  answered,
  onJump,
  accent,
  single,
}: {
  groups: QuestionJumpGroup[];
  currentIdx: number;
  answered: Set<number>;
  onJump: (idx: number) => void;
  accent?: { bg: string; text: string; border: string };
  single: boolean;
}) {
  return (
    <div className="space-y-3">
      {groups.map((group, i) => (
        <section key={`${group.label}-${i}`}>
          {!single && (
            <p className="mb-1.5 text-[10px] font-semibold uppercase tracking-wide text-text-muted">
              {group.label}
            </p>
          )}
          <div className="grid grid-cols-5 gap-1">
            {group.indices.map((idx) => {
              const isCurrent = idx === currentIdx;
              const isAnswered = answered.has(idx);
              const cls = isCurrent
                ? `border ${accent?.border ?? "border-primary"} ${accent?.bg ?? "bg-primary"} ${accent?.text ?? "text-primary-fg"} font-bold`
                : isAnswered
                  ? "border border-success/30 bg-success/15 text-success"
                  : "border border-border bg-surface text-text-muted hover:text-text hover:border-border-strong";
              return (
                <button
                  key={idx}
                  type="button"
                  onClick={() => onJump(idx)}
                  className={`flex aspect-square items-center justify-center rounded-md text-xs tabular-nums transition-colors ${cls}`}
                  aria-label={`${idx + 1}번 문제로 이동${isAnswered ? " (응답 완료)" : ""}${isCurrent ? " (현재)" : ""}`}
                  aria-current={isCurrent ? "step" : undefined}
                >
                  {idx + 1}
                </button>
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}
