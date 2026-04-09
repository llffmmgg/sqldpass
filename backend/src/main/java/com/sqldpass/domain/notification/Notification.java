package com.sqldpass.domain.notification;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class Notification {

    private final Long id;
    private final Long memberId;
    private final String type;
    private final String title;
    private final String body;
    private final String link;
    private final Long refId;
    private final LocalDateTime readAt;
    private final LocalDateTime createdAt;

    public Notification(Long id, Long memberId, String type, String title, String body,
                        String link, Long refId, LocalDateTime readAt, LocalDateTime createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
        this.refId = refId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }
}
