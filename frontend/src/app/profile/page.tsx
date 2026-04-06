"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { isLoggedIn, setNickname as saveNickname } from "@/lib/auth";
import { getMe, updateNickname, type MemberMe } from "@/lib/memberApi";
import { generateNickname } from "@/lib/nickname";
import LoginRequired from "@/components/LoginRequired";

export default function ProfilePage() {
  const router = useRouter();
  const [me, setMe] = useState<MemberMe | null>(null);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    if (!isLoggedIn()) {
      setAuthChecked(true);
      return;
    }
    getMe()
      .then((data) => {
        setMe(data);
        setInput(data.nickname);
      })
      .catch((e) => setError(e instanceof Error ? e.message : "회원 정보를 불러올 수 없습니다."))
      .finally(() => setAuthChecked(true));
  }, []);

  if (!authChecked) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  if (!isLoggedIn()) {
    return <LoginRequired />;
  }

  function handleRandomize() {
    setInput(generateNickname());
    setSuccess(null);
    setError(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!input.trim() || input.trim() === me?.nickname) return;

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const updated = await updateNickname(input.trim());
      setMe(updated);
      saveNickname(updated.nickname);
      setSuccess("닉네임이 변경되었습니다.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "변경에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-xl px-4 py-12">
        <h1 className="text-2xl font-bold">프로필</h1>

        {me && (
          <div className="mt-8 rounded-xl border border-border bg-surface p-6">
            <div className="space-y-2 text-sm text-muted">
              <p>
                <span className="text-foreground/60">제공자:</span> {me.provider}
              </p>
              <p>
                <span className="text-foreground/60">가입일:</span>{" "}
                {new Date(me.createdAt).toLocaleDateString("ko-KR")}
              </p>
            </div>

            <form onSubmit={handleSubmit} className="mt-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-foreground">
                  닉네임
                </label>
                <div className="mt-2 flex gap-2">
                  <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    minLength={2}
                    maxLength={30}
                    className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                  />
                  <button
                    type="button"
                    onClick={handleRandomize}
                    className="rounded-lg border border-border bg-background px-3 py-2 text-sm text-muted transition hover:text-foreground"
                  >
                    🎲 랜덤
                  </button>
                </div>
                <p className="mt-1 text-xs text-muted">2~30자, 다른 사용자와 중복 불가</p>
              </div>

              {error && <p className="text-sm text-red-400">{error}</p>}
              {success && <p className="text-sm text-green-400">{success}</p>}

              <button
                type="submit"
                disabled={loading || !input.trim() || input.trim() === me.nickname}
                className="w-full rounded-lg bg-primary py-2 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover disabled:opacity-50"
              >
                {loading ? "저장 중..." : "변경 저장"}
              </button>
            </form>
          </div>
        )}

        <button
          onClick={() => router.back()}
          className="mt-6 text-sm text-muted hover:text-foreground"
        >
          ← 돌아가기
        </button>
      </div>
    </main>
  );
}
