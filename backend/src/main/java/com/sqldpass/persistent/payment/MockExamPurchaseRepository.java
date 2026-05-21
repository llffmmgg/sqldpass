package com.sqldpass.persistent.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MockExamPurchaseRepository extends JpaRepository<MockExamPurchaseEntity, Long> {

    boolean existsByMemberIdAndMockExamId(Long memberId, Long mockExamId);

    @Query("select p.mockExamId from MockExamPurchaseEntity p where p.memberId = :memberId")
    List<Long> findMockExamIdsByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("update MockExamPurchaseEntity p set p.memberId = null where p.memberId = :memberId")
    int nullifyMember(@Param("memberId") Long memberId);
}
