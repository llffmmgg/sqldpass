package com.sqldpass.persistent.payment;

/**
 * 구독 이력 감사 액션.
 *
 * - GRANTED: 어드민 수동 발급 또는 결제 후 신규 발급.
 * - UPGRADED: 활성 plan 보유 상태에서 결제로 상위 plan 으로 전환된 경우.
 * - REVOKED: 운영자 수동 회수(환불 외).
 * - EXPIRED: 어드민 수동 만료.
 * - REFUNDED: Play Billing RTDN 환불.
 * - ARCHIVED: 만료 후 어드민이 통계 집계에서 분리(테스트 결제 정리 용도). row 보존, 권한엔 영향 없음.
 */
public enum SubscriptionHistoryAction {
    GRANTED,
    UPGRADED,
    REVOKED,
    EXPIRED,
    REFUNDED,
    ARCHIVED
}
