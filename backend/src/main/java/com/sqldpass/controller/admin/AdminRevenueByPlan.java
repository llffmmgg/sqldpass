package com.sqldpass.controller.admin;

/**
 * 플랜별 매출 분포.
 * - plan: SubscriptionPlan enum 이름 ("THREE_DAY" | "FOCUS" | "ONE_MONTH" | "UNLIMITED").
 *   프론트가 plan-tokens 로 라벨/색 매핑.
 * - count: PAID 건수.
 * - revenue: PAID 매출 합산.
 *
 * archived 구독에 연결된 결제는 집계에서 제외. revenue DESC 정렬로 반환.
 */
public record AdminRevenueByPlan(
        String plan,
        int count,
        long revenue
) {}
