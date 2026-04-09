package com.sqldpass.domain.notice;

import java.time.LocalDateTime;

import com.sqldpass.persistent.notice.NoticeDisplayType;

import lombok.Getter;

@Getter
public class SiteNotice {

    private final Long id;
    private final NoticeDisplayType displayType;
    private final String title;
    private final String body;
    private final boolean active;
    private final int version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public SiteNotice(Long id, NoticeDisplayType displayType, String title, String body,
                      boolean active, int version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.displayType = displayType;
        this.title = title;
        this.body = body;
        this.active = active;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
