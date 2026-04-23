package com.sqldpass.persistent.mockexam;

/**
 * 모의고사 구분 — AI 생성(AI) / 전문가 복원 기출(PAST_EXAM).
 */
public enum MockExamKind {
    /** AI 생성 모의고사 (기본) */
    AI,

    /** 실제 시험에서 복원한 기출 회차 */
    PAST_EXAM
}
