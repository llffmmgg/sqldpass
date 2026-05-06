package com.sqldpass.persistent.payment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    /**
     * 회원의 활성 구독을 강한 순(UNLIMITED → ONE_MONTH → THREE_DAY)으로 정렬해 반환.
     * SubscriptionService 가 첫 번째 결과를 활성 구독으로 사용.
     */
    @Query("select s from SubscriptionEntity s where s.memberId = :memberId " +
           "and (s.expiresAt is null or s.expiresAt > :now) " +
           "order by case s.plan when 'UNLIMITED' then 0 when 'ONE_MONTH' then 1 when 'THREE_DAY' then 2 end, " +
           "s.expiresAt desc nulls first")
    List<SubscriptionEntity> findActiveByMemberId(@Param("memberId") Long memberId,
                                                  @Param("now") LocalDateTime now);
}
