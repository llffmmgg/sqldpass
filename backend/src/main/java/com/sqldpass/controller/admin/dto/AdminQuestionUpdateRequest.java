package com.sqldpass.controller.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminQuestionUpdateRequest(
        @NotBlank(message = "문제 내용은 필수입니다.") String content,
        @NotNull(message = "정답 번호는 필수입니다.") @Min(1) @Max(4) Integer correctOption,
        String explanation,
        String summary) {
}
