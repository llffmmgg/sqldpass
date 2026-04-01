package com.sqldpass.controller.solve;

import jakarta.validation.constraints.NotNull;

public record SolveAnswerRequest(
        @NotNull(message = "문제 ID는 필수입니다.") Long questionId,
        @NotNull(message = "선택한 보기 번호는 필수입니다.") Integer selectedOption) {
}
