package com.sqldpass.persistent.mockexam;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MockExamRepository extends JpaRepository<MockExamEntity, Long> {

    @Query("SELECT MAX(m.sequence) FROM MockExamEntity m")
    Optional<Integer> findMaxSequence();

    @Query("SELECT m FROM MockExamEntity m LEFT JOIN FETCH m.questions q LEFT JOIN FETCH q.subject s LEFT JOIN FETCH s.parent WHERE m.id = :id")
    Optional<MockExamEntity> findByIdWithQuestions(Long id);

    /** 목록 조회 — 한 번의 GROUP BY 쿼리로 exam + 문항 수 동시 조회 (N+1 방지) */
    @Query("SELECT m, COUNT(q) FROM MockExamEntity m LEFT JOIN m.questions q GROUP BY m ORDER BY m.sequence DESC")
    List<Object[]> findAllWithQuestionCounts();
}
