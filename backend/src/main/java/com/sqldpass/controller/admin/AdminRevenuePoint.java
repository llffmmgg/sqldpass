package com.sqldpass.controller.admin;

import java.time.LocalDate;

/**
 * 일별 매출 지표.
 * - revenue: 해당 일자 PAID 결제 합산 (KRW).
 * - refundAmount: 해당 일자 CANCELLED(=환불) 결제 합산 — 환불은 status 가 CANCELLED 로 변경됨.
 * - count: 해당 일자 PAID 건수.
 *
 * archived 구독에 연결된 결제는 집계에서 제외.
 */
public record AdminRevenuePoint(
        LocalDate date,
        long revenue,
        long refundAmount,
        int count
) {}
