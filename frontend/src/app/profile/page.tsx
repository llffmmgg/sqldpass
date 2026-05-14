"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { isLoggedIn, setNickname as saveNickname, clearAuth } from "@/lib/auth";
import { getMe, updateNickname, withdrawMember, type MemberMe } from "@/lib/memberApi";
import { generateNickname } from "@/lib/nickname";
import LoginRequired from "@/components/LoginRequired";
import { useSubscription } from "@/hooks/useSubscription";
import { Container } from "@/components/ui/Container";
import { getSolves, getWrongAnswerStats } from "@/lib/api";
import { getMyStreak, type Streak } from "@/lib/streakApi";
import type { SubscriptionPlan } from "@/lib/payment";

function planLabel(plan: SubscriptionPlan): string {
  switch (plan) {
    case "THREE_DAY":
      return "Thunder";
    case "FOCUS":
      return "Focus";
    case "ONE_MONTH":
      return "Pro";
    case "UNLIMITED":
      return "All Pass";
  }
}

/** 만료까지 남은 일수. UNLIMITED 는 null. */
function daysUntilExpiry(expiresAt: string | null): number | null {
  if (!expiresAt) return null;
  const ms = new Date(expiresAt).getTime() - Date.now();
  return Math.max(0, Math.ceil(ms / (1000 * 60 * 60 * 24)));
}

export default function ProfilePage() {
  const router = useRouter();
  const [me, setMe] = useState<MemberMe | null>(null);
  const [input, setInput] = useState("");
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [withdrawConfirm, setWithdrawConfirm] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);
  const [withdrawError, setWithdrawError] = useState<string | null>(null);
  const [stats, setStats] = useState<{
    totalSolved: number;
    totalCorrect: number;
    overallRate: number;
    wrongCount: number;
    streak: Streak | null;
  } | null>(null);
  const [statsLoading, setStatsLoading] = useState(true);
  const { subscription, loading: subLoading } = useSubscription();

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

  useEffect(() => {
    if (!isLoggedIn()) {
      setStatsLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const [solves, wrongStats, streak] = await Promise.all([
          getSolves(),
          getWrongAnswerStats(),
          getMyStreak().catch(() => null),
        ]);
        if (cancelled) return;
        const totalSolved = solves.reduce((acc, s) => acc + s.totalCount, 0);
        const totalCorrect = solves.reduce((acc, s) => acc + s.correctCount, 0);
        const overallRate =
          totalSolved > 0 ? Math.round((totalCorrect / totalSolved) * 100) : 0;
        const wrongCount = wrongStats.reduce((acc, w) => acc + (w.wrongCount ?? 0), 0);
        setStats({ totalSolved, totalCorrect, overallRate, wrongCount, streak });
      } catch {
        // 통계 페치 실패는 회원정보/구독에 영향 주지 않음 — KPI 만 fallback 으로 떨어진다.
      } finally {
        if (!cancelled) setStatsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (!authChecked) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-bg text-text">
        <p className="text-text-muted">로딩 중...</p>
      </main>
    );
  }

  if (!isLoggedIn()) {
    return <LoginRequired />;
  }

  function startEdit() {
    if (!me) return;
    setEditing(true);
    setInput(me.nickname);
    setError(null);
    setSuccess(null);
  }

  function cancelEdit() {
    setEditing(false);
    if (me) setInput(me.nickname);
    setError(null);
    setSuccess(null);
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
      setEditing(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : "변경에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-10 md:py-12">
        {/* ① 정체성 헤더 */}
        <header className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <p className="t-label text-text-subtle">프로필</p>
            <h1 className="t-h1 mt-1 truncate text-text">{me?.nickname ?? "—"}</h1>
            <p className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-text-muted">
              {me?.provider && (
                <span className="inline-flex items-center gap-1 rounded-md border border-border bg-bg-elevated px-1.5 py-0.5 font-medium">
                  {me.provider}
                </span>
              )}
              {me?.createdAt && (
                <span>
                  가입일 {new Date(me.createdAt).toLocaleDateString("ko-KR")}
                </span>
              )}
            </p>
          </div>
        </header>

        {/* 학습 스냅샷 */}
        <section className="mt-6 overflow-hidden rounded-lg border border-border bg-surface">
          <div className="flex items-center justify-between gap-3 border-b border-border bg-bg-elevated px-5 py-2.5">
            <span className="t-label text-text-subtle">내 학습</span>
            <Link
              href="/dashboard"
              className="text-xs text-text-muted transition-colors hover:text-text"
            >
              대시보드에서 자세히 →
            </Link>
          </div>
          <dl className="grid grid-cols-3 divide-x divide-border">
            <KpiCell
              label="총 풀이"
              value={stats?.totalSolved ?? null}
              suffix="문제"
              loading={statsLoading}
            />
            <KpiCell
              label="정답률"
              value={stats?.overallRate ?? null}
              suffix="%"
              loading={statsLoading}
              tone={rateTone(stats?.overallRate)}
            />
            <KpiCell
              label="연속 학습"
              value={stats?.streak?.currentStreak ?? null}
              suffix="일"
              loading={statsLoading}
            />
          </dl>
        </section>

        {/* ② 닉네임 카드 — 인라인 편집 */}
        {me && (
          <section className="mt-8 overflow-hidden rounded-lg border border-border bg-surface">
            <div className="flex items-center justify-between gap-3 border-b border-border bg-bg-elevated px-5 py-2.5">
              <span className="t-label text-text-subtle">닉네임</span>
              {!editing && (
                <button
                  type="button"
                  onClick={startEdit}
                  className="text-xs font-medium text-text-muted transition-colors hover:text-text"
                >
                  변경
                </button>
              )}
            </div>
            <div className="px-5 py-4">
              {!editing ? (
                <>
                  <p className="text-base font-medium text-text">{me.nickname}</p>
                  {success && <p className="mt-2 text-xs text-success">{success}</p>}
                </>
              ) : (
                <form onSubmit={handleSubmit} className="space-y-3">
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={input}
                      onChange={(e) => setInput(e.target.value)}
                      minLength={2}
                      maxLength={30}
                      autoFocus
                      className="flex-1 rounded-md border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                    />
                    <button
                      type="button"
                      onClick={handleRandomize}
                      className="rounded-md border border-border bg-bg px-3 py-2 text-sm text-text-muted transition-colors hover:border-border-strong hover:text-text"
                    >
                      🎲 랜덤
                    </button>
                  </div>
                  <p className="text-xs text-text-muted">2~30자, 다른 사용자와 중복 불가</p>
                  {error && <p className="text-sm text-danger">{error}</p>}
                  <div className="flex items-center justify-end gap-2">
                    <button
                      type="button"
                      onClick={cancelEdit}
                      disabled={loading}
                      className="rounded-md border border-border bg-bg px-3 py-2 text-sm text-text-muted transition-colors hover:text-text disabled:opacity-50"
                    >
                      취소
                    </button>
                    <button
                      type="submit"
                      disabled={loading || !input.trim() || input.trim() === me.nickname}
                      className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-fg transition-colors hover:bg-primary-hover disabled:opacity-50"
                    >
                      {loading ? "저장 중..." : "변경 저장"}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </section>
        )}

        {/* ③ 구독 카드 */}
        {me && !subLoading && <SubscriptionCard subscription={subscription} />}

        {/* ④ 빠른 링크 */}
        {me && (
          <section className="mt-8 overflow-hidden rounded-lg border border-border bg-surface">
            <div className="border-b border-border bg-bg-elevated px-5 py-2.5">
              <span className="t-label text-text-subtle">바로가기</span>
            </div>
            <ul className="divide-y divide-border">
              <QuickLinkRow
                href="/wrong-answers"
                label="오답 노트"
                hint="틀린 문제를 모아 복습"
                badge={
                  stats?.wrongCount && stats.wrongCount > 0
                    ? `${stats.wrongCount}개`
                    : undefined
                }
                badgeTone="danger"
              />
              <QuickLinkRow
                href="/dashboard"
                label="대시보드"
                hint="과목별 약점과 학습 추이"
              />
              <QuickLinkRow
                href="/checkout"
                label="결제·청구"
                hint="이용권 관리 및 영수증"
              />
            </ul>
          </section>
        )}

        {/* ⑤ 위험구역 — details 디스클로저 */}
        {me && (
          <details className="group mt-8 overflow-hidden rounded-lg border border-border bg-surface transition-colors open:border-danger/30 open:bg-danger/[0.03]">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-5 py-3 text-sm font-medium text-text-muted transition-colors hover:bg-surface-hover group-open:text-danger">
              <span className="inline-flex items-center gap-2">
                <span className="font-mono text-xs transition-transform duration-150 group-open:rotate-90">▸</span>
                계정 관리
              </span>
              <span className="t-label text-text-subtle group-open:hidden">위험 작업</span>
            </summary>
            <div className="border-t border-danger/20 bg-danger/[0.02] px-5 py-4">
              <h2 className="text-sm font-semibold text-danger">회원 탈퇴</h2>
              <p className="mt-1 text-xs text-text-muted">
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
                className="mt-4 rounded-md border border-danger/40 bg-danger/10 px-4 py-2 text-sm font-medium text-danger transition-colors hover:bg-danger/20"
              >
                회원 탈퇴
              </button>
            </div>
          </details>
        )}

        <div className="mt-8">
          <Link
            href="/dashboard"
            className="text-sm text-text-muted transition-colors hover:text-text"
          >
            ← 대시보드로
          </Link>
        </div>
      </Container>

      {/* 탈퇴 모달 */}
      {withdrawOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
          <div className="mx-4 w-full max-w-md rounded-xl border border-border bg-bg p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-text">회원 탈퇴</h3>
            <p className="mt-2 text-sm text-text-muted">
              이 작업은 되돌릴 수 없습니다. 풀이 기록·알림이 영구 삭제됩니다.
            </p>
            <p className="mt-4 text-xs text-text-muted">
              계속하려면 아래에 <span className="font-semibold text-danger">{WITHDRAW_PHRASE}</span> 를 입력하세요.
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
              className="mt-2 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-text focus:border-danger/60 focus:outline-none focus:ring-2 focus:ring-danger/30"
            />
            {withdrawError && (
              <p className="mt-2 text-xs text-danger">{withdrawError}</p>
            )}
            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setWithdrawOpen(false)}
                disabled={withdrawing}
                className="rounded-lg border border-border bg-surface px-4 py-2 text-sm text-text-muted transition-colors hover:text-text disabled:opacity-50"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleWithdraw}
                disabled={withdrawing || withdrawConfirm.trim() !== WITHDRAW_PHRASE}
                className="rounded-lg bg-danger px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-danger/90 disabled:opacity-50"
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

/* ─────────────────────────────────────────────────────────────
 * 구독 카드 — 활성 구독 정보 표시 + 업그레이드 동선
 * ───────────────────────────────────────────────────────────── */
function SubscriptionCard({ subscription }: { subscription: ReturnType<typeof useSubscription>["subscription"] }) {
  if (!subscription.active || !subscription.plan) {
    return <FreePlanCard />;
  }

  const remaining = daysUntilExpiry(subscription.expiresAt);
  const isExpiringSoon = remaining !== null && remaining <= 7;
  const isLifetime = subscription.expiresAt === null;

  return (
    <div className="mt-8 relative overflow-hidden rounded-xl border-2 border-primary/40 bg-gradient-to-br from-primary/[0.08] via-bg to-bg p-6 shadow-[0_0_40px_-15px_rgba(124,92,196,0.5)]">
      <div className="flex items-start justify-between gap-3">
        <div>
          <span className="inline-flex items-center gap-1.5 rounded-full bg-success/15 px-2.5 py-0.5 text-[10px] font-bold text-success">
            <span className="relative flex h-1.5 w-1.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-success opacity-60" />
              <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-success" />
            </span>
            이용 중
          </span>
          <h2 className="mt-2 text-2xl font-bold tracking-tight text-text">
            {planLabel(subscription.plan)}
          </h2>
          <p className="mt-1 text-xs text-text-muted">
            {isLifetime
              ? "무기한 이용 가능"
              : `만료까지 ${remaining}일 남음 · ${new Date(subscription.expiresAt!).toLocaleDateString("ko-KR")}`}
          </p>
          {isExpiringSoon && !isLifetime && (
            <span className="mt-2 inline-flex items-center gap-1 rounded-full border border-warning/40 bg-warning/[0.08] px-2 py-0.5 text-[10px] font-medium text-warning">
              ⚠ 곧 만료 — 미리 연장하세요
            </span>
          )}
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-1.5">
        <FeatureChip enabled label="고난이도 모의고사" />
        <FeatureChip enabled={subscription.removesAds} label="광고 제거" />
        <FeatureChip enabled={subscription.allowsPdf} label="PDF 다운로드" />
      </div>

      <Link
        href="/checkout"
        className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-primary/40 bg-primary/[0.08] px-4 py-2.5 text-sm font-semibold text-primary transition-all hover:border-primary/60 hover:bg-primary/[0.12]"
      >
        플랜 보러가기 →
      </Link>
    </div>
  );
}

function FreePlanCard() {
  return (
    <div className="mt-8 rounded-xl border border-border bg-surface p-6">
      <span className="inline-flex items-center rounded-full bg-surface-hover px-2.5 py-0.5 text-[10px] font-bold text-text-muted">
        무료
      </span>
      <h2 className="mt-2 text-xl font-bold tracking-tight text-text">무료 플랜 이용 중</h2>
      <p className="mt-2 text-sm leading-relaxed text-text-muted">
        쉬움/보통 모의고사·오답 노트·대시보드 등 기본 기능을 무료로 사용 중이에요.
        <br />
        고난이도 모의고사·광고 제거·PDF 다운로드는 유료 이용권에서 제공됩니다.
      </p>
      <Link
        href="/checkout"
        className="mt-5 inline-flex items-center gap-2 rounded-lg bg-gradient-to-r from-primary to-[#5ee0a5] px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-primary/20 transition-all hover:shadow-xl hover:shadow-primary/30 hover:-translate-y-0.5"
      >
        이용권 보러가기
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
        </svg>
      </Link>
    </div>
  );
}

function FeatureChip({ enabled, label }: { enabled: boolean; label: string }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[10px] font-medium ${
        enabled
          ? "border-success/40 bg-success/[0.08] text-success"
          : "border-border bg-surface text-text-subtle line-through"
      }`}
    >
      {enabled ? "✓" : "—"} {label}
    </span>
  );
}

/* ─────────────────────────────────────────────────────────────
 * 학습 스냅샷 + 빠른 링크 헬퍼
 * ───────────────────────────────────────────────────────────── */

function rateTone(rate: number | null | undefined): "success" | "warning" | "danger" | undefined {
  if (rate == null) return undefined;
  if (rate >= 80) return "success";
  if (rate >= 60) return "warning";
  return "danger";
}

function KpiCell({
  label,
  value,
  suffix,
  loading,
  tone,
}: {
  label: string;
  value: number | null;
  suffix: string;
  loading: boolean;
  tone?: "success" | "warning" | "danger";
}) {
  const toneCls =
    tone === "success"
      ? "text-success"
      : tone === "warning"
        ? "text-warning"
        : tone === "danger"
          ? "text-danger"
          : "text-text";
  return (
    <div className="px-3 py-4 text-center sm:px-4">
      <dt className="t-label text-text-subtle">{label}</dt>
      <dd className={`mt-1.5 font-mono text-xl font-bold tabular-nums sm:text-2xl ${toneCls}`}>
        {loading ? "—" : value ?? 0}
        <span className="ml-0.5 text-xs font-medium text-text-muted">{suffix}</span>
      </dd>
    </div>
  );
}

function QuickLinkRow({
  href,
  label,
  hint,
  badge,
  badgeTone,
}: {
  href: string;
  label: string;
  hint: string;
  badge?: string;
  badgeTone?: "danger" | "primary";
}) {
  const badgeCls =
    badgeTone === "danger"
      ? "border-danger/30 bg-danger/10 text-danger"
      : "border-primary/30 bg-primary/10 text-primary";
  return (
    <li>
      <Link
        href={href}
        className="flex items-center justify-between gap-3 px-5 py-3 transition-colors hover:bg-surface-hover"
      >
        <div className="min-w-0">
          <p className="text-sm font-medium text-text">{label}</p>
          <p className="mt-0.5 text-xs text-text-muted">{hint}</p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          {badge && (
            <span
              className={`rounded-md border px-1.5 py-0.5 text-[10px] font-semibold ${badgeCls}`}
            >
              {badge}
            </span>
          )}
          <span className="text-text-subtle">›</span>
        </div>
      </Link>
    </li>
  );
}
