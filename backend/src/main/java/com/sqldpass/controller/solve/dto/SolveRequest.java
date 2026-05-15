package com.sqldpass.controller.solve.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SolveRequest(
        Long subjectId,
        Long mockExamId,
        SolveSource source,
        @NotEmpty(message = "답안 목록은 비어있을 수 없습니다.") @Valid List<SolveAnswerRequest> answers) {

    /**
     * source 기본값은 NORMAL.
     * NORMAL: subjectId 또는 mockExamId 중 정확히 하나 필요.
     * BOOKMARK: 즐겨찾기 모아 풀기 — subjectId/mockExamId 둘 다 null 이어야 한다.
     * 실제 검증은 SolveService 에서 수행.
     */
    public SolveSource effectiveSource() {
        return source != null ? source : SolveSource.NORMAL;
    }
}
