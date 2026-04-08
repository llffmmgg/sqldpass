package com.sqldpass.controller.admin.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AdminQuestionUpdateRequest(
        @NotBlank(message = "문제 내용은 필수입니다.") String content,
        @NotBlank(message = "문제 유형은 필수입니다.") String questionType,
        @Min(1) @Max(4) Integer correctOption,
        String answer,
        List<String> keywords,
        String explanation,
        String summary) {
}
