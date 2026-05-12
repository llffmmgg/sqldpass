package com.sqldpass.persistent.payment;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sqldpass.controller.admin.AdminPaymentRow;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByPaymentId(String paymentId);

    /** Google Play Billing RTDN webhook 이 token 으로 결제 row 찾을 때 사용. */
    Optional<PaymentEntity> findByPurchaseToken(String purchaseToken);

    /**
     * 어드민 결제 목록 조회 — Member LEFT JOIN 으로 nickname 같이 가져오고,
     * status/provider/nickname/paymentId 4종 필터를 옵셔널 적용.
     *
     * <p>nickname/paymentId 는 LIKE, 정렬은 paidAt DESC (MySQL 은 DESC 에서 NULL 을 자동
     * LAST 배치) + id DESC tiebreak.
     *
     * <p>{@code supersededByNewerPayment} 는 같은 회원의 더 최신 PAID 결제 중 subscription 이
     * 활성(expires_at IS NULL 이거나 미래) 인 것이 존재하면 true. UI 가 환불 버튼 disable 처리해
     * 옛 결제 환불 + 활성 구독 그대로 사고를 차단한다.
     */
    @Query("""
            SELECT new com.sqldpass.controller.admin.AdminPaymentRow(
                    p.id, p.paymentId, p.memberId, m.nickname,
                    p.plan, p.amount, p.baseAmount, p.prorateDiscount,
                    p.status, p.provider,
                    p.buyerName, p.buyerEmail, p.buyerPhoneNumber,
                    p.paidAt, p.createdAt,
                    CASE WHEN EXISTS (
                        SELECT 1 FROM PaymentEntity p2
                        WHERE p2.memberId = p.memberId
                          AND p2.id <> p.id
                          AND p2.status = com.sqldpass.persistent.payment.PaymentStatus.PAID
                          AND p2.paidAt > p.paidAt
                          AND EXISTS (
                              SELECT 1 FROM SubscriptionEntity s
                              WHERE s.paymentId = p2.id
                                AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
                          )
                    ) THEN TRUE ELSE FALSE END
            )
            FROM PaymentEntity p LEFT JOIN MemberEntity m ON m.id = p.memberId
            WHERE (:status IS NULL OR p.status = :status)
              AND (:provider IS NULL OR p.provider = :provider)
              AND (:nickname IS NULL OR m.nickname LIKE %:nickname%)
              AND (:paymentIdLike IS NULL OR p.paymentId LIKE %:paymentIdLike%)
            ORDER BY p.paidAt DESC, p.id DESC
            """)
    Page<AdminPaymentRow> findAdminPage(@Param("status") PaymentStatus status,
                                        @Param("provider") PaymentProvider provider,
                                        @Param("nickname") String nickname,
                                        @Param("paymentIdLike") String paymentIdLike,
                                        Pageable pageable);

    /**
     * 환불 가드 — 같은 회원에게 본 결제보다 더 최신 PAID 결제가 있고 그 결제의 subscription 이
     * 활성이면 본 결제는 superseded 상태. revokePortOnePayment 가 호출 전에 검사해
     * 옛 결제 환불 사고를 차단한다.
     */
    @Query("""
            SELECT CASE WHEN COUNT(p2) > 0 THEN true ELSE false END
            FROM PaymentEntity p2
            WHERE p2.memberId = :memberId
              AND p2.id <> :paymentId
              AND p2.status = com.sqldpass.persistent.payment.PaymentStatus.PAID
              AND p2.paidAt > :paidAt
              AND EXISTS (
                  SELECT 1 FROM SubscriptionEntity s
                  WHERE s.paymentId = p2.id
                    AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
              )
            """)
    boolean existsNewerActivePaidPaymentForMember(@Param("memberId") Long memberId,
                                                  @Param("paymentId") Long paymentId,
                                                  @Param("paidAt") LocalDateTime paidAt);
}
