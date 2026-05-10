package com.sqldpass.persistent.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistoryEntity, Long> {

    /** 회원별 이력을 최신 발생순으로 조회. */
    List<SubscriptionHistoryEntity> findByMemberIdOrderByOccurredAtDesc(Long memberId);
}
