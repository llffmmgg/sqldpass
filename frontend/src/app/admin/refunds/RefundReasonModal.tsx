"use client";

import { useEffect, useRef, useState } from "react";

import {
  refundAdminPayment,
  type AdminPaymentRow,
  type AdminSubscriptionPlan,
} from "@/lib/adminApi";
import { useToast } from "@/components/Toast";

const PLAN_LABEL: Record<AdminSubscriptionPlan, string> = {
  THREE_DAY: "Thunder",
  FOCUS: "Focus",
  ONE_MONTH: "Pro",
  UNLIMITED: "All Pass",
};

const REFUND_TIMEOUT_MS = 30_000;
const MIN_REASON = 5;
const MAX_REASON = 200;

interface Props {
  payment: AdminPaymentRow;
  onClose: () => void;
  onConfirmed: () => void;
}

function validateReason(v: string): string | null {
  const t = v.trim();
  if (!t) return "환불 사유를 입력해주세요.";
  if (t.length < MIN_REASON) return `사유는 ${MIN_REASON}자 이상으로 입력해주세요.`;
  if (t.length > MAX_REASON) return `사유는 ${MAX_REASON}자 이내로 입력해주세요.`;
  return null;
}

/**
 * 환불 사유 입력 모달.
 *
 * - busy 중에는 ESC/overlay/X 닫기 차단 — 응답 결과 안내 누락 + setState on unmounted 회피
 * - 30초 AbortController timeout — PG 응답 지연 시 사용자 답답함 방지
 * - 에러 catch 시 PortOne 콘솔 확인 안내 토스트 + 모달 유지 (재시도 가능)
 */
export default function RefundReasonModal({ payment, onClose, onConfirmed }: Props) {
  const toast = useToast();
  const [reason, setReason] = useState("");
  const [touched, setTouched] = useState(false);
  const [busy, setBusy] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  // ESC/스크롤 락 — busy 중 닫기 차단
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape" && !busy) onClose();
    }
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
      abortRef.current?.abort();
    };
  }, [busy, onClose]);

  const reasonError = validateReason(reason);
  const canSubmit = !reasonError && !busy;
  const nickname = payment.nickname ?? `(탈퇴 회원 #${payment.memberId})`;
  const planLabel = payment.plan ? PLAN_LABEL[payment.plan] : "(plan 없음)";

  async function handleSubmit() {
    setTouched(true);
    if (!canSubmit) return;
    setBusy(true);
    const controller = new AbortController();
    abortRef.current = controller;
    const timer = window.setTimeout(() => controller.abort(), REFUND_TIMEOUT_MS);
    try {
      await refundAdminPayment(payment.id, reason.trim(), controller.signal);
      toast.show(`${nickname} ${planLabel} 환불 처리 완료`, "success");
      onConfirmed();
    } catch (e) {
      const message = e instanceof Error ? e.message : "";
      if (controller.signal.aborted) {
        toast.show(
          "PG 응답 지연으로 환불 결과 확인이 늦어졌습니다. 잠시 후 결제 목록을 새로고침해주세요.",
          "info",
        );
        onClose();
      } else {
        toast.show(
          message
            ? `환불 실패: ${message}. PortOne 콘솔에서 결제 상태를 확인하고 운영자에게 보고해주세요.`
            : "환불에 실패했습니다. PortOne 콘솔에서 결제 상태를 확인하고 운영자에게 보고해주세요.",
          "error",
        );
        // 모달 유지 — 재시도 가능
      }
    } finally {
      window.clearTimeout(timer);
      abortRef.current = null;
      setBusy(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[60] bg-black/60 backdrop-blur-sm"
      onClick={() => {
        if (!busy) onClose();
      }}
    >
      <div className="flex min-h-full items-center justify-center px-4 py-6 sm:py-8">
        <div
          className="max-h-[calc(100dvh-3rem)] w-full max-w-lg overflow-y-auto rounded-2xl border border-border bg-surface p-6 shadow-xl sm:max-h-[calc(100dvh-4rem)] sm:p-8"
          onClick={(e) => e.stopPropagation()}
        >
          {/* 헤더 */}
          <div className="flex items-start justify-between gap-4">
            <h2 className="text-xl font-bold tracking-tight text-text">결제 환불</h2>
            <button
              type="button"
              onClick={() => {
                if (!busy) onClose();
              }}
              disabled={busy}
              className="text-text-subtle transition-colors hover:text-text disabled:cursor-not-allowed disabled:opacity-50"
              aria-label="닫기"
            >
              ✕
            </button>
          </div>

          {/* 결제 요약 */}
          <div className="mt-5 rounded-lg border border-border bg-bg-elevated p-4 text-sm text-text">
            <div className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5">
              <span className="text-text-subtle">회원</span>
              <span>{nickname}</span>
              <span className="text-text-subtle">Plan</span>
              <span>{planLabel}</span>
              <span className="text-text-subtle">금액</span>
              <span>
                ₩{payment.amount.toLocaleString()}
                {payment.prorateDiscount > 0 && (
                  <span className="ml-2 text-xs text-text-subtle">
                    (원가 ₩{payment.baseAmount.toLocaleString()} − 차감 ₩
                    {payment.prorateDiscount.toLocaleString()})
                  </span>
                )}
              </span>
              <span className="text-text-subtle">paymentId</span>
              <span className="break-all font-mono text-xs">{payment.paymentId}</span>
              <span className="text-text-subtle">구매자</span>
              <span>
                {payment.buyerName ?? "–"}
                {payment.buyerEmail && (
                  <span className="ml-2 text-xs text-text-muted">· {payment.buyerEmail}</span>
                )}
                {payment.buyerPhoneNumber && (
                  <span className="ml-2 text-xs text-text-muted">· {payment.buyerPhoneNumber}</span>
                )}
              </span>
            </div>
          </div>

          {/* 안내 */}
          <p className="mt-4 text-[12.5px] leading-relaxed text-text-muted">
            확인 후 진행하면 PortOne 결제 취소 + 구독 즉시 회수 + 환불 이력 기록이 한 번에
            처리됩니다. PortOne 콘솔에서 직접 취소하지 마세요.
          </p>

          {/* 사유 입력 */}
          <div className="mt-5">
            <label htmlFor="refund-reason" className="block text-xs font-medium text-text-muted">
              환불 사유 <span className="text-danger">*</span>
              <span className="ml-2 text-[11px] text-text-subtle">
                ({MIN_REASON}~{MAX_REASON}자)
              </span>
            </label>
            <textarea
              id="refund-reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              onBlur={() => setTouched(true)}
              placeholder="예: 고객 요청 - 단순 변심 (2026-05-12 CS)"
              rows={4}
              maxLength={MAX_REASON}
              disabled={busy}
              className="mt-1.5 w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle transition-colors focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30 disabled:opacity-60"
            />
            <div className="mt-1 flex items-center justify-between text-[11px]">
              {touched && reasonError ? (
                <p className="text-danger">{reasonError}</p>
              ) : (
                <p className="text-text-subtle">감사 로그에 기록됩니다.</p>
              )}
              <span className="text-text-subtle">
                {reason.length} / {MAX_REASON}
              </span>
            </div>
          </div>

          {/* 버튼 */}
          <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:justify-end">
            <button
              type="button"
              onClick={onClose}
              disabled={busy}
              className="rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-text transition-colors hover:bg-bg-elevated disabled:cursor-not-allowed disabled:opacity-50"
            >
              취소
            </button>
            <button
              type="button"
              onClick={handleSubmit}
              disabled={!canSubmit}
              className="rounded-lg bg-danger px-4 py-2 text-sm font-semibold text-white transition hover:bg-danger/90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {busy ? (
                <span className="inline-flex items-center gap-2">
                  <svg className="h-3.5 w-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth={3} className="opacity-25" />
                    <path fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" className="opacity-75" />
                  </svg>
                  환불 처리 중...
                </span>
              ) : (
                "환불 처리"
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
