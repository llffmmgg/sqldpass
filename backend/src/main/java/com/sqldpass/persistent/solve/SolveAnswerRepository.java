package com.sqldpass.persistent.solve;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveAnswerRepository extends JpaRepository<SolveAnswerEntity, Long> {

    /**
     * 오답 노트 — "마지막 풀이가 틀린 문제" 정의.
     *
     * 각 question_id별로 사용자의 가장 최근 SolveAnswer를 찾고, 그것이 is_correct=false인 문제들만 노출.
     * → 다시 풀어서 맞히면 자동으로 목록에서 사라짐 (마스터 완료)
     * → 누적 wrongCount는 이력 표시용으로 함께 반환
     */
    @Query(value = """
            SELECT last_sa.question_id AS questionId,
                   q.content AS questionContent,
                   q.question_type AS questionType,
                   q.subject_id AS subjectId,
                   s2.name AS subjectName,
                   (
                       SELECT COUNT(*) FROM solve_answer wcsa
                       JOIN solve wcs ON wcsa.solve_id = wcs.id
                       WHERE wcs.member_id = :memberId
                         AND wcsa.question_id = last_sa.question_id
                         AND wcsa.is_correct = false
                   ) AS wrongCount,
                   last_sa.created_at AS lastWrongAt
            FROM solve_answer last_sa
            JOIN solve s ON last_sa.solve_id = s.id
            JOIN question q ON last_sa.question_id = q.id
            JOIN subject s2 ON q.subject_id = s2.id
            WHERE s.member_id = :memberId
              AND last_sa.is_correct = false
              AND last_sa.id = (
                  SELECT MAX(sa2.id) FROM solve_answer sa2
                  JOIN solve s2x ON sa2.solve_id = s2x.id
                  WHERE s2x.member_id = :memberId AND sa2.question_id = last_sa.question_id
              )
              AND (:subjectId IS NULL OR q.subject_id = :subjectId)
            ORDER BY wrongCount DESC, last_sa.created_at DESC
            """, nativeQuery = true)
    List<WrongAnswerProjection> findWrongAnswers(@Param("memberId") Long memberId, @Param("subjectId") Long subjectId);

    /**
     * 과목별 취약 영역 통계 — "마지막 풀이가 틀린 문제" 정의 동일.
     *
     * - totalSolved: 사용자가 해당 과목에서 한 번이라도 시도한 distinct 문제 수 (마지막 풀이 기준)
     * - wrongCount: 그 중 마지막 풀이가 틀린 문제 수 (= 현재 오답노트 노출 수)
     * - wrongRate: wrongCount / totalSolved * 100
     */
    @Query(value = """
            SELECT q.subject_id AS subjectId,
                   s2.name AS subjectName,
                   COUNT(*) AS totalSolved,
                   SUM(CASE WHEN last_sa.is_correct = false THEN 1 ELSE 0 END) AS wrongCount,
                   ROUND(SUM(CASE WHEN last_sa.is_correct = false THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) AS wrongRate
            FROM solve_answer last_sa
            JOIN solve s ON last_sa.solve_id = s.id
            JOIN question q ON last_sa.question_id = q.id
            JOIN subject s2 ON q.subject_id = s2.id
            WHERE s.member_id = :memberId
              AND last_sa.id = (
                  SELECT MAX(sa2.id) FROM solve_answer sa2
                  JOIN solve s2x ON sa2.solve_id = s2x.id
                  WHERE s2x.member_id = :memberId AND sa2.question_id = last_sa.question_id
              )
            GROUP BY q.subject_id, s2.name
            ORDER BY wrongRate DESC
            """, nativeQuery = true)
    List<WrongAnswerStatsProjection> findWrongAnswerStats(@Param("memberId") Long memberId);

    /**
     * 인사이트 — 오답률 best N (과목별).
     *
     * 1) "공부한 학생" 만 통계에 포함 — solve.total_count 합 ≥ :minMemberAttempts
     *    AND 평균 정답률 (correct_count / total_count) × 100 ≥ :minMemberAccuracy
     *    → 운빨로 한두 문제만 풀고 던진 사용자 제외
     * 2) 그 학생들의 답안만 question 단위로 그룹화
     * 3) 과목 (q.subject_id) 필터 + 최소 시도수 (HAVING) + 오답률 desc
     *
     * 인덱스 활용: solve.member_id, solve_answer.solve_id, question.subject_id (모두 기존)
     * 캐시: InsightsService 의 Caffeine (TTL 1시간) 가 DB hit 을 차단.
     */
    @Query(value = """
            SELECT q.id                                                                 AS questionId,
                   q.subject_id                                                         AS subjectId,
                   s2.name                                                              AS subjectName,
                   q.content                                                            AS questionContent,
                   COUNT(*)                                                             AS attempts,
                   SUM(CASE WHEN sa.is_correct = false THEN 1 ELSE 0 END)               AS wrongCount,
                   ROUND(SUM(CASE WHEN sa.is_correct = false THEN 1 ELSE 0 END) * 100.0
                         / COUNT(*), 1)                                                 AS wrongRate
            FROM solve_answer sa
            JOIN solve s         ON s.id = sa.solve_id
            JOIN (
                SELECT s2.member_id
                FROM solve s2
                GROUP BY s2.member_id
                HAVING SUM(s2.total_count) >= :minMemberAttempts
                   AND SUM(s2.correct_count) * 100.0 / SUM(s2.total_count) >= :minMemberAccuracy
            ) qm                 ON qm.member_id = s.member_id
            JOIN question q      ON q.id = sa.question_id
            JOIN subject s2      ON s2.id = q.subject_id
            WHERE q.subject_id = :subjectId
            GROUP BY q.id, q.subject_id, s2.name, q.content
            HAVING COUNT(*) >= :minQuestionAttempts
            ORDER BY wrongRate DESC, attempts DESC
            LIMIT :topN
            """, nativeQuery = true)
    List<HardestQuestionProjection> findHardestQuestions(
            @Param("subjectId") Long subjectId,
            @Param("minMemberAttempts") int minMemberAttempts,
            @Param("minMemberAccuracy") int minMemberAccuracy,
            @Param("minQuestionAttempts") int minQuestionAttempts,
            @Param("topN") int topN);
}
