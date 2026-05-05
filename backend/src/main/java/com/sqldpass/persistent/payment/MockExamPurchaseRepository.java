package com.sqldpass.persistent.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamPurchaseRepository extends JpaRepository<MockExamPurchaseEntity, Long> {

    boolean existsByMemberIdAndMockExamId(Long memberId, Long mockExamId);
}
