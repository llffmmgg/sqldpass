package com.sqldpass.persistent.question;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestionVerificationRunRepository extends JpaRepository<QuestionVerificationRunEntity, Long> {

    @Query("""
            SELECT r FROM QuestionVerificationRunEntity r
            LEFT JOIN FETCH r.subject s
            ORDER BY r.completedAt DESC
            """)
    List<QuestionVerificationRunEntity> findRecentRuns(Pageable pageable);
}
