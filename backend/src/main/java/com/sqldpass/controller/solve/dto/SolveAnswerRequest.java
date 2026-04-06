package com.sqldpass.controller.solve.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 답안 제출 — MCQ는 selectedOption, 단답/서술은 answerText 사용.
 * 둘 다 null이면 미답(오답 처리).
 */
public record SolveAnswerRequest(
        @NotNull(message = "문제 ID는 필수입니다.") Long questionId,
        Integer selectedOption,
        String answerText) {
}
