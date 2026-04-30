"use client";

import Link from "next/link";
import { getGoogleLoginUrl } from "@/lib/oauth";
import MascotImage from "@/components/mascot/MascotImage";

export default function LoginRequired() {
  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto flex max-w-3xl flex-col items-center px-4 pt-16 sm:flex-row sm:items-center sm:gap-10 sm:pt-24">
        <div className="shrink-0">
          <MascotImage pose="guide" size={200} priority />
        </div>

        <div className="flex flex-col items-center sm:items-start sm:text-left">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/25 bg-primary/10 px-3 py-1 text-[11px] font-medium text-primary">
            3초 가입 · 결제 없음
          </span>

          <h2 className="mt-4 text-center text-2xl font-bold tracking-tight sm:text-left sm:text-[28px]">
            점수 기록을 남기려면<br className="hidden sm:block" />
            <span className="sm:inline"> 로그인이 한 번 필요해요</span>
          </h2>

          <p className="mt-3 max-w-sm text-center text-sm leading-relaxed text-muted sm:text-left">
            오답 자동 복습과 회차별 점수 추이는 로그인된 세션에만 저장됩니다.
            구글 계정으로 한 번만 클릭해주세요.
          </p>

          <button
            onClick={() => {
              window.location.href = getGoogleLoginUrl();
            }}
            className="mt-6 inline-flex items-center gap-3 rounded-xl border border-border bg-white px-7 py-3 text-sm font-semibold text-zinc-800 shadow-sm transition-all duration-200 hover:shadow-lg hover:scale-[1.02] active:scale-[0.98]"
          >
            <svg className="h-5 w-5" viewBox="0 0 24 24">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
            </svg>
            Google 계정으로 계속하기
          </button>

          <p className="mt-3 max-w-sm text-center text-xs text-muted/70 sm:text-left">
            로그인 없이도 모의고사는 풀 수 있어요. 다만 결과가 저장되지 않습니다.
          </p>

          <Link
            href="/"
            className="mt-6 text-sm text-muted transition-colors hover:text-foreground"
          >
            ← 홈으로 돌아가기
          </Link>
        </div>
      </div>
    </main>
  );
}
