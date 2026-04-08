package com.sqldpass.persistent.solve;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveRepository extends JpaRepository<SolveEntity, Long> {

    List<SolveEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 어드민 멤버 목록의 인라인 통계용 — 여러 멤버의 풀이 (memberId, totalCount, createdAt) 만 batch로 가져옴.
     * 결과 row: [Long memberId, Integer totalCount, LocalDateTime createdAt]
     * JVM에서 group by memberId 로 묶어 누적 풀이 수와 streak 일을 계산한다.
     */
    @Query("SELECT s.member.id, s.totalCount, s.createdAt FROM SolveEntity s WHERE s.member.id IN :memberIds")
    List<Object[]> findStatsByMemberIds(@Param("memberIds") List<Long> memberIds);

    /**
     * 랜딩 페이지 TOP N 랭킹 — 누적 정답 수 기준.
     * 1문제 이상 푼 사용자만 (INNER JOIN solve), 동점은 가입 순(member.id ASC).
     *
     * 결과 row: [String nickname, Long totalCorrect]
     */
    @Query("""
            SELECT m.nickname, SUM(s.correctCount)
            FROM SolveEntity s
            JOIN s.member m
            GROUP BY m.id, m.nickname
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
}
