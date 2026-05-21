package com.sqldpass.persistent.usage;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원별 일일 사용량 카운터. V94 daily_usage 매핑.
 * 활성 구독자는 서비스 레이어에서 row 생성 자체를 스킵 — 본 엔티티는 무료 회원만 보유.
 * 복합 PK (memberId, usageDate) 라 자정 경계에서 새 row 가 자동 생성되어 별도 리셋 cron 불필요.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "daily_usage", indexes = {
        @Index(name = "idx_daily_usage_date", columnList = "usage_date")
})
public class DailyUsageEntity {

    @EmbeddedId
    private DailyUsageId id;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "mock_session_count", nullable = false)
    private int mockSessionCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DailyUsageEntity(Long memberId, LocalDate usageDate) {
        LocalDateTime now = LocalDateTime.now();
        this.id = new DailyUsageId(memberId, usageDate);
        this.questionCount = 0;
        this.mockSessionCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getMemberId() {
        return id != null ? id.getMemberId() : null;
    }

    public LocalDate getUsageDate() {
        return id != null ? id.getUsageDate() : null;
    }

    public void addQuestionCount(int delta) {
        this.questionCount += delta;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementMockSession() {
        this.mockSessionCount += 1;
        this.updatedAt = LocalDateTime.now();
    }
}
