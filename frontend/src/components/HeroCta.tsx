"use client";

import { useEffect, useState } from "react";
import { isLoggedIn, getNickname } from "@/lib/auth";
import { trackEvent } from "@/lib/gtag";
import { ButtonLink } from "@/components/ui";

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
        <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-primary/25 bg-primary/10 px-3.5 py-1.5 text-xs text-text backdrop-blur-sm">
          <span className="relative flex h-1.5 w-1.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-60" />
            <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-primary" />
          </span>
          <span className="font-medium tracking-tight">
            {nickname ? (
              <>
                다시 오셨군요, <span className="font-bold text-primary">{nickname}</span> 님
              </>
            ) : (
              "다시 오셨군요"
            )}
          </span>
        </div>
      )}

      <div className="flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
        <ButtonLink
          href={primary.href}
          variant="primary"
          size="lg"
          glow
          onClick={() => trackEvent("click_cta", { cta: "primary", label: primary.label, authed })}
          rightIcon={
            authed ? (
              <svg
                className="h-4 w-4"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2.5}
                aria-hidden="true"
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
              </svg>
            ) : undefined
          }
        >
          {primary.label}
        </ButtonLink>
        <ButtonLink
          href={secondary.href}
          variant="outline"
          size="lg"
          onClick={() => trackEvent("click_cta", { cta: "secondary", label: secondary.label, authed })}
        >
          {secondary.label}
        </ButtonLink>
      </div>
    </div>
  );
}
