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

    /** 목록 조회 — 한 번의 GROUP BY 쿼리로 exam + 문항 수 + 난이도 통계 동시 조회 (N+1 방지) */
    @Query("SELECT m, COUNT(q), AVG(q.difficulty), MIN(q.difficulty), MAX(q.difficulty) " +
            "FROM MockExamEntity m LEFT JOIN m.questions q " +
            "GROUP BY m ORDER BY m.sequence DESC")
    List<Object[]> findAllWithQuestionCounts();
}
