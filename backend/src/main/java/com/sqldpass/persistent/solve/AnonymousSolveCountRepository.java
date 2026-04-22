package com.sqldpass.persistent.solve;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnonymousSolveCountRepository extends JpaRepository<AnonymousSolveCountEntity, Long> {

    /**
     * 지정 날짜 카운터를 delta 만큼 원자 증가. 없으면 새로 INSERT. upsert.
     */
    @Modifying
    @Query(value = "INSERT INTO anonymous_solve_count (solve_date, count, created_at, updated_at) " +
            "VALUES (:date, :delta, NOW(6), NOW(6)) " +
            "ON DUPLICATE KEY UPDATE count = count + :delta, updated_at = NOW(6)",
            nativeQuery = true)
    void increment(@Param("date") LocalDate date, @Param("delta") long delta);

    /**
     * 누적 합 (전체 비회원 풀이).
     */
    @Query("SELECT COALESCE(SUM(a.count), 0) FROM AnonymousSolveCountEntity a")
    long sumAll();

    /**
     * 특정 날짜 카운트 조회 (없으면 0).
     */
    @Query("SELECT COALESCE(a.count, 0) FROM AnonymousSolveCountEntity a WHERE a.solveDate = :date")
    Long countByDate(@Param("date") LocalDate date);
}
