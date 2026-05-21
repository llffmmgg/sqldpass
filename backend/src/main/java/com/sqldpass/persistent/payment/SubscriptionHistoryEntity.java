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
 * 구독 이력 감사(audit) row. 환불·만료·관리자 발급/회수 흔적을 보존한다.
 * expireManual 의 delete 패턴을 revoke + history insert 로 대체하기 위한 기반.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "subscription_history",
        indexes = {
            @Index(name = "idx_history_member_occurred", columnList = "member_id,occurred_at")
        })
public class SubscriptionHistoryEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private SubscriptionHistoryAction action;

    @Column(name = "reason", length = 500)
    private String reason;

    /** 운영자 수동 조작 시 admin id. RTDN 자동 환불 등은 null. */
    @Column(name = "actor_admin_id")
    private Long actorAdminId;

    /** 연관된 결제 row id. 어드민 수동 발급/회수처럼 결제와 무관한 경우 null. */
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public SubscriptionHistoryEntity(Long memberId, SubscriptionPlan plan,
                                     SubscriptionHistoryAction action, String reason,
                                     Long actorAdminId, Long paymentId,
                                     LocalDateTime occurredAt) {
        this.memberId = memberId;
        this.plan = plan;
        this.action = action;
        this.reason = reason;
        this.actorAdminId = actorAdminId;
        this.paymentId = paymentId;
        this.occurredAt = occurredAt;
    }
}
