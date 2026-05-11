"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

import { Button } from "@/components/ui";
import { getStoredBuyerInfo, setStoredBuyerInfo } from "@/lib/buyerStorage";
import type { BuyerInfo, SubscriptionPlan } from "@/lib/payment";
import { planLabel } from "@/components/billing/CheckoutLanding";

interface Props {
  open: boolean;
  plan: SubscriptionPlan | null;
  onClose: () => void;
  onSubmit: (buyer: BuyerInfo) => void;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_RE = /^01[0-9][-\s]?\d{3,4}[-\s]?\d{4}$/;

function validateName(v: string): string | null {
  const t = v.trim();
  if (!t) return "이름을 입력해주세요.";
  if (t.length > 50) return "이름은 50자 이내로 입력해주세요.";
  return null;
}

function validateEmail(v: string): string | null {
  const t = v.trim();
  if (!t) return "이메일을 입력해주세요.";
  if (!EMAIL_RE.test(t)) return "이메일 형식이 올바르지 않습니다.";
  return null;
}

function validatePhone(v: string): string | null {
  const t = v.trim();
  if (!t) return "휴대폰 번호를 입력해주세요.";
  if (!PHONE_RE.test(t)) return "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)";
  return null;
}

/**
 * 결제 직전 구매자 정보 입력 모달.
 *
 * KG이니시스 PortOne V2 PC 일반결제는 customer.fullName/email/phoneNumber 가 필수다.
 * 회원가입 시에는 닉네임 외 개인정보를 받지 않기 때문에, 결제 시점에만 모달로 수집해
 * payment 테이블에 결제 기록과 함께 저장한다. 이메일·휴대폰은 localStorage 에 저장해
 * 다음 결제 시 자동 채움. 이름은 영수증 실명용이라 매번 명시 입력.
 */
export default function BuyerInfoModal({ open, plan, onClose, onSubmit }: Props) {
  // 이름은 영수증 실명용이라 매번 비어있는 상태로 시작 (저장 안 함).
  // email/phone 은 localStorage 자동 채움 — lazy initializer 로 마운트 시 한 번만 읽음.
  // 부모(CheckoutClient) 가 open 토글마다 conditional mount 해서 매 호출 시 state 초기화됨.
  const [name, setName] = useState("");
  const [email, setEmail] = useState(() => getStoredBuyerInfo().email);
  const [phoneNumber, setPhoneNumber] = useState(() => getStoredBuyerInfo().phoneNumber);
  const [touched, setTouched] = useState<{ name: boolean; email: boolean; phone: boolean }>({
    name: false,
    email: false,
    phone: false,
  });

  // ESC로 닫기 + body scroll lock
  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open, onClose]);

  if (!open) return null;

  const nameError = validateName(name);
  const emailError = validateEmail(email);
  const phoneError = validatePhone(phoneNumber);
  const valid = !nameError && !emailError && !phoneError;

  function handleSubmit() {
    setTouched({ name: true, email: true, phone: true });
    if (!valid) return;
    const trimmedName = name.trim();
    const trimmedEmail = email.trim();
    const trimmedPhone = phoneNumber.trim();
    setStoredBuyerInfo(trimmedEmail, trimmedPhone);
    onSubmit({ name: trimmedName, email: trimmedEmail, phoneNumber: trimmedPhone });
  }

  return (
    <div
      className="fixed inset-0 z-[60] bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <div className="flex min-h-full items-center justify-center px-4 py-6 sm:py-8">
        <div
          className="max-h-[calc(100dvh-3rem)] w-full max-w-lg overflow-y-auto rounded-2xl border border-border bg-surface p-6 shadow-xl sm:max-h-[calc(100dvh-4rem)] sm:p-8"
          onClick={(e) => e.stopPropagation()}
        >
          {/* 헤더 */}
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="text-lg font-bold tracking-tight">결제 정보 입력</h2>
              {plan && (
                <p className="mt-0.5 text-xs text-text-muted">
                  {planLabel(plan)} 결제를 진행합니다.
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={onClose}
              className="text-muted transition-colors hover:text-foreground"
              aria-label="닫기"
            >
              ✕
            </button>
          </div>

          {/* 안내 박스 — 회원정보 미저장 정책 + 왜 결제 시점에만 받는지 + 어디 쓰는지 */}
          <div className="mt-5 rounded-lg border border-amber-500/30 bg-amber-500/[0.08] p-4 text-[12.5px] leading-relaxed text-text">
            <p>
              문어 CBT 는 <span className="font-semibold">회원가입 시 닉네임 외에는 어떤 개인정보도 저장하지 않습니다</span>.
            </p>
            <p className="mt-2 text-text-muted">
              결제 영수증 발송·결제 내역 확인·결제 오류 대응(환불, CS 식별) 을 위해 신용카드 PG(KG이니시스) 가
              구매자 정보를 요구하므로, 결제 시점에만 아래 정보를 입력받습니다. 입력하신 정보는 결제 기록과 함께
              저장되며 회원 정보와는 분리됩니다.
            </p>
          </div>

          {/* 이름 */}
          <div className="mt-5">
            <label htmlFor="buyer-name" className="block text-xs font-medium text-muted">
              이름 <span className="text-error">*</span>
            </label>
            <input
              id="buyer-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, name: true }))}
              placeholder="홍길동"
              className="mt-1.5 w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-foreground placeholder:text-text-subtle focus:border-primary focus:outline-none"
              maxLength={50}
            />
            <p className="mt-1 text-[11px] text-text-subtle">결제 영수증과 환불·CS 식별에 사용</p>
            {touched.name && nameError && (
              <p className="mt-1 text-[11px] text-error">{nameError}</p>
            )}
          </div>

          {/* 이메일 */}
          <div className="mt-4">
            <label htmlFor="buyer-email" className="block text-xs font-medium text-muted">
              이메일 <span className="text-error">*</span>
            </label>
            <input
              id="buyer-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, email: true }))}
              placeholder="example@email.com"
              autoComplete="email"
              className="mt-1.5 w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-foreground placeholder:text-text-subtle focus:border-primary focus:outline-none"
              maxLength={255}
            />
            <p className="mt-1 text-[11px] text-text-subtle">결제 영수증·결제 알림 발송용. 다음 결제 시 자동 채움</p>
            {touched.email && emailError && (
              <p className="mt-1 text-[11px] text-error">{emailError}</p>
            )}
          </div>

          {/* 휴대폰 */}
          <div className="mt-4">
            <label htmlFor="buyer-phone" className="block text-xs font-medium text-muted">
              휴대폰 번호 <span className="text-error">*</span>
            </label>
            <input
              id="buyer-phone"
              type="tel"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, phone: true }))}
              placeholder="010-1234-5678"
              autoComplete="tel"
              inputMode="numeric"
              className="mt-1.5 w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-foreground placeholder:text-text-subtle focus:border-primary focus:outline-none"
              maxLength={20}
            />
            <p className="mt-1 text-[11px] text-text-subtle">결제 오류 대응 시 연락용. 다음 결제 시 자동 채움</p>
            {touched.phone && phoneError && (
              <p className="mt-1 text-[11px] text-error">{phoneError}</p>
            )}
          </div>

          {/* 버튼 */}
          <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button variant="outline" size="md" onClick={onClose}>
              취소
            </Button>
            <Button
              variant="primary"
              size="md"
              onClick={handleSubmit}
              disabled={!valid}
            >
              결제 진행
            </Button>
          </div>

          {/* 약관 안내 */}
          <p className="mt-4 text-center text-[11px] text-text-subtle">
            결제 시{" "}
            <Link
              href="/terms"
              target="_blank"
              rel="noopener noreferrer"
              className="underline transition-colors hover:text-text"
            >
              이용약관
            </Link>
            {" · "}
            <Link
              href="/privacy"
              target="_blank"
              rel="noopener noreferrer"
              className="underline transition-colors hover:text-text"
            >
              개인정보처리방침
            </Link>
            에 동의하는 것으로 간주됩니다.
          </p>
        </div>
      </div>
    </div>
  );
}
