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
     * 회원의 활성 구독을 강한 순(UNLIMITED → ONE_MONTH → THREE_DAY → FOCUS)으로 정렬해 반환.
     * SubscriptionService 가 첫 번째 결과를 활성 구독으로 사용.
     * archived 여부와 무관 — archived 는 통계 분리용일 뿐 권한엔 영향 없음.
     *
     * FOCUS 는 PASS+ 풀이 권한이 없으므로 PASS+ 보유 plan(THREE_DAY/ONE_MONTH/UNLIMITED) 보다 약함.
     * 정렬에서 누락하면 case 값이 null 로 풀려 ASC 시 1순위로 잡혀 Focus → Pro 업그레이드 직후
     * `allowsPremium=false` 로 잘못 보이는 정합성 버그가 발생한다.
     */
    @Query("select s from SubscriptionEntity s where s.memberId = :memberId " +
           "and (s.expiresAt is null or s.expiresAt > :now) " +
           "order by case s.plan " +
           "when 'UNLIMITED' then 0 " +
           "when 'ONE_MONTH' then 1 " +
           "when 'THREE_DAY' then 2 " +
           "when 'FOCUS' then 3 " +
           "end, s.expiresAt desc nulls first")
    List<SubscriptionEntity> findActiveByMemberId(@Param("memberId") Long memberId,
                                                  @Param("now") LocalDateTime now);

    /** 결제 row FK 로 구독 lookup — RTDN 환불 처리 시 revoke 대상 식별용. */
    java.util.Optional<SubscriptionEntity> findByPaymentId(Long paymentId);

    /** 어드민 목록용 — archived 제외. */
    Page<SubscriptionEntity> findByArchivedAtIsNull(Pageable pageable);
}
