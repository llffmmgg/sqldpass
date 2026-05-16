"use client";

import { useSyncExternalStore } from "react";
import { isLoggedIn, getNickname } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";
import { trackEvent } from "@/lib/gtag";
import { Button, ButtonLink } from "@/components/ui";

function subscribeAuth(callback: () => void) {
  if (typeof window === "undefined") return () => {};
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}

function subscribeNoop() {
  return () => {};
}

export default function HeroCta() {
  // SSR/hydration 시점엔 false, 클라이언트 mount 직후 true — useState + useEffect 마운트 플래그 대체.
  const isClient = useSyncExternalStore(
    subscribeNoop,
    () => true,
    () => false,
  );
  const loggedIn = useSyncExternalStore(
    subscribeAuth,
    () => isLoggedIn(),
    () => false,
  );
  const nickname = useSyncExternalStore<string | null>(
    subscribeAuth,
    () => getNickname(),
    () => null,
  );

  const authed = isClient && loggedIn;
  const primary = authed
    ? { href: "/solve", label: "이어서 문제 풀기" }
    : { href: "/solve", label: "무료로 시작하기" };

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
        {authed ? (
          <ButtonLink
            href="/dashboard"
            variant="outline"
            size="lg"
            onClick={() => trackEvent("click_cta", { cta: "secondary", label: "내 대시보드", authed })}
          >
            내 대시보드
          </ButtonLink>
        ) : (
          <Button
            variant="outline"
            size="lg"
            onClick={() => {
              trackEvent("click_cta", { cta: "secondary", label: "3초 로그인", authed });
              window.location.href = getGoogleLoginUrl();
            }}
          >
            3초 로그인
          </Button>
        )}
      </div>
    </div>
  );
}
