package com.sqldpass.controller.admin;

/**
 * provider × plan 분리 매출 분포.
 * - provider: "PORTONE" | "PLAY_BILLING" | "APP_STORE" (NULL 은 Service 단에서 "PORTONE" 보정).
 * - plan: SubscriptionPlan enum 이름 ("THREE_DAY" | "FOCUS" | "ONE_MONTH" | "UNLIMITED").
 * - count: PAID 건수.
 * - revenue: PAID 매출 합산 (KRW).
 *
 * archived 구독에 연결된 결제는 집계에서 제외. revenue DESC 정렬로 반환.
 */
public record AdminRevenueByProviderPlan(
        String provider,
        String plan,
        int count,
        long revenue
) {}
