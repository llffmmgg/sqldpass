package com.sqldpass.controller.solve.dto;

/**
 * 풀이 출처 — 자격증·과목 단위 풀이(NORMAL) 와 즐겨찾기 모아 풀기(BOOKMARK) 를 구분.
 * BOOKMARK 인 경우 subjectId/mockExamId 둘 다 null 로 들어와도 허용한다.
 */
public enum SolveSource {
    NORMAL,
    BOOKMARK
}
