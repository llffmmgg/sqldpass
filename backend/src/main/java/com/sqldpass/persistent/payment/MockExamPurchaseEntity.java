package com.sqldpass.persistent.payment;

import java.time.LocalDateTime;

import com.sqldpass.persistent.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 특정 PREMIUM 모의고사의 잠금을 해제할 권리.
 * payment 검증이 통과하면 본 row 가 생성되며, MockExamService.getForUser 가
 * PREMIUM 가드 단계에서 본 행을 보고 통과 여부를 결정한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "mock_exam_purchase",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_mock_exam_purchase_member_exam",
                              columnNames = {"member_id", "mock_exam_id"})
        },
        indexes = {
            @Index(name = "idx_mock_exam_purchase_member", columnList = "member_id")
        })
public class MockExamPurchaseEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "mock_exam_id", nullable = false)
    private Long mockExamId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    public MockExamPurchaseEntity(Long memberId, Long mockExamId, Long paymentId, LocalDateTime purchasedAt) {
        this.memberId = memberId;
        this.mockExamId = mockExamId;
        this.paymentId = paymentId;
        this.purchasedAt = purchasedAt;
    }
}
