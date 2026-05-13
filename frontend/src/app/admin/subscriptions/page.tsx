"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 어드민 페이지의 fetch → setState 흐름 */

import { useEffect, useState } from "react";
import {
  expireSubscription,
  grantSubscription,
  listSubscriptions,
  type AdminSubscription,
  type AdminSubscriptionPage,
  type AdminSubscriptionPlan,
} from "@/lib/adminApi";
import PageHeader from "@/components/admin/PageHeader";
import DataTable, { TableSkeleton } from "@/components/admin/DataTable";
import EmptyState from "@/components/admin/EmptyState";

const PLAN_LABEL: Record<AdminSubscriptionPlan, string> = {
  THREE_DAY: "Thunder",
  FOCUS: "Focus",
  ONE_MONTH: "Pro",
  UNLIMITED: "Lifetime",
};

const PLAN_CHIP: Record<AdminSubscriptionPlan, string> = {
  THREE_DAY: "border-amber-500/40 bg-amber-500/10 text-amber-300",
  FOCUS: "border-sky-500/40 bg-sky-500/10 text-sky-300",
  ONE_MONTH: "border-violet-500/40 bg-violet-500/10 text-violet-300",
  UNLIMITED: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
};

export default function AdminSubscriptionsPage() {
  const [data, setData] = useState<AdminSubscriptionPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [grantOpen, setGrantOpen] = useState(false);

  function reload() {
    setLoading(true);
    listSubscriptions({ nickname: search.trim() || undefined, page, size: 30 })
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }

  useEffect(reload, [page, search]);

  return (
    <div>
      <PageHeader
        title="구독 관리"
        description="회원의 활성/만료 구독을 조회하고, 운영 보상이나 환불 후 재발급은 수동으로 처리합니다."
        actions={
          <button
            onClick={() => setGrantOpen(true)}
            className="rounded-lg bg-violet-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-violet-600"
          >
            + 수동 발급
          </button>
        }
      />

      <div className="mb-4 flex items-center gap-2">
        <input
          type="text"
          placeholder="닉네임 검색…"
          value={search}
          onChange={(e) => {
            setPage(0);
            setSearch(e.target.value);
          }}
          className="w-64 rounded-lg border border-border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
        />
      </div>

      {loading ? (
        <DataTable>
          <DataTable.Head>
            <DataTable.HeadCell>닉네임</DataTable.HeadCell>
            <DataTable.HeadCell>Plan</DataTable.HeadCell>
            <DataTable.HeadCell align="right">결제일</DataTable.HeadCell>
            <DataTable.HeadCell align="right">만료일</DataTable.HeadCell>
            <DataTable.HeadCell align="right">상태</DataTable.HeadCell>
            <DataTable.HeadCell align="right">액션</DataTable.HeadCell>
          </DataTable.Head>
          <TableSkeleton cols={6} rows={6} />
        </DataTable>
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title="조회된 구독이 없어요"
          description={search ? `"${search}" 닉네임 검색 결과 없음` : "아직 결제된 구독이 없습니다."}
        />
      ) : (
        <>
          <DataTable maxHeight="640px">
            <DataTable.Head>
              <DataTable.HeadCell>닉네임</DataTable.HeadCell>
              <DataTable.HeadCell>Plan</DataTable.HeadCell>
              <DataTable.HeadCell align="right">결제일</DataTable.HeadCell>
              <DataTable.HeadCell align="right">만료일</DataTable.HeadCell>
              <DataTable.HeadCell align="right">상태</DataTable.HeadCell>
              <DataTable.HeadCell align="right">액션</DataTable.HeadCell>
            </DataTable.Head>
            <tbody>
              {data.content.map((s) => (
                <SubscriptionRow key={s.id} sub={s} onChanged={reload} />
              ))}
            </tbody>
          </DataTable>

          {data.totalPages > 1 && (
            <div className="mt-4 flex items-center justify-center gap-2 text-sm">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded border border-border bg-surface px-3 py-1.5 disabled:opacity-40"
              >
                ← 이전
              </button>
              <span className="text-muted">
                {page + 1} / {data.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                disabled={page >= data.totalPages - 1}
                className="rounded border border-border bg-surface px-3 py-1.5 disabled:opacity-40"
              >
                다음 →
              </button>
            </div>
          )}
        </>
      )}

      {grantOpen && <GrantModal onClose={() => setGrantOpen(false)} onDone={reload} />}
    </div>
  );
}

function SubscriptionRow({ sub, onChanged }: { sub: AdminSubscription; onChanged: () => void }) {
  const [busy, setBusy] = useState(false);

  async function handleExpire() {
    const reason = prompt(`구독 #${sub.id} (${sub.nickname}) 을 만료 처리합니다. 사유를 입력하세요:`);
    if (!reason || !reason.trim()) return;
    if (!confirm("정말 만료 처리하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) return;

    setBusy(true);
    try {
      await expireSubscription(sub.id, reason.trim());
      onChanged();
    } catch (e) {
      alert(e instanceof Error ? e.message : "만료 처리 실패");
    } finally {
      setBusy(false);
    }
  }

  return (
    <DataTable.Row>
      <DataTable.Cell className="font-medium">{sub.nickname}</DataTable.Cell>
      <DataTable.Cell>
        <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-bold ${PLAN_CHIP[sub.plan]}`}>
          {PLAN_LABEL[sub.plan]}
        </span>
      </DataTable.Cell>
      <DataTable.Cell align="right" mono className="text-muted">
        {new Date(sub.purchasedAt).toLocaleDateString("ko-KR")}
      </DataTable.Cell>
      <DataTable.Cell align="right" mono className="text-muted">
        {sub.expiresAt ? new Date(sub.expiresAt).toLocaleDateString("ko-KR") : "평생"}
      </DataTable.Cell>
      <DataTable.Cell align="right">
        {sub.active ? (
          <span className="inline-flex items-center rounded-full bg-success/15 px-2 py-0.5 text-[10px] font-bold text-success">
            활성
          </span>
        ) : (
          <span className="inline-flex items-center rounded-full bg-surface-hover px-2 py-0.5 text-[10px] font-bold text-muted">
            만료
          </span>
        )}
      </DataTable.Cell>
      <DataTable.Cell align="right">
        {sub.active && (
          <button
            onClick={handleExpire}
            disabled={busy}
            className="rounded border border-rose-500/40 bg-rose-500/10 px-2.5 py-1 text-[11px] font-medium text-rose-300 transition hover:bg-rose-500/20 disabled:opacity-50"
          >
            만료
          </button>
        )}
      </DataTable.Cell>
    </DataTable.Row>
  );
}

function GrantModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [memberId, setMemberId] = useState("");
  const [plan, setPlan] = useState<AdminSubscriptionPlan>("THREE_DAY");
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setError(null);
    const id = Number(memberId);
    if (!id || id <= 0) {
      setError("memberId 는 양의 정수여야 합니다.");
      return;
    }
    if (!reason.trim()) {
      setError("발급 사유를 입력하세요 (감사 로그용).");
      return;
    }
    setBusy(true);
    try {
      await grantSubscription(id, plan, reason.trim());
      onDone();
      onClose();
    } catch (e) {
      setError(e instanceof Error ? e.message : "발급 실패");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
      <div className="mx-4 w-full max-w-md rounded-xl border border-border bg-background p-6 shadow-2xl">
        <h3 className="text-lg font-bold">구독 수동 발급</h3>
        <p className="mt-1 text-xs text-muted">
          보상·이벤트·환불 후 재발급 등 운영 케이스 전용. 사유는 감사 로그에 기록됩니다.
        </p>

        <div className="mt-5 space-y-4">
          <div>
            <label className="block text-xs font-medium">Member ID</label>
            <input
              type="number"
              value={memberId}
              onChange={(e) => setMemberId(e.target.value)}
              placeholder="예: 42"
              className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
            />
            <p className="mt-1 text-[11px] text-muted">회원 관리 페이지에서 확인 가능</p>
          </div>

          <div>
            <label className="block text-xs font-medium">Plan</label>
            <div className="mt-1 grid grid-cols-2 gap-2">
              {(["THREE_DAY", "FOCUS", "ONE_MONTH", "UNLIMITED"] as const).map((p) => (
                <button
                  key={p}
                  onClick={() => setPlan(p)}
                  className={`rounded-lg border px-3 py-2 text-xs font-medium transition ${
                    plan === p
                      ? "border-violet-500 bg-violet-500/10 text-violet-200"
                      : "border-border bg-surface text-muted hover:text-foreground"
                  }`}
                >
                  {PLAN_LABEL[p]}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium">발급 사유 *</label>
            <input
              type="text"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="예: 결제 오류 보상, 이벤트 참여 보상"
              className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
            />
          </div>

          {error && <p className="text-xs text-rose-400">{error}</p>}
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <button
            onClick={onClose}
            disabled={busy}
            className="rounded-lg border border-border bg-surface px-4 py-2 text-sm text-muted hover:text-foreground disabled:opacity-50"
          >
            취소
          </button>
          <button
            onClick={submit}
            disabled={busy}
            className="rounded-lg bg-violet-500 px-4 py-2 text-sm font-semibold text-white hover:bg-violet-600 disabled:opacity-50"
          >
            {busy ? "발급 중..." : "발급"}
          </button>
        </div>
      </div>
    </div>
  );
}
