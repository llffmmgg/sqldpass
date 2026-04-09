package com.sqldpass.controller.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFeedbackReplyRequest(
        @NotBlank @Size(max = 2000) String reply
) {
}
