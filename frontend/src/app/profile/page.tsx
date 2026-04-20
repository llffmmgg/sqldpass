"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { isLoggedIn, setNickname as saveNickname, clearAuth } from "@/lib/auth";
import { getMe, updateNickname, withdrawMember, type MemberMe } from "@/lib/memberApi";
import { generateNickname } from "@/lib/nickname";
import LoginRequired from "@/components/LoginRequired";
import StreakBox from "@/components/StreakBox";

export default function ProfilePage() {
  const router = useRouter();
  const [me, setMe] = useState<MemberMe | null>(null);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [withdrawConfirm, setWithdrawConfirm] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);
  const [withdrawError, setWithdrawError] = useState<string | null>(null);

  const WITHDRAW_PHRASE = "탈퇴합니다";

  async function handleWithdraw() {
    if (withdrawConfirm.trim() !== WITHDRAW_PHRASE) {
      setWithdrawError(`"${WITHDRAW_PHRASE}" 를 정확히 입력해주세요.`);
      return;
    }
    setWithdrawing(true);
    setWithdrawError(null);
    try {
      await withdrawMember();
      clearAuth();
      router.replace("/");
    } catch (e) {
      setWithdrawError(e instanceof Error ? e.message : "탈퇴 처리에 실패했습니다.");
      setWithdrawing(false);
    }
  }

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

        <div className="mt-6">
          <StreakBox />
        </div>

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
                className="w-full rounded-lg bg-primary py-2 text-sm font-semibold text-primary-fg transition hover:bg-primary-hover disabled:opacity-50"
              >
                {loading ? "저장 중..." : "변경 저장"}
              </button>
            </form>
          </div>
        )}

        {me && (
          <div className="mt-8 rounded-xl border border-rose-500/20 bg-rose-500/5 p-6">
            <h2 className="text-sm font-semibold text-rose-300">위험 구역</h2>
            <p className="mt-1 text-xs text-muted">
              탈퇴 시 풀이 기록과 받은 알림이 모두 삭제되며 복구할 수 없습니다.
              작성한 건의사항은 운영을 위해 익명으로 보존됩니다.
            </p>
            <button
              type="button"
              onClick={() => {
                setWithdrawOpen(true);
                setWithdrawConfirm("");
                setWithdrawError(null);
              }}
              className="mt-4 rounded-lg border border-rose-500/40 bg-rose-500/10 px-4 py-2 text-sm font-medium text-rose-200 transition hover:bg-rose-500/20"
            >
              회원 탈퇴
            </button>
          </div>
        )}

        <button
          onClick={() => router.back()}
          className="mt-6 text-sm text-muted hover:text-foreground"
        >
          ← 돌아가기
        </button>
      </div>

      {withdrawOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
          <div className="mx-4 w-full max-w-md rounded-xl border border-border bg-background p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-foreground">회원 탈퇴</h3>
            <p className="mt-2 text-sm text-muted">
              이 작업은 되돌릴 수 없습니다. 풀이 기록·알림이 영구 삭제됩니다.
            </p>
            <p className="mt-4 text-xs text-muted">
              계속하려면 아래에 <span className="font-semibold text-rose-300">{WITHDRAW_PHRASE}</span> 를 입력하세요.
            </p>
            <input
              type="text"
              value={withdrawConfirm}
              onChange={(e) => {
                setWithdrawConfirm(e.target.value);
                setWithdrawError(null);
              }}
              disabled={withdrawing}
              autoFocus
              className="mt-2 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm focus:border-rose-500/60 focus:outline-none focus:ring-2 focus:ring-rose-500/30"
            />
            {withdrawError && (
              <p className="mt-2 text-xs text-rose-400">{withdrawError}</p>
            )}
            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setWithdrawOpen(false)}
                disabled={withdrawing}
                className="rounded-lg border border-border bg-surface px-4 py-2 text-sm text-muted transition hover:text-foreground disabled:opacity-50"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleWithdraw}
                disabled={withdrawing || withdrawConfirm.trim() !== WITHDRAW_PHRASE}
                className="rounded-lg bg-rose-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-600 disabled:opacity-50"
              >
                {withdrawing ? "처리 중…" : "탈퇴하기"}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
