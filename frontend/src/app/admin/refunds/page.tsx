"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 어드민 페이지의 fetch → setState 흐름 */

import { useEffect, useState } from "react";

import {
  listAdminPayments,
  type AdminPaymentPage,
  type AdminPaymentProvider,
  type AdminPaymentRow,
  type AdminPaymentStatus,
  type AdminSubscriptionPlan,
} from "@/lib/adminApi";
import PageHeader from "@/components/admin/PageHeader";
import DataTable, { TableSkeleton } from "@/components/admin/DataTable";
import EmptyState from "@/components/admin/EmptyState";

import RefundReasonModal from "./RefundReasonModal";

const PLAN_LABEL: Record<AdminSubscriptionPlan, string> = {
  THREE_DAY: "3일권",
  ONE_MONTH: "한달권",
  UNLIMITED: "무제한",
};

const STATUS_LABEL: Record<AdminPaymentStatus, string> = {
  PENDING: "대기",
  PAID: "결제완료",
  FAILED: "실패",
  CANCELLED: "환불됨",
};

const STATUS_CHIP: Record<AdminPaymentStatus, string> = {
  PENDING: "border-amber-500/40 bg-amber-500/10 text-amber-300",
  PAID: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  FAILED: "border-rose-500/40 bg-rose-500/10 text-rose-300",
  CANCELLED: "border-zinc-500/40 bg-zinc-500/10 text-zinc-400",
};

const PROVIDER_LABEL: Record<AdminPaymentProvider, string> = {
  PORTONE: "PortOne",
  PLAY_BILLING: "Play",
};

const STATUS_OPTIONS: { value: "" | AdminPaymentStatus; label: string }[] = [
  { value: "", label: "전체 상태" },
  { value: "PAID", label: "결제완료" },
  { value: "CANCELLED", label: "환불됨" },
  { value: "FAILED", label: "실패" },
  { value: "PENDING", label: "대기" },
];

const PROVIDER_OPTIONS: { value: "" | AdminPaymentProvider; label: string }[] = [
  { value: "", label: "전체 채널" },
  { value: "PORTONE", label: "PortOne" },
  { value: "PLAY_BILLING", label: "Play Billing" },
];

export default function AdminRefundsPage() {
  const [data, setData] = useState<AdminPaymentPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"" | AdminPaymentStatus>("");
  const [providerFilter, setProviderFilter] = useState<"" | AdminPaymentProvider>("");
  const [page, setPage] = useState(0);
  const [refundTarget, setRefundTarget] = useState<AdminPaymentRow | null>(null);

  function reload() {
    setLoading(true);
    // 검색 input 자동 분기 — "sqldpass-" 접두사면 paymentId 필터, 아니면 nickname
    const trimmed = search.trim();
    const opts: Parameters<typeof listAdminPayments>[0] = {
      page,
      size: 20,
      status: statusFilter || undefined,
      provider: providerFilter || undefined,
    };
    if (trimmed) {
      if (trimmed.startsWith("sqldpass-")) {
        opts.paymentId = trimmed;
      } else {
        opts.nickname = trimmed;
      }
    }
    listAdminPayments(opts)
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }

  useEffect(reload, [page, search, statusFilter, providerFilter]);

  return (
    <div>
      <PageHeader
        title="환불 관리"
        description="결제 목록을 보고 PortOne 결제를 안전하게 환불합니다. PortOne 콘솔 직접 취소는 DB 동기화가 깨지므로 금지."
      />

      {/* 필터 */}
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <input
          type="text"
          placeholder="닉네임 또는 paymentId(sqldpass-...) 검색"
          value={search}
          onChange={(e) => {
            setPage(0);
            setSearch(e.target.value);
          }}
          className="w-80 rounded-lg border border-border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
        />
        <select
          value={statusFilter}
          onChange={(e) => {
            setPage(0);
            setStatusFilter(e.target.value as "" | AdminPaymentStatus);
          }}
          className="rounded-lg border border-border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
        >
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <select
          value={providerFilter}
          onChange={(e) => {
            setPage(0);
            setProviderFilter(e.target.value as "" | AdminPaymentProvider);
          }}
          className="rounded-lg border border-border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
        >
          {PROVIDER_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <DataTable>
          <DataTable.Head>
            <DataTable.HeadCell>결제일</DataTable.HeadCell>
            <DataTable.HeadCell>회원</DataTable.HeadCell>
            <DataTable.HeadCell>Plan</DataTable.HeadCell>
            <DataTable.HeadCell align="right">금액</DataTable.HeadCell>
            <DataTable.HeadCell align="right">상태</DataTable.HeadCell>
            <DataTable.HeadCell align="right">채널</DataTable.HeadCell>
            <DataTable.HeadCell align="right">액션</DataTable.HeadCell>
          </DataTable.Head>
          <TableSkeleton cols={7} rows={6} />
        </DataTable>
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title="조회된 결제가 없어요"
          description={
            search || statusFilter || providerFilter
              ? "필터 조건에 맞는 결제가 없습니다."
              : "아직 결제 기록이 없습니다."
          }
        />
      ) : (
        <>
          <DataTable maxHeight="640px">
            <DataTable.Head>
              <DataTable.HeadCell>결제일</DataTable.HeadCell>
              <DataTable.HeadCell>회원</DataTable.HeadCell>
              <DataTable.HeadCell>Plan</DataTable.HeadCell>
              <DataTable.HeadCell align="right">금액</DataTable.HeadCell>
              <DataTable.HeadCell align="right">상태</DataTable.HeadCell>
              <DataTable.HeadCell align="right">채널</DataTable.HeadCell>
              <DataTable.HeadCell align="right">액션</DataTable.HeadCell>
            </DataTable.Head>
            <tbody>
              {data.content.map((row) => (
                <PaymentRow
                  key={row.id}
                  row={row}
                  onRefund={() => setRefundTarget(row)}
                />
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

      {refundTarget && (
        <RefundReasonModal
          payment={refundTarget}
          onClose={() => setRefundTarget(null)}
          onConfirmed={() => {
            setRefundTarget(null);
            reload();
          }}
        />
      )}
    </div>
  );
}

function PaymentRow({
  row,
  onRefund,
}: {
  row: AdminPaymentRow;
  onRefund: () => void;
}) {
  const cancelled = row.status === "CANCELLED";
  const refundable = row.status === "PAID" && row.provider === "PORTONE";
  const disabledReason = !refundable ? getDisabledReason(row) : null;
  const nickname = row.nickname ?? `(탈퇴 회원 #${row.memberId})`;
  const planLabel = row.plan ? PLAN_LABEL[row.plan] : "–";

  return (
    <tr className={cancelled ? "opacity-60" : ""}>
      <DataTable.Cell mono>
        {row.paidAt ? formatDateTime(row.paidAt) : <span className="text-text-subtle">미결제</span>}
      </DataTable.Cell>
      <DataTable.Cell>{nickname}</DataTable.Cell>
      <DataTable.Cell>{planLabel}</DataTable.Cell>
      <DataTable.Cell align="right">
        <div className="tabular-nums">
          ₩{row.amount.toLocaleString()}
          {row.prorateDiscount > 0 && (
            <div className="text-[11px] text-text-subtle">
              원가 ₩{row.baseAmount.toLocaleString()}
            </div>
          )}
        </div>
      </DataTable.Cell>
      <DataTable.Cell align="right">
        <span
          className={`inline-flex rounded-full border px-2 py-0.5 text-[11px] font-medium ${STATUS_CHIP[row.status]}`}
        >
          {STATUS_LABEL[row.status]}
        </span>
      </DataTable.Cell>
      <DataTable.Cell align="right">
        <span className="text-text-muted">{PROVIDER_LABEL[row.provider]}</span>
      </DataTable.Cell>
      <DataTable.Cell align="right">
        <button
          type="button"
          onClick={onRefund}
          disabled={!refundable}
          title={disabledReason ?? "환불 모달 열기"}
          className="rounded-md border border-rose-500/40 bg-rose-500/10 px-3 py-1.5 text-xs font-medium text-rose-300 transition hover:bg-rose-500/20 disabled:cursor-not-allowed disabled:border-border disabled:bg-transparent disabled:text-text-subtle"
        >
          환불
        </button>
      </DataTable.Cell>
    </tr>
  );
}

function getDisabledReason(row: AdminPaymentRow): string {
  if (row.status === "CANCELLED") return "이미 환불됨";
  if (row.status === "FAILED") return "결제 실패 — 환불 대상 아님";
  if (row.status === "PENDING") return "결제 미완료 — 환불 대상 아님";
  if (row.provider === "PLAY_BILLING") {
    return "Play Billing 은 Google RTDN 으로 자동 환불 처리 — 어드민 수동 환불 금지";
  }
  return "환불 불가";
}

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${day} ${hh}:${mm}`;
}
