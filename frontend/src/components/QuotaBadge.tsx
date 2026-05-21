"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import { isLoggedIn } from "@/lib/auth";
import { fetchQuota, type Quota } from "@/lib/quotaApi";

function subscribeAuth(callback: () => void) {
  if (typeof window === "undefined") return () => {};
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}

// 헤더용 quota 표시 칩 — "오늘 18 / 30 문제" 형태.
//
// 표시 규칙:
// - 비로그인 → 표시 안 함 (서버가 어차피 limit=null 줌)
// - 서버가 준 `*Limit` 이 null → 활성 구독자/특수 흐름 → 숨김
// - 그 외 → "오늘 {used} / {limit} {kind}" 출력
//
// 클라이언트는 절대 자체 카운팅하지 않는다. 매 마운트마다 GET /api/quota 호출해서 최신값 사용.
//
// 색은 기존 토큰(zinc/border) 유지 — 옅은 배경 사용은 token 변경 없이 한도 근접 시
// warning 톤 1단계만 강하게 한다. 그 외엔 surface/text-muted 의 단단한 톤.

type Kind = "question" | "mock";

interface QuotaBadgeProps {
  kind: Kind;
  /**
   * SolveClient 처럼 비로그인 사용자에게 별도 한도 칩(anonQuota)을 이미 노출하는 화면이 있다.
   * 그런 곳에서는 이 컴포넌트를 마운트해도 비로그인 라인은 숨겨 중복을 피한다.
   * (기본 true — 비로그인은 항상 숨김. 별도로 켤 일이 생기면 false 로 호출.)
   */
  hideForAnonymous?: boolean;
  className?: string;
}

export default function QuotaBadge({
  kind,
  hideForAnonymous = true,
  className,
}: QuotaBadgeProps) {
  // 로그인 상태를 외부 store 로 구독 — SSR 단계에선 null, effect 안 setState 회피
  const loggedIn = useSyncExternalStore<boolean | null>(
    subscribeAuth,
    () => isLoggedIn(),
    () => null,
  );
  const [quota, setQuota] = useState<Quota | null>(null);

  useEffect(() => {
    if (loggedIn !== true) return;
    if (hideForAnonymous && !loggedIn) return;
    let alive = true;
    fetchQuota()
      .then((q) => {
        if (alive) setQuota(q);
      })
      .catch(() => {
        // quota 조회 실패는 UX 에 영향 없게 무시 — 한도 도달 시 서버 가드가 페이월을 띄움
      });
    return () => {
      alive = false;
    };
  }, [hideForAnonymous, kind, loggedIn]);

  // SSR / 로그인 상태 미확정
  if (loggedIn === null) return null;
  if (hideForAnonymous && loggedIn === false) return null;
  if (!quota) return null;

  const limit = kind === "question" ? quota.questionLimit : quota.mockLimit;
  // 활성 구독자(또는 정책상 무제한) — 서버가 null 로 알려줌
  if (limit === null || limit === undefined) return null;

  const used = kind === "question" ? quota.questionUsed : quota.mockUsed;
  const label = kind === "question" ? "문제" : "모의고사";
  const remaining = Math.max(limit - used, 0);
  const exhausted = used >= limit;
  // 잔량이 적을 때만 톤 변경 — 색 계열(amber/zinc) 변경은 없고 기존 warning/danger 토큰만 사용.
  const tone = exhausted
    ? "border-danger/40 bg-danger/10 text-danger"
    : remaining <= 3
      ? "border-warning/40 bg-warning/10 text-warning"
      : "border-border bg-surface text-text-muted";

  return (
    <span
      className={
        "inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-[11px] font-medium " +
        tone +
        (className ? ` ${className}` : "")
      }
      aria-label={`오늘 ${used} / ${limit} ${label}`}
    >
      <span className="tabular-nums">
        오늘 {used} / {limit} {label}
      </span>
      {exhausted && <span className="text-text-subtle">· 자정에 리셋</span>}
    </span>
  );
}
