package com.sqldpass.persistent.solve;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveAnswerRepository extends JpaRepository<SolveAnswerEntity, Long> {

    @Query(value = """
            SELECT sa.question_id AS questionId,
                   q.content AS questionContent,
                   s2.name AS subjectName,
                   COUNT(*) AS wrongCount,
                   MAX(sa.created_at) AS lastWrongAt
            FROM solve_answer sa
            JOIN question q ON sa.question_id = q.id
            JOIN subject s2 ON q.subject_id = s2.id
            JOIN solve s ON sa.solve_id = s.id
            WHERE s.member_id = :memberId
              AND sa.is_correct = false
              AND (:subjectId IS NULL OR q.subject_id = :subjectId)
            GROUP BY sa.question_id, q.content, s2.name
            ORDER BY wrongCount DESC
            """, nativeQuery = true)
    List<WrongAnswerProjection> findWrongAnswers(@Param("memberId") Long memberId, @Param("subjectId") Long subjectId);

    @Query(value = """
            SELECT q.subject_id AS subjectId,
                   s2.name AS subjectName,
                   COUNT(*) AS totalSolved,
                   SUM(CASE WHEN sa.is_correct = false THEN 1 ELSE 0 END) AS wrongCount,
                   ROUND(SUM(CASE WHEN sa.is_correct = false THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) AS wrongRate
            FROM solve_answer sa
            JOIN question q ON sa.question_id = q.id
            JOIN subject s2 ON q.subject_id = s2.id
            JOIN solve s ON sa.solve_id = s.id
            WHERE s.member_id = :memberId
            GROUP BY q.subject_id, s2.name
            ORDER BY wrongRate DESC
            """, nativeQuery = true)
    List<WrongAnswerStatsProjection> findWrongAnswerStats(@Param("memberId") Long memberId);
}
