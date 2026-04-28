package com.sqldpass.persistent.solve;

/**
 * 인사이트 — 오답률 best N 문제 행.
 * 학생 평균 정답률 ≥ 임계 + 풀이수 ≥ 임계 통과한 회원들의 답안만 집계됨.
 */
public interface HardestQuestionProjection {
    Long getQuestionId();
    Long getSubjectId();
    String getSubjectName();
    String getQuestionContent();
    long getAttempts();
    long getWrongCount();
    Double getWrongRate();
}
