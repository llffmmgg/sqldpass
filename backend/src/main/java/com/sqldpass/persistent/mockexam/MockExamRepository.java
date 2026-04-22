package com.sqldpass.persistent.mockexam;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MockExamRepository extends JpaRepository<MockExamEntity, Long> {

    /** 자격증 유형별 MAX sequence — 자격증마다 1,2,3 독립 번호 부여에 사용 */
    @Query("SELECT MAX(m.sequence) FROM MockExamEntity m WHERE m.examType = :examType")
    Optional<Integer> findMaxSequenceByExamType(@Param("examType") ExamType examType);

    @Query("SELECT m FROM MockExamEntity m LEFT JOIN FETCH m.questions q LEFT JOIN FETCH q.subject s LEFT JOIN FETCH s.parent WHERE m.id = :id")
    Optional<MockExamEntity> findByIdWithQuestions(Long id);

    /** 어드민용 — 모든 visibility(DRAFT/PUBLISHED/PREMIUM) 포함. N+1 방지 GROUP BY */
    @Query("SELECT m, COUNT(q), AVG(q.difficulty), MIN(q.difficulty), MAX(q.difficulty) " +
            "FROM MockExamEntity m LEFT JOIN m.questions q " +
            "GROUP BY m ORDER BY m.sequence DESC")
    List<Object[]> findAllWithQuestionCounts();

    /** 사용자용 — 전문가 검수 완료 + (PUBLISHED 또는 PREMIUM). PREMIUM은 프론트에서 잠금 */
    @Query("SELECT m, COUNT(q), AVG(q.difficulty), MIN(q.difficulty), MAX(q.difficulty) " +
            "FROM MockExamEntity m LEFT JOIN m.questions q " +
            "WHERE m.visibility <> com.sqldpass.persistent.mockexam.MockExamVisibility.DRAFT " +
            "  AND m.expertVerified = true " +
            "GROUP BY m ORDER BY m.sequence DESC")
    List<Object[]> findUserVisibleWithQuestionCounts();
}
