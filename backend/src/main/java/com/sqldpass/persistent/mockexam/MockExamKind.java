package com.sqldpass.persistent.mockexam;

/**
 * 모의고사 구분 — AI 생성(AI) / 전문가 복원 기출(PAST_EXAM) / 미니 모의고사(MINI).
 */
public enum MockExamKind {
    /** AI 생성 모의고사 (기본) */
    AI,

    /** 실제 시험에서 복원한 기출 회차 */
    PAST_EXAM,

    /**
     * 미니 모의고사 — 기존 풀(AI Published / PREMIUM / PAST_EXAM) 에서 문제를 복제해
     * 분량을 축소한 PREMIUM 회차. 원본 문제는 included_in_mini_at 으로 마킹돼 중복 방지.
     */
    MINI
}
