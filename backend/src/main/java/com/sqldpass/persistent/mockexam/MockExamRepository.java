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

    /** 사용자용 — AI 모의고사 + 전문가 검수 완료 + (PUBLISHED 또는 PREMIUM). 기출 복원(PAST_EXAM) 은 /past-exams 전용 */
    @Query("SELECT m, COUNT(q), AVG(q.difficulty), MIN(q.difficulty), MAX(q.difficulty) " +
            "FROM MockExamEntity m LEFT JOIN m.questions q " +
            "WHERE m.visibility <> com.sqldpass.persistent.mockexam.MockExamVisibility.DRAFT " +
            "  AND m.expertVerified = true " +
            "  AND m.kind = com.sqldpass.persistent.mockexam.MockExamKind.AI " +
            "GROUP BY m ORDER BY m.sequence DESC")
    List<Object[]> findUserVisibleWithQuestionCounts();

    /**
     * 기출 복원 목록 — 비로그인 공개.
     * kind=PAST_EXAM 이면서 PUBLISHED(=일반 공개) + 전문가 검수 완료만 노출.
     * 회차(exam_round) 내림차순 → 없으면 sequence 내림차순으로 최신 회차가 상단.
     */
    @Query("SELECT m, COUNT(q), AVG(q.difficulty), MIN(q.difficulty), MAX(q.difficulty) " +
            "FROM MockExamEntity m LEFT JOIN m.questions q " +
            "WHERE m.examType = :examType " +
            "  AND m.kind = com.sqldpass.persistent.mockexam.MockExamKind.PAST_EXAM " +
            "  AND m.visibility = com.sqldpass.persistent.mockexam.MockExamVisibility.PUBLISHED " +
            "  AND m.expertVerified = true " +
            "GROUP BY m " +
            "ORDER BY COALESCE(m.examYear, 0) DESC, COALESCE(m.examRound, 0) DESC, m.sequence DESC")
    List<Object[]> findPublicPastExams(@Param("examType") ExamType examType);
}
