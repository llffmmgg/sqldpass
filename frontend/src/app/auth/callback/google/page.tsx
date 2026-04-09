"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { setAuth, setNickname as saveNickname } from "@/lib/auth";
import { fetchApi } from "@/lib/api";
import { updateNickname } from "@/lib/memberApi";
import { generateNickname } from "@/lib/nickname";
import { trackEvent, setUserProperties } from "@/lib/gtag";

interface LoginResponse {
  token: string;
  nickname: string;
  isNew: boolean;
}

// 신규 가입 시 랜덤 닉네임 자동 생성 + PATCH (유니크 충돌 시 최대 5회 재시도)
async function assignRandomNickname(): Promise<string> {
  for (let i = 0; i < 5; i++) {
    const candidate = generateNickname();
    try {
      const updated = await updateNickname(candidate);
      return updated.nickname;
    } catch (e) {
      // 409(중복)면 다음 후보로 재시도, 그 외는 throw
      if (e instanceof Error && e.message.includes("이미 사용 중")) {
        continue;
      }
      throw e;
    }
  }
  throw new Error("닉네임 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
}

function GoogleCallback() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const code = searchParams.get("code");
    if (!code) {
      setError("인증 코드가 없습니다.");
      return;
    }

    const redirectUri = `${window.location.origin}/auth/callback/google`;

    (async () => {
      try {
        const data = await fetchApi<LoginResponse>("/auth/login/google", {
          method: "POST",
          body: JSON.stringify({ code, redirectUri }),
        });

        // 토큰 먼저 저장 → 후속 PATCH 호출에 Authorization 헤더 사용
        setAuth(data.token, data.nickname);

        // GA4 — 회원가입/로그인 이벤트 + 사용자 속성
        setUserProperties({ plan_type: "free" });
        if (data.isNew) {
          trackEvent("sign_up", { method: "google" });
        }
        trackEvent("login", { method: "google" });

        // 신규 가입이면 랜덤 닉네임 생성해서 교체
        if (data.isNew) {
          try {
            const finalNickname = await assignRandomNickname();
            saveNickname(finalNickname);
          } catch (e) {
            // 닉네임 생성 실패해도 일단 로그인은 완료된 상태 (placeholder 닉네임 유지)
            console.error("자동 닉네임 생성 실패:", e);
          }
        }

        router.replace("/");
      } catch (e) {
        setError(e instanceof Error ? e.message : "로그인에 실패했습니다.");
      }
    })();
  }, [searchParams, router]);

  if (error) {
    return (
      <div className="text-center">
        <p className="text-red-400">{error}</p>
        <button
          onClick={() => router.replace("/")}
          className="mt-4 rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-zinc-900 transition-colors hover:bg-primary-hover"
        >
          홈으로 돌아가기
        </button>
      </div>
    );
  }

  return <p className="text-muted">로그인 처리 중...</p>;
}

export default function GoogleCallbackPage() {
  return (
    <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
      <Suspense fallback={<p className="text-muted">로딩 중...</p>}>
        <GoogleCallback />
      </Suspense>
    </main>
  );
}
