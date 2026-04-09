package com.sqldpass.controller.notification.dto;

import java.time.LocalDateTime;

import com.sqldpass.domain.notification.Notification;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        String link,
        Long refId,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getLink(),
                n.getRefId(),
                n.getReadAt(),
                n.getCreatedAt());
    }
}
