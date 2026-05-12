"use client";

import { useEffect, useState } from "react";

import PageHeader from "@/components/admin/PageHeader";
import {
  getCheckoutOpenSetting,
  updateCheckoutOpenSetting,
} from "@/lib/adminApi";

export default function AdminSettingsPage() {
  const [openToAll, setOpenToAll] = useState<boolean | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [savedAt, setSavedAt] = useState<number | null>(null);

  useEffect(() => {
    getCheckoutOpenSetting()
      .then((r) => setOpenToAll(r.openToAll))
      .catch((e) => setError(e instanceof Error ? e.message : "설정을 불러오지 못했습니다."));
  }, []);

  async function toggle() {
    if (openToAll === null || saving) return;
    const next = !openToAll;
    setSaving(true);
    setError(null);
    try {
      const r = await updateCheckoutOpenSetting(next);
      setOpenToAll(r.openToAll);
      setSavedAt(Date.now());
    } catch (e) {
      setError(e instanceof Error ? e.message : "변경에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="런타임 설정"
        description="재배포 없이 즉시 적용되는 운영 토글. 변경 즉시 모든 사용자에게 반영됩니다."
      />

      <section className="rounded-xl border border-border bg-surface/60 p-5 sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <h2 className="text-base font-semibold text-foreground">결제창 전체 공개</h2>
            <p className="mt-1.5 text-sm leading-relaxed text-muted">
              ON: 모든 로그인 사용자에게 <code className="rounded bg-background/60 px-1 py-0.5 text-[12px]">/checkout</code> 노출.
              <br />
              OFF: <code className="rounded bg-background/60 px-1 py-0.5 text-[12px]">PAYMENT_REVIEWER_NICKNAMES</code> 환경변수에
              등록된 닉네임만 노출(베타 화이트리스트 모드).
            </p>
          </div>

          <div className="flex shrink-0 items-center gap-3">
            <span
              className={`text-xs font-semibold ${
                openToAll === null
                  ? "text-muted"
                  : openToAll
                    ? "text-emerald-400"
                    : "text-amber-400"
              }`}
            >
              {openToAll === null ? "확인 중…" : openToAll ? "ON · 전원 공개" : "OFF · 화이트리스트"}
            </span>
            <button
              type="button"
              role="switch"
              aria-checked={openToAll === true}
              disabled={openToAll === null || saving}
              onClick={toggle}
              className={`relative inline-flex h-7 w-12 shrink-0 items-center rounded-full border transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${
                openToAll
                  ? "border-emerald-500/50 bg-emerald-500/30"
                  : "border-border bg-background/60"
              }`}
            >
              <span
                aria-hidden
                className={`inline-block h-5 w-5 transform rounded-full bg-foreground shadow transition-transform ${
                  openToAll ? "translate-x-6" : "translate-x-1"
                }`}
              />
            </button>
          </div>
        </div>

        {error && (
          <p className="mt-4 rounded-md border border-red-500/30 bg-red-500/5 px-3 py-2 text-xs text-red-400">
            {error}
          </p>
        )}
        {savedAt && !error && (
          <p className="mt-4 text-xs text-emerald-400">저장되었습니다.</p>
        )}
      </section>
    </div>
  );
}
