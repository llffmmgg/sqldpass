package com.sqldpass.persistent.payment;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * 일별 매출 / 환불 / 건수 — 어드민 통계 라인 차트용.
     *
     * <p>archived 구독에 연결된 결제는 제외. payment LEFT JOIN subscription 으로 묶고
     * (s.id IS NULL OR s.archived_at IS NULL) 필터 — 구독 row 없는 결제는 통과,
     * archived 구독에 연결된 결제만 제외.
     *
     * <p>환불은 status=CANCELLED 로 집계(PaymentService.revokePortOnePayment 가 환불 시 CANCELLED 마킹).
     *
     * <p>반환 Object[] 컬럼: [date(java.sql.Date), revenue(BigDecimal), refundAmount(BigDecimal), count(BigInteger)].
     */
    @Query(value = """
            SELECT
                DATE(p.paid_at)                                                          AS d,
                COALESCE(SUM(CASE WHEN p.status = 'PAID'      THEN p.amount ELSE 0 END), 0) AS revenue,
                COALESCE(SUM(CASE WHEN p.status = 'CANCELLED' THEN p.amount ELSE 0 END), 0) AS refund_amount,
                SUM(CASE WHEN p.status = 'PAID' THEN 1 ELSE 0 END)                       AS paid_count
            FROM payment p
            LEFT JOIN subscription s ON s.payment_id = p.id
            WHERE p.paid_at >= :since
              AND (s.id IS NULL OR s.archived_at IS NULL)
            GROUP BY DATE(p.paid_at)
            ORDER BY DATE(p.paid_at) ASC
            """, nativeQuery = true)
    List<Object[]> findDailyRevenue(@Param("since") LocalDateTime since);

    /**
     * 플랜별 PAID 분포 — 어드민 통계 막대 차트용.
     * archived 구독 연결 결제 제외. revenue DESC.
     *
     * <p>반환 Object[] 컬럼: [plan(String), count(BigInteger), revenue(BigDecimal)].
     */
    @Query(value = """
            SELECT
                p.plan                       AS plan,
                COUNT(*)                     AS paid_count,
                COALESCE(SUM(p.amount), 0)   AS revenue
            FROM payment p
            LEFT JOIN subscription s ON s.payment_id = p.id
            WHERE p.status = 'PAID'
              AND p.paid_at >= :since
              AND p.plan IS NOT NULL
              AND (s.id IS NULL OR s.archived_at IS NULL)
            GROUP BY p.plan
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> findRevenueByPlan(@Param("since") LocalDateTime since);

    /**
     * 일별 × provider 매출/환불/건수 — 어드민 채널별 분리 통계용.
     *
     * <p>{@link #findDailyRevenue} 의 provider 분리 버전. {@code GROUP BY DATE(p.paid_at), p.provider}
     * 로 같은 날짜라도 PORTONE/PLAY_BILLING/APP_STORE 별 row 가 분리된다.
     * archived 구독 연결 결제 제외 규칙은 그대로 유지.
     *
     * <p>V79 이전 옛 결제는 {@code p.provider} 가 NULL 일 수 있다. NULL row 도 그대로
     * 그룹에 포함되며, Service 매핑 단계에서 "PORTONE" 으로 보정한다.
     *
     * <p>반환 Object[] 컬럼: [date(java.sql.Date), provider(String), revenue(BigDecimal),
     * refundAmount(BigDecimal), count(BigInteger)].
     */
    @Query(value = """
            SELECT
                DATE(p.paid_at)                                                          AS d,
                p.provider                                                               AS provider,
                COALESCE(SUM(CASE WHEN p.status = 'PAID'      THEN p.amount ELSE 0 END), 0) AS revenue,
                COALESCE(SUM(CASE WHEN p.status = 'CANCELLED' THEN p.amount ELSE 0 END), 0) AS refund_amount,
                SUM(CASE WHEN p.status = 'PAID' THEN 1 ELSE 0 END)                       AS paid_count
            FROM payment p
            LEFT JOIN subscription s ON s.payment_id = p.id
            WHERE p.paid_at >= :since
              AND (s.id IS NULL OR s.archived_at IS NULL)
            GROUP BY DATE(p.paid_at), p.provider
            ORDER BY DATE(p.paid_at) ASC, p.provider ASC
            """, nativeQuery = true)
    List<Object[]> findDailyRevenueByProviderRaw(@Param("since") LocalDateTime since);

    /**
     * provider × plan 별 PAID 매출 분포 — 어드민 채널별 플랜 분포 차트용.
     *
     * <p>{@link #findRevenueByPlan} 의 provider 분리 버전. archived 제외 + plan NOT NULL 필터 동일.
     * NULL provider 도 그룹에 포함되며 Service 단계에서 "PORTONE" 으로 보정한다.
     *
     * <p>반환 Object[] 컬럼: [provider(String), plan(String), count(BigInteger), revenue(BigDecimal)].
     */
    @Query(value = """
            SELECT
                p.provider                   AS provider,
                p.plan                       AS plan,
                COUNT(*)                     AS paid_count,
                COALESCE(SUM(p.amount), 0)   AS revenue
            FROM payment p
            LEFT JOIN subscription s ON s.payment_id = p.id
            WHERE p.status = 'PAID'
              AND p.paid_at >= :since
              AND p.plan IS NOT NULL
              AND (s.id IS NULL OR s.archived_at IS NULL)
            GROUP BY p.provider, p.plan
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> findRevenueByProviderAndPlanRaw(@Param("since") LocalDateTime since);
}
