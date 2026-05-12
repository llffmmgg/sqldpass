package com.sqldpass.controller.admin;

import java.time.LocalDateTime;

import com.sqldpass.persistent.payment.PaymentProvider;
import com.sqldpass.persistent.payment.PaymentStatus;
import com.sqldpass.persistent.payment.SubscriptionPlan;

/**
 * 어드민 결제 목록 조회 응답 행.
 *
 * <p>{@link com.sqldpass.persistent.payment.PaymentRepository#findAdminPage} JPQL 의 SELECT new
 * projection 으로 생성된다. member LEFT JOIN 이라 nickname 은 nullable (탈퇴 회원).
 * paidAt 도 PENDING/FAILED 결제에서 null.
 *
 * <p>buyer 3필드(name/email/phone) 는 V84 이전 결제에서 null 가능 — 프론트에서 "–" 처리.
 */
public record AdminPaymentRow(
        Long id,
        String paymentId,
        Long memberId,
        String nickname,
        SubscriptionPlan plan,
        Integer amount,
        Integer baseAmount,
        Integer prorateDiscount,
        PaymentStatus status,
        PaymentProvider provider,
        String buyerName,
        String buyerEmail,
        String buyerPhoneNumber,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        /**
         * 같은 회원의 더 최신(paid_at 기준) PAID 결제 중 subscription 이 활성인 것이 존재하면 true.
         * UI 는 이 결제의 환불 버튼을 disable 처리해 운영 사고(옛 결제 환불 + 활성 구독 그대로) 차단.
         * 백엔드도 동일 판별로 revokePortOnePayment 에서 PAYMENT_SUPERSEDED_BY_NEWER throw.
         */
        Boolean supersededByNewerPayment
) {}
