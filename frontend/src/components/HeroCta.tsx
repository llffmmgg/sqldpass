"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { isLoggedIn, getNickname } from "@/lib/auth";
import { trackEvent } from "@/lib/gtag";

/**
 * 랜딩 히어로 CTA — 로그인 상태에 따라 문구/링크 분기 + 닉네임 개인화 배지.
 *
 * - 로그아웃: "무료로 시작하기" / "문제 미리보기"
 * - 로그인: "이어서 문제 풀기" / "내 대시보드" + 재방문 웰컴 배지
 *
 * 하이드레이션 깜빡임 방지: mounted 플래그로 첫 페인트는 로그아웃 기본형 고정.
 */
export default function HeroCta() {
  const [mounted, setMounted] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);
  const [nickname, setNickname] = useState<string | null>(null);

  useEffect(() => {
    setLoggedIn(isLoggedIn());
    setNickname(getNickname());
    setMounted(true);
  }, []);

  const authed = mounted && loggedIn;

  const primary = authed
    ? { href: "/solve", label: "이어서 문제 풀기" }
    : { href: "/solve", label: "무료로 시작하기" };

  const secondary = authed
    ? { href: "/dashboard", label: "내 대시보드" }
    : { href: "#preview", label: "문제 미리보기" };

  return (
    <div className="flex flex-col items-center">
      {authed && (
        <p className="mb-4 text-sm text-muted">
          {nickname ? (
            <>
              다시 오셨군요, <span className="font-medium text-foreground">{nickname}</span> 님
            </>
          ) : (
            "다시 오셨군요"
          )}
        </p>
      )}

      <div className="flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
        <Link
          href={primary.href}
          onClick={() => trackEvent("click_cta", { cta: "primary", label: primary.label, authed })}
          className="inline-flex items-center rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-zinc-900 transition-colors hover:bg-primary-hover"
        >
          {primary.label}
          {authed && (
            <svg
              className="ml-1.5 h-4 w-4 transition-transform group-hover:translate-x-0.5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={2.5}
              aria-hidden="true"
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
            </svg>
          )}
        </Link>
        <Link
          href={secondary.href}
          onClick={() => trackEvent("click_cta", { cta: "secondary", label: secondary.label, authed })}
          className="inline-flex items-center rounded-lg border border-border px-6 py-3 text-sm font-semibold text-foreground transition-colors hover:bg-surface"
        >
          {secondary.label}
        </Link>
      </div>
    </div>
  );
}
