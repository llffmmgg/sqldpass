package com.sqldpass.controller.notice.dto;

import java.time.LocalDateTime;

import com.sqldpass.domain.notice.SiteNotice;
import com.sqldpass.persistent.notice.NoticeDisplayType;

public record NoticeResponse(
        Long id,
        NoticeDisplayType displayType,
        String title,
        String body,
        boolean active,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeResponse from(SiteNotice n) {
        return new NoticeResponse(
                n.getId(),
                n.getDisplayType(),
                n.getTitle(),
                n.getBody(),
                n.isActive(),
                n.getVersion(),
                n.getCreatedAt(),
                n.getUpdatedAt());
    }
}
