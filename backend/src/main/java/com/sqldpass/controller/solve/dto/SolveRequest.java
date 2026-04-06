package com.sqldpass.controller.solve.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SolveRequest(
        Long subjectId,
        Long mockExamId,
        @NotEmpty(message = "답안 목록은 비어있을 수 없습니다.") @Valid List<SolveAnswerRequest> answers) {

    /**
     * subjectId 또는 mockExamId 중 하나는 반드시 있어야 함.
     * SolveService에서 validate.
     */
}
