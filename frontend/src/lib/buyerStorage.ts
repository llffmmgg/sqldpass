/**
 * 결제 모달의 이메일/휴대폰 입력값 localStorage wrapper.
 * 이름(fullName) 은 영수증 실명용이라 매번 명시 입력 — 저장 안 함.
 *
 * SSR 안전성: typeof window 가드로 빌드 타임 prerender 회피.
 */

const EMAIL_KEY = "checkout_buyer_email";
const PHONE_KEY = "checkout_buyer_phone";

export type StoredBuyerInfo = {
  email: string;
  phoneNumber: string;
};

export function getStoredBuyerInfo(): StoredBuyerInfo {
  if (typeof window === "undefined") return { email: "", phoneNumber: "" };
  return {
    email: localStorage.getItem(EMAIL_KEY) ?? "",
    phoneNumber: localStorage.getItem(PHONE_KEY) ?? "",
  };
}

export function setStoredBuyerInfo(email: string, phoneNumber: string): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(EMAIL_KEY, email);
  localStorage.setItem(PHONE_KEY, phoneNumber);
}
