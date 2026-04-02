package com.sqldpass.controller.solve.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SolveRequest(
        @NotNull(message = "과목 ID는 필수입니다.") Long subjectId,
        @NotEmpty(message = "답안 목록은 비어있을 수 없습니다.") @Valid List<SolveAnswerRequest> answers) {
}
