package com.sqldpass.persistent.question;

/**
 * 문제 유형 — 채점 로직 분기 기준.
 */
public enum QuestionType {
    /** 4지선다형. selectedOption(1~4) == correctOption 비교 */
    MCQ,

    /** 단답형. 정규화 후 answer 또는 keywords alias 중 매치 */
    SHORT_ANSWER,

    /** 서술형. keywords 포함률 + 길이 조건으로 정답/부분/오답 판정 */
    DESCRIPTIVE
}
