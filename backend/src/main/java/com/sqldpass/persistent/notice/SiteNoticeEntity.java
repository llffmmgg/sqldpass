package com.sqldpass.persistent.notice;

import com.sqldpass.persistent.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "site_notice", indexes = {
        @Index(name = "idx_site_notice_active", columnList = "display_type, active, updated_at")
})
public class SiteNoticeEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_type", nullable = false, length = 16)
    private NoticeDisplayType displayType;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int version;

    public SiteNoticeEntity(NoticeDisplayType displayType, String title, String body, boolean active) {
        this.displayType = displayType;
        this.title = title;
        this.body = body;
        this.active = active;
        this.version = 1;
    }

    public void update(NoticeDisplayType displayType, String title, String body, boolean active) {
        this.displayType = displayType;
        this.title = title;
        this.body = body;
        this.active = active;
        this.version += 1;
    }

    public void changeActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            this.version += 1;
        }
    }
}
