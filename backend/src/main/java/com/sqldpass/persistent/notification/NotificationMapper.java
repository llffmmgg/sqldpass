package com.sqldpass.persistent.notification;

import com.sqldpass.domain.notification.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static Notification toDomain(NotificationEntity e) {
        return new Notification(
                e.getId(),
                e.getMemberId(),
                e.getType(),
                e.getTitle(),
                e.getBody(),
                e.getLink(),
                e.getRefId(),
                e.getReadAt(),
                e.getCreatedAt());
    }
}
