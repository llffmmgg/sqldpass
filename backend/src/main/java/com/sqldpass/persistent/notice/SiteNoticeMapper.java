package com.sqldpass.persistent.notice;

import com.sqldpass.domain.notice.SiteNotice;

public final class SiteNoticeMapper {

    private SiteNoticeMapper() {
    }

    public static SiteNotice toDomain(SiteNoticeEntity entity) {
        return new SiteNotice(
                entity.getId(),
                entity.getDisplayType(),
                entity.getTitle(),
                entity.getBody(),
                entity.isActive(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
