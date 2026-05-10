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
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payment",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_payment_payment_id", columnNames = {"payment_id"})
        },
        indexes = {
            @Index(name = "idx_payment_member_status", columnList = "member_id,status"),
            @Index(name = "idx_payment_mock_exam", columnList = "mock_exam_id"),
            @Index(name = "idx_payment_purchase_token", columnList = "purchase_token")
        })
public class PaymentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PortOne paymentId — client 가 결제창 호출 시 사용한 멱등 키. */
    @Column(name = "payment_id", nullable = false, length = 80)
    private String paymentId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 잠금 해제 대상 모의고사. 단건 결제 흐름에서만 사용. 향후 패스 도입 시 nullable 유지. */
    @Column(name = "mock_exam_id")
    private Long mockExamId;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    /** 구독 plan — prepare 시 채워짐. 옛 단일 회차 결제는 null. */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", length = 20)
    private SubscriptionPlan plan;

    /** 실제 결제 금액 (PG 청구액). 업그레이드 prorate 차감 후의 값. */
    @Column(name = "amount", nullable = false)
    private int amount;

    /** plan 의 정가 (PaymentProperties.PlanConfig.amount). 회계 보존용. */
    @Column(name = "base_amount", nullable = false)
    private int baseAmount;

    /** 업그레이드 prorate 차감액 (없으면 0). amount = baseAmount - prorateDiscount. */
    @Column(name = "prorate_discount", nullable = false)
    private int prorateDiscount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /** 결제 채널 — PORTONE(웹) | PLAY_BILLING(안드로이드). 기본값은 PortOne(레거시 호환). */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private PaymentProvider provider = PaymentProvider.PORTONE;

    /** Google Play Billing 영수증 토큰. PORTONE 결제에선 null. RTDN webhook lookup 키. */
    @Column(name = "purchase_token", length = 512)
    private String purchaseToken;

    /** PortOne 검증 응답 원본(JSON). 디버깅·환불 분쟁 대비. */
    @Column(name = "pg_response", columnDefinition = "TEXT")
    private String pgResponse;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public PaymentEntity(String paymentId, Long memberId, Long mockExamId,
                         String productName, int amount) {
        this(paymentId, memberId, mockExamId, productName, null, amount, amount, 0);
    }

    public PaymentEntity(String paymentId, Long memberId, Long mockExamId,
                         String productName, SubscriptionPlan plan, int amount) {
        this(paymentId, memberId, mockExamId, productName, plan, amount, amount, 0);
    }

    public PaymentEntity(String paymentId, Long memberId, Long mockExamId,
                         String productName, SubscriptionPlan plan,
                         int amount, int baseAmount, int prorateDiscount) {
        this(paymentId, memberId, mockExamId, productName, plan,
                amount, baseAmount, prorateDiscount, PaymentProvider.PORTONE, null);
    }

    public PaymentEntity(String paymentId, Long memberId, Long mockExamId,
                         String productName, SubscriptionPlan plan,
                         int amount, int baseAmount, int prorateDiscount,
                         PaymentProvider provider, String purchaseToken) {
        this.paymentId = paymentId;
        this.memberId = memberId;
        this.mockExamId = mockExamId;
        this.productName = productName;
        this.plan = plan;
        this.amount = amount;
        this.baseAmount = baseAmount;
        this.prorateDiscount = prorateDiscount;
        this.status = PaymentStatus.PENDING;
        this.provider = provider != null ? provider : PaymentProvider.PORTONE;
        this.purchaseToken = purchaseToken;
    }

    public void markPaid(String pgResponse, LocalDateTime paidAt) {
        this.status = PaymentStatus.PAID;
        this.pgResponse = pgResponse;
        this.paidAt = paidAt;
    }

    public void markFailed(String pgResponse) {
        this.status = PaymentStatus.FAILED;
        this.pgResponse = pgResponse;
    }

    public void markCancelled(String pgResponse) {
        this.status = PaymentStatus.CANCELLED;
        this.pgResponse = pgResponse;
    }
}
