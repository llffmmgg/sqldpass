package com.sqldpass.persistent.payment;

import java.time.LocalDateTime;

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

/**
 * 회원의 구독권 row. 단발 결제 → expiresAt = now + plan.days.
 * UNLIMITED 는 paidAt + 181일 00:00 KR 만료 (6개월 정책).
 * 단 기존 데이터에 expiresAt=NULL 로 저장된 평생권은 그대로 활성 유지된다.
 * 한 회원이 다건 보유 가능 (구매 누적). SubscriptionService 에서 가장 강한 plan 우선 적용.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "subscription",
        indexes = {
            @Index(name = "idx_subscription_member_expires", columnList = "member_id,expires_at")
        })
public class SubscriptionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private SubscriptionPlan plan;

    /** 결제 row FK. 어드민 수동 발급(보상·이벤트·환불 후 재발급) 시엔 null. */
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    /** UNLIMITED 면 null. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 운영자가 통계 집계에서 분리한 시점. NULL = 정상.
     * 활성 구독은 archive 거부 — 만료된 row 만 archived 상태가 됨(테스트 결제 정리 용도).
     * 권한 판정(isActive)에는 영향 없음 — 통계 분리 전용 마커.
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public SubscriptionEntity(Long memberId, SubscriptionPlan plan, Long paymentId,
                              LocalDateTime purchasedAt, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.plan = plan;
        this.paymentId = paymentId;
        this.purchasedAt = purchasedAt;
        this.expiresAt = expiresAt;
    }

    /** now 기준 활성 여부. UNLIMITED 또는 expiresAt > now. */
    public boolean isActive(LocalDateTime now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    /**
     * 환불·강제 만료 — Play Billing RTDN(REFUND) 또는 어드민 보상 회수 시 호출.
     * UNLIMITED 도 expiresAt = now 로 강제해 즉시 비활성화한다.
     */
    public void revoke(LocalDateTime now) {
        this.expiresAt = now;
    }

    /** 만료된 구독을 통계 집계에서 분리. 호출 측에서 isActive 거부 확인 필수. */
    public void archive(LocalDateTime now) {
        this.archivedAt = now;
    }
}
