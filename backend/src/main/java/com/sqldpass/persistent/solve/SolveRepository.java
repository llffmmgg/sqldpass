package com.sqldpass.persistent.solve;

import java.util.List;

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
}
