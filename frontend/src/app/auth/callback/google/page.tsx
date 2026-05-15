"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { setAuth, setNickname as saveNickname } from "@/lib/auth";
import { invalidateSubscriptionCache } from "@/hooks/useSubscription";
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
  const code = searchParams.get("code");
  const [error, setError] = useState<string | null>(null);

  // code 부재는 render 단계에서 직접 분기 — effect 안 sync setState 회피.
  // 실제 토큰 교환은 code가 있을 때만 effect로 진행 (cancelled 가드로 unmount 안전).
  useEffect(() => {
    if (!code) return;

    const redirectUri = `${window.location.origin}/auth/callback/google`;
    let cancelled = false;

    (async () => {
      try {
        const data = await fetchApi<LoginResponse>("/auth/login/google", {
          method: "POST",
          body: JSON.stringify({ code, redirectUri }),
        });
        if (cancelled) return;

        // 토큰 먼저 저장 → 후속 PATCH 호출에 Authorization 헤더 사용
        setAuth(data.token, data.nickname);
        // 이전 계정의 구독 캐시가 새 계정에 새지 않도록 초기화
        invalidateSubscriptionCache();

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
            if (cancelled) return;
            saveNickname(finalNickname);
          } catch (e) {
            // 닉네임 생성 실패해도 일단 로그인은 완료된 상태 (placeholder 닉네임 유지)
            console.error("자동 닉네임 생성 실패:", e);
          }
        }

        // 로그인 진입 직전 페이지에서 sessionStorage 에 postLoginRedirect 를 남겨
        // 두면 그곳으로 복귀. 그 외에는 홈으로. open-redirect 차단을 위해 내부
        // 경로(`/...`) 만 허용한다.
        let target = "/";
        try {
          const stored = sessionStorage.getItem("postLoginRedirect");
          if (stored && stored.startsWith("/") && !stored.startsWith("//")) {
            target = stored;
          }
          sessionStorage.removeItem("postLoginRedirect");
        } catch {
          // sessionStorage 사용 불가 환경 — 기본값 유지
        }
        router.replace(target);
      } catch (e) {
        if (cancelled) return;
        setError(e instanceof Error ? e.message : "로그인에 실패했습니다.");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [code, router]);

  const displayError = !code ? "인증 코드가 없습니다." : error;

  if (displayError) {
    return (
      <div className="text-center">
        <p className="text-red-400">{displayError}</p>
        <button
          onClick={() => router.replace("/")}
          className="mt-4 rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-primary-fg transition-colors hover:bg-primary-hover"
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
