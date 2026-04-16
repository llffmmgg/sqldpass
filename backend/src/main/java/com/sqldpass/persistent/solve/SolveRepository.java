package com.sqldpass.persistent.solve;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveRepository extends JpaRepository<SolveEntity, Long> {

    /** 어드민 풀이 상세 — answers, question, subject 까지 한 번에 로딩 */
    @Query("SELECT DISTINCT s FROM SolveEntity s "
            + "LEFT JOIN FETCH s.answers a "
            + "LEFT JOIN FETCH a.question q "
            + "LEFT JOIN FETCH q.subject sub "
            + "LEFT JOIN FETCH sub.parent "
            + "LEFT JOIN FETCH s.subject "
            + "LEFT JOIN FETCH s.mockExam "
            + "LEFT JOIN FETCH s.member "
            + "WHERE s.id = :id")
    java.util.Optional<SolveEntity> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT s FROM SolveEntity s "
            + "LEFT JOIN FETCH s.subject subj "
            + "LEFT JOIN FETCH subj.parent "
            + "LEFT JOIN FETCH s.mockExam "
            + "WHERE s.member.id = :memberId "
            + "ORDER BY s.createdAt DESC")
    List<SolveEntity> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    /**
     * 회원 탈퇴 시 사용. orphanRemoval=true 가 적용된 SolveAnswerEntity 를 cascade 시키기 위해
     * 엔티티를 fetch 한 뒤 deleteAll 로 호출하는 방식이 안전하므로,
     * 호출부에서 findAllByMember_Id → deleteAllInBatch 가 아닌 deleteAll 을 사용한다.
     */
    List<SolveEntity> findAllByMember_Id(Long memberId);

    /**
     * 어드민 멤버 목록의 인라인 통계용 — 여러 멤버의 풀이 (memberId, totalCount, correctCount, createdAt) 를 batch로 가져옴.
     * 결과 row: [Long memberId, Integer totalCount, Integer correctCount, LocalDateTime createdAt]
     * JVM에서 group by memberId 로 묶어 누적 풀이/정답 수, 고유 풀이 일수, streak 일을 계산한다.
     */
    @Query("SELECT s.member.id, s.totalCount, s.correctCount, s.createdAt FROM SolveEntity s WHERE s.member.id IN :memberIds")
    List<Object[]> findStatsByMemberIds(@Param("memberIds") List<Long> memberIds);

    /**
     * 랜딩 페이지 TOP N 랭킹 — 누적 정답 수 기준.
     * 1문제 이상 풀고 정답이 1개 이상인 사용자만 (HAVING SUM > 0).
     * 동점은 가입 순(member.id ASC).
     *
     * 결과 row: [String nickname, Long totalCorrect]
     */
    @Query("""
            SELECT m.nickname, SUM(s.correctCount)
            FROM SolveEntity s
            JOIN s.member m
            GROUP BY m.id, m.nickname
            HAVING SUM(s.correctCount) > 0
            ORDER BY SUM(s.correctCount) DESC, m.id ASC
            """)
    List<Object[]> findTopRanking(Pageable pageable);

    /**
     * 사용자가 푼 모의고사별 최고 점수 (정답 수 / 총 문항 수) 일괄 조회.
     * 모의고사 목록 화면의 "풀이 완료" 마킹용. N+1 방지를 위해 1쿼리로.
     *
     * 결과 row: [Long mockExamId, Integer bestCorrect, Integer bestTotal]
     * - bestCorrect = MAX(correctCount) — 같은 모의고사의 totalCount는 고정이라 MAX(total)은 그냥 동일 값
     */
    @Query("""
            SELECT s.mockExam.id, MAX(s.correctCount), MAX(s.totalCount)
            FROM SolveEntity s
            WHERE s.member.id = :memberId AND s.mockExam IS NOT NULL
            GROUP BY s.mockExam.id
            """)
    List<Object[]> findBestScoresByMember(@Param("memberId") Long memberId);

    /**
     * 과목별(parent 기준) 풀이 사용자 수 + 풀이 수 + 총 문제 수 집계.
     * 결과 row: [Long subjectId, String subjectName, Long uniqueUsers, Long solveCount, Long totalQuestions]
     */
    @Query(value = """
            SELECT
              COALESCE(p.id, s2.id) AS subject_id,
              COALESCE(p.name, s2.name) AS subject_name,
              COUNT(DISTINCT sv.member_id) AS unique_users,
              COUNT(sv.id) AS solve_count,
              SUM(sv.total_count) AS total_questions
            FROM solve sv
            JOIN subject s2 ON s2.id = sv.subject_id
            LEFT JOIN subject p ON p.id = s2.parent_id
            WHERE sv.subject_id IS NOT NULL
            GROUP BY COALESCE(p.id, s2.id), COALESCE(p.name, s2.name)
            ORDER BY unique_users DESC
            """, nativeQuery = true)
    List<Object[]> findSubjectSolveStats();

    /**
     * 지정 시점 이후 풀이한 사용자별 총 풀이 수 집계.
     * 대시보드 차트의 "전체 평균" 비교선 계산용 — 각 멤버의 14일간 합계를 구한 뒤
     * JVM에서 평균/일수를 나눠 per-user 일평균을 산출한다.
     *
     * 결과 row: [Long memberId, Long totalCount]
     */
    @Query("SELECT s.member.id, SUM(s.totalCount) FROM SolveEntity s WHERE s.createdAt >= :since GROUP BY s.member.id")
    List<Object[]> findMemberTotalsSince(@Param("since") LocalDateTime since);
}
