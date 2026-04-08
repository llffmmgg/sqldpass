package com.sqldpass.persistent.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    Page<FeedbackEntity> findByStatus(FeedbackStatus status, Pageable pageable);

    Page<FeedbackEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
