package com.sqldpass.controller.admin.dto;

import com.sqldpass.persistent.notice.NoticeDisplayType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateNoticeRequest(
        @NotNull NoticeDisplayType displayType,
        @Size(max = 200) String title,
        @NotBlank String body,
        boolean active
) {
}
