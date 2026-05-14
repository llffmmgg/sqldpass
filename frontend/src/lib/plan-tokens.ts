// SubscriptionPlan 별 색·라벨 토큰. cert-tokens 패턴과 일관.
// admin/subscriptions/page.tsx 의 PLAN_LABEL/PLAN_CHIP 와 색이 같게 정렬.

export type SubscriptionPlanKey = "THREE_DAY" | "FOCUS" | "ONE_MONTH" | "UNLIMITED";

export const PLAN_TOKENS: Record<SubscriptionPlanKey, {
  label: string;
  short: string;
  bar: string;   // 막대 채움
  dot: string;   // 도트/배지
  text: string;  // 텍스트 강조
}> = {
  THREE_DAY: { label: "Thunder",  short: "3일",    bar: "bg-amber-500",   dot: "bg-amber-500",   text: "text-amber-300" },
  FOCUS:     { label: "Focus",    short: "30일",   bar: "bg-sky-500",     dot: "bg-sky-500",     text: "text-sky-300" },
  ONE_MONTH: { label: "Pro",      short: "30일",   bar: "bg-violet-500",  dot: "bg-violet-500",  text: "text-violet-300" },
  UNLIMITED: { label: "All Pass", short: "무기한", bar: "bg-emerald-500", dot: "bg-emerald-500", text: "text-emerald-300" },
};

export function planLabelOf(key: string): string {
  return (PLAN_TOKENS as Record<string, { label: string }>)[key]?.label ?? key;
}
