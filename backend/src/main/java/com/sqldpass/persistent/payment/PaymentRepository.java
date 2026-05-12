package com.sqldpass.persistent.payment;

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
     * <p>nickname 은 LIKE, paymentId 는 LIKE — 검색 input 의 자동 분기는 프론트가 담당.
     * 정렬은 paidAt DESC (MySQL 은 DESC 에서 NULL 을 자동 LAST 로 배치) + id DESC tiebreak.
     */
    @Query("""
            SELECT new com.sqldpass.controller.admin.AdminPaymentRow(
                    p.id, p.paymentId, p.memberId, m.nickname,
                    p.plan, p.amount, p.baseAmount, p.prorateDiscount,
                    p.status, p.provider,
                    p.buyerName, p.buyerEmail, p.buyerPhoneNumber,
                    p.paidAt, p.createdAt
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
}
