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
import jakarta.persistence.Lob;

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
            @Index(name = "idx_payment_mock_exam", columnList = "mock_exam_id")
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

    @Column(name = "amount", nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /** PortOne 검증 응답 원본(JSON). 디버깅·환불 분쟁 대비. */
    @Lob
    @Column(name = "pg_response")
    private String pgResponse;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public PaymentEntity(String paymentId, Long memberId, Long mockExamId,
                         String productName, int amount) {
        this.paymentId = paymentId;
        this.memberId = memberId;
        this.mockExamId = mockExamId;
        this.productName = productName;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
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
