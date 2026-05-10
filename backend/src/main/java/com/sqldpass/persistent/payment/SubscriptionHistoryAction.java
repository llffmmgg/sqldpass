package com.sqldpass.persistent.payment;

/**
 * 구독 이력 감사 액션.
 *
 * - GRANTED: 어드민 수동 발급 또는 결제 후 신규 발급(옵션).
 * - REVOKED: 운영자 수동 회수(환불 외).
 * - EXPIRED: 어드민 수동 만료.
 * - REFUNDED: Play Billing RTDN 환불.
 */
public enum SubscriptionHistoryAction {
    GRANTED,
    REVOKED,
    EXPIRED,
    REFUNDED
}
