package com.sqldpass.controller.admin.dto;

import com.sqldpass.persistent.feedback.FeedbackStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateFeedbackStatusRequest(
        @NotNull FeedbackStatus status
) {
}
