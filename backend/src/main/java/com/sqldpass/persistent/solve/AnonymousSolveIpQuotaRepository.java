package com.sqldpass.persistent.solve;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnonymousSolveIpQuotaRepository
        extends JpaRepository<AnonymousSolveIpQuotaEntity, AnonymousSolveIpQuotaEntity.IpQuotaId> {

    /**
     * (ip, today) 의 사용량을 delta 만큼 원자 증가. 없으면 새 row INSERT.
     * 하루가 지나면 (ip, today) 키가 달라져 새 row 가 생기고, 어제 row 는 보존되어 자연스럽게 자정 리셋된다.
     */
    @Modifying
    @Query(value = "INSERT INTO anonymous_solve_ip_quota (ip, solve_date, used_count, created_at, updated_at) " +
            "VALUES (:ip, :date, :delta, NOW(6), NOW(6)) " +
            "ON DUPLICATE KEY UPDATE used_count = used_count + :delta, updated_at = NOW(6)",
            nativeQuery = true)
    void increment(@Param("ip") String ip, @Param("date") LocalDate date, @Param("delta") int delta);

    /**
     * (ip, today) 의 현재 사용량 조회. row 가 없으면 0.
     */
    @Query("SELECT COALESCE(q.usedCount, 0) FROM AnonymousSolveIpQuotaEntity q WHERE q.id.ip = :ip AND q.id.solveDate = :date")
    Integer usedCount(@Param("ip") String ip, @Param("date") LocalDate date);
}
