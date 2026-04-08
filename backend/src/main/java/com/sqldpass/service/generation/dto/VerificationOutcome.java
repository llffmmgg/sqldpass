package com.sqldpass.service.generation.dto;

/**
 * 단일 문제 LLM 검증 판정 결과.
 *
 * APPROVED  — LLM이 정상이라고 판정. verified_at 마킹 대상.
 * REJECTED  — LLM이 의심이라고 판정. fix 시도 또는 수동 검토 큐로.
 * UNKNOWN   — 빈 응답/파싱 실패/예외. 다음 회차에 다시 검증해야 함 — verified_at은 건드리지 않음.
 */
public enum VerificationOutcome {
    APPROVED,
    REJECTED,
    UNKNOWN
}
