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

    /** 미니 회차 이름 부여용 — examType + kind 조합 카운트 ("SQLD 미니 모의고사 N회" 의 N) */
    long countByExamTypeAndKind(ExamType examType, MockExamKind kind);

    @Query("SELECT m FROM MockExamEntity m LEFT JOIN FETCH m.questions q LEFT JOIN FETCH q.subject s LEFT JOIN FETCH s.parent WHERE m.id = :id")
    Optional<MockExamEntity> findByIdWithQuestions(Long id);

    /** 어드민용 — 모든 visibility(DRAFT/PUBLISHED/PREMIUM) 포함. N+1 방지 GROUP BY */
    @Query("SELECT m, COUNT(q), AVG(q.difficulty), MIN(q.difficulty), MAX(q.difficulty) " +
            "FROM MockExamEntity m LEFT JOIN m.questions q " +
            "GROUP BY m ORDER BY m.sequence DESC")
    List<Object[]> findAllWithQuestionCounts();

    /**
     * 사용자용 — AI 모의고사 + 전문가 검수 완료 + (PUBLISHED 또는 PREMIUM). 기출 복원(PAST_EXAM) 은
     * /past-exams 전용. <strong>MINI</strong> 는 사용자 목록에서 제외 (별도 탭/페이지 도입 전까지 숨김).
     *
     * <p>정렬: <strong>PREMIUM(PASS+) 회차 먼저</strong>(결제 유도), 같은 그룹 안에서는 sequence DESC.
     * CASE WHEN 으로 visibility=PREMIUM 인 행에 정렬 키 0, 나머지 1 부여.
     */
    @Query("SELECT m, COUNT(q), AVG(q.difficulty), MIN(q.difficulty), MAX(q.difficulty) " +
            "FROM MockExamEntity m LEFT JOIN m.questions q " +
            "WHERE m.visibility <> com.sqldpass.persistent.mockexam.MockExamVisibility.DRAFT " +
            "  AND m.expertVerified = true " +
            "  AND m.kind = com.sqldpass.persistent.mockexam.MockExamKind.AI " +
            "GROUP BY m " +
            "ORDER BY CASE WHEN m.visibility = com.sqldpass.persistent.mockexam.MockExamVisibility.PREMIUM " +
            "              THEN 0 ELSE 1 END, " +
            "         m.sequence DESC")
    List<Object[]> findUserVisibleWithQuestionCounts();

    /**
     * 사용자용 — MINI 회차 전용. 정규(AI) 와 동일한 visibility/expertVerified 필터를 적용하되
     * kind=MINI 만 반환. /mini-mock-exams 페이지 / `GET /api/mock-exams/mini` (로그인) +
     * `GET /api/public/mock-exams/mini` (비로그인) 가 사용.
     *
     * <p>정렬은 정규와 동일 (PREMIUM 먼저 → sequence DESC) — 미니는 모두 PREMIUM 이라
     * 첫 정렬 키가 무차별이지만 정책 일관성을 위해 동일 식 유지.
     */
    @Query("SELECT m, COUNT(q), AVG(q.difficulty), MIN(q.difficulty), MAX(q.difficulty) " +
            "FROM MockExamEntity m LEFT JOIN m.questions q " +
            "WHERE m.visibility <> com.sqldpass.persistent.mockexam.MockExamVisibility.DRAFT " +
            "  AND m.expertVerified = true " +
            "  AND m.kind = com.sqldpass.persistent.mockexam.MockExamKind.MINI " +
            "GROUP BY m " +
            "ORDER BY CASE WHEN m.visibility = com.sqldpass.persistent.mockexam.MockExamVisibility.PREMIUM " +
            "              THEN 0 ELSE 1 END, " +
            "         m.sequence DESC")
    List<Object[]> findUserVisibleMiniWithQuestionCounts();

    /**
     * 안드로이드 앱 첫 부트 prefetch 용 스냅샷 — visibility != DRAFT + expert_verified=true 인
     * 모든 회차(AI/PAST_EXAM 모두 포함)를 문제 + 과목까지 한 번에 fetch.
     * MINI 는 사용자 노출 차단되므로 스냅샷에도 포함하지 않는다.
     * 결과는 단일 큰 JSON 으로 직렬화돼 IndexedDB 에 들어간다.
     */
    @Query("SELECT DISTINCT m FROM MockExamEntity m " +
            "LEFT JOIN FETCH m.questions q " +
            "LEFT JOIN FETCH q.subject s " +
            "LEFT JOIN FETCH s.parent " +
            "WHERE m.visibility <> com.sqldpass.persistent.mockexam.MockExamVisibility.DRAFT " +
            "  AND m.expertVerified = true " +
            "  AND m.kind <> com.sqldpass.persistent.mockexam.MockExamKind.MINI")
    List<MockExamEntity> findAllForSnapshot();

    /** 스냅샷 버전 계산용 — visibility != DRAFT + expert_verified 회차의 max(updated_at). MINI 는 제외. */
    @Query("SELECT MAX(m.updatedAt) FROM MockExamEntity m " +
            "WHERE m.visibility <> com.sqldpass.persistent.mockexam.MockExamVisibility.DRAFT " +
            "  AND m.expertVerified = true " +
            "  AND m.kind <> com.sqldpass.persistent.mockexam.MockExamKind.MINI")
    Optional<java.time.LocalDateTime> findSnapshotMaxUpdatedAt();

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
