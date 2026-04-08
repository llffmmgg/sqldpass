package com.sqldpass.controller.feedback.dto;

import com.sqldpass.persistent.feedback.FeedbackType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
        @NotNull(message = "type 은 필수입니다.") FeedbackType type,
        Long questionId,
        @NotNull @Size(min = 5, max = 2000, message = "내용은 5~2000자 사이여야 합니다.") String content,
        @Size(max = 500) String pageUrl
) {
}
