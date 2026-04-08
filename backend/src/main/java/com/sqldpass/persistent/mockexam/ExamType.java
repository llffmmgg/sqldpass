package com.sqldpass.persistent.mockexam;

/**
 * 모의고사 타입 — 시험 종류에 따라 생성 로직/채점 방식이 다름.
 */
public enum ExamType {
    /** SQL 개발자 자격시험 (4지선다 50문항) */
    SQLD,

    /** 정보처리기사 실기 (단답형 + 서술형 20문항) */
    ENGINEER_PRACTICAL,

    /** 컴퓨터활용능력 1급 필기 (4지선다 60문항: 컴퓨터일반/스프레드시트/데이터베이스 각 20) */
    COMPUTER_LITERACY_1
}
