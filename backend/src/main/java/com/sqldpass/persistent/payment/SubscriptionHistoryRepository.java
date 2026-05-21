package com.sqldpass.persistent.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistoryEntity, Long> {

    /** 회원별 이력을 최신 발생순으로 조회. */
    List<SubscriptionHistoryEntity> findByMemberIdOrderByOccurredAtDesc(Long memberId);

    @Modifying
    @Query("update SubscriptionHistoryEntity h set h.memberId = null where h.memberId = :memberId")
    int nullifyMember(@Param("memberId") Long memberId);
}
