package com.sqldpass.persistent.payment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    /**
     * 회원의 활성 구독을 강한 순(UNLIMITED → ONE_MONTH → THREE_DAY)으로 정렬해 반환.
     * SubscriptionService 가 첫 번째 결과를 활성 구독으로 사용.
     * archived 여부와 무관 — archived 는 통계 분리용일 뿐 권한엔 영향 없음.
     */
    @Query("select s from SubscriptionEntity s where s.memberId = :memberId " +
           "and (s.expiresAt is null or s.expiresAt > :now) " +
           "order by case s.plan when 'UNLIMITED' then 0 when 'ONE_MONTH' then 1 when 'THREE_DAY' then 2 end, " +
           "s.expiresAt desc nulls first")
    List<SubscriptionEntity> findActiveByMemberId(@Param("memberId") Long memberId,
                                                  @Param("now") LocalDateTime now);

    /** 결제 row FK 로 구독 lookup — RTDN 환불 처리 시 revoke 대상 식별용. */
    java.util.Optional<SubscriptionEntity> findByPaymentId(Long paymentId);

    /** 어드민 목록용 — archived 제외. */
    Page<SubscriptionEntity> findByArchivedAtIsNull(Pageable pageable);
}
