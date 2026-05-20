package com.sqldpass.controller.admin;

import java.time.LocalDate;

/**
 * 일자 × provider 분리 매출 지표.
 * - provider: "PORTONE" | "PLAY_BILLING" | "APP_STORE".
 *   V79 이전 옛 결제는 DB 상 NULL 일 수 있으나 Service 단에서 "PORTONE" 으로 보정한다.
 * - revenue: 해당 일자/provider PAID 결제 합산 (KRW).
 * - refundAmount: 해당 일자/provider CANCELLED 결제 합산.
 * - count: 해당 일자/provider PAID 건수.
 *
 * archived 구독에 연결된 결제는 집계에서 제외 — 기존 {@link AdminRevenuePoint} 와 동일 규칙.
 */
public record AdminRevenueByProviderPoint(
        LocalDate date,
        String provider,
        long revenue,
        long refundAmount,
        int count
) {}
