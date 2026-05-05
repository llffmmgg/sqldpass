package com.sqldpass.persistent.payment;

public enum PaymentStatus {
    /** prepare 시점 — PortOne 호출 전 사전 등록 상태. */
    PENDING,
    /** PortOne 검증 통과 — 잠금 해제 권리 발급 완료. */
    PAID,
    /** 검증 실패 — 금액 불일치/상태 미일치 등. */
    FAILED,
    /** 결제 취소 (관리자 환불 또는 PortOne 측 취소). */
    CANCELLED
}
