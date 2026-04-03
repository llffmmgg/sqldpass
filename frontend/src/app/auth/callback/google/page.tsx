"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { setAuth } from "@/lib/auth";
import { fetchApi } from "@/lib/api";

interface LoginResponse {
  token: string;
  nickname: string;
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

    fetchApi<LoginResponse>("/auth/login/google", {
      method: "POST",
      body: JSON.stringify({ code, redirectUri }),
    })
      .then((data) => {
        setAuth(data.token, data.nickname);
        router.replace("/");
      })
      .catch((e) => {
        setError(e.message || "로그인에 실패했습니다.");
      });
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
