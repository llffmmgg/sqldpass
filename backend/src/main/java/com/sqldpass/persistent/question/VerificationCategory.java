package com.sqldpass.persistent.question;

/**
 * 어드민 LLM 검증 결과 카테고리.
 * 검증 실행 시 매 문제마다 결과에 따라 세팅되며, 어드민이 문제를 수정하면 NONE 으로 리셋된다.
 */
public enum VerificationCategory {
    /** 미검증이거나 마지막 검증에서 APPROVED — 표시 대상 아님 */
    NONE,
    /** REJECTED + 자동 fix 성공. 자동으로 수정되었으나 어드민의 사후 검토 필요 */
    AUTO_FIXED,
    /** REJECTED + 자동 fix 실패. 어드민이 직접 수정 필요 */
    MANUAL_REVIEW,
    /** UNKNOWN — LLM 이 판단하지 못함 (호출 예외/응답 누락 포함) */
    ERROR
}
