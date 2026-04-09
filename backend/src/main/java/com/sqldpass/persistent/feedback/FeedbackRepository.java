package com.sqldpass.persistent.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    Page<FeedbackEntity> findByStatus(FeedbackStatus status, Pageable pageable);

    Page<FeedbackEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Query("update FeedbackEntity f set f.memberId = null where f.memberId = :memberId")
    int nullifyMember(@Param("memberId") Long memberId);
}
