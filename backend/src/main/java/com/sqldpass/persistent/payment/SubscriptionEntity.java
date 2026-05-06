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
 * UNLIMITED 는 expiresAt = null (평생).
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

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private SubscriptionPlan plan;

    /** 결제 row FK. */
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    /** UNLIMITED 면 null. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

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
}
