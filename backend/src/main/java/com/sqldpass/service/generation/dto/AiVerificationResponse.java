package com.sqldpass.service.generation.dto;

/**
 * 단일 문제 검증 응답.
 *
 * - outcome: APPROVED/REJECTED/UNKNOWN. UNKNOWN은 verified_at을 건드리지 않고 다음 회차에 재시도.
 * - reason:  REJECTED일 때 LLM이 제시한 사유, 또는 UNKNOWN일 때 실패 이유.
 * - fixable: REJECTED일 때 자동 fix를 시도해도 좋은지 LLM이 판단한 hint. null이면 미상.
 */
public record AiVerificationResponse(VerificationOutcome outcome, String reason, Boolean fixable) {

    public boolean isApproved() {
        return outcome == VerificationOutcome.APPROVED;
    }

    public boolean isRejected() {
        return outcome == VerificationOutcome.REJECTED;
    }

    public boolean isUnknown() {
        return outcome == VerificationOutcome.UNKNOWN;
    }

    /** 호환용 — QuestionGenerationService 등 기존 호출자가 .approved() boolean 사용 */
    public boolean approved() {
        return isApproved();
    }

    public static AiVerificationResponse ofApproved() {
        return new AiVerificationResponse(VerificationOutcome.APPROVED, null, null);
    }

    public static AiVerificationResponse ofRejected(String reason, Boolean fixable) {
        return new AiVerificationResponse(VerificationOutcome.REJECTED, reason, fixable);
    }

    public static AiVerificationResponse ofUnknown(String reason) {
        return new AiVerificationResponse(VerificationOutcome.UNKNOWN, reason, null);
    }
}
