package com.sqldpass.persistent.usage;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyUsageRepository extends JpaRepository<DailyUsageEntity, DailyUsageId> {

    /**
     * 단순 조회. 표시·검사용. UPSERT 직후 같은 트랜잭션에서 SELECT 로도 재사용된다.
     */
    @Query("SELECT d FROM DailyUsageEntity d WHERE d.id.memberId = :memberId AND d.id.usageDate = :usageDate")
    Optional<DailyUsageEntity> findByMemberIdAndUsageDate(@Param("memberId") Long memberId,
                                                         @Param("usageDate") LocalDate usageDate);

    /**
     * (memberId, usageDate) 의 카운터를 atomic 으로 증가. 없으면 새 row INSERT.
     * MySQL row-level lock 하에 ON DUPLICATE KEY UPDATE 가 atomic 이라 동시 요청 race 방어.
     *
     * PostgreSQL 의 ON CONFLICT ... RETURNING 은 MySQL 에 없으므로, 직후 같은 트랜잭션에서
     * findByMemberIdAndUsageDate 로 새 값 SELECT.
     */
    @Modifying
    @Query(value = """
            INSERT INTO daily_usage (member_id, usage_date, question_count, mock_session_count, created_at, updated_at)
            VALUES (:memberId, :usageDate, :questionDelta, :mockDelta, NOW(6), NOW(6))
            ON DUPLICATE KEY UPDATE
                question_count = question_count + VALUES(question_count),
                mock_session_count = mock_session_count + VALUES(mock_session_count),
                updated_at = NOW(6)
            """, nativeQuery = true)
    int upsertAndAdd(@Param("memberId") Long memberId,
                     @Param("usageDate") LocalDate usageDate,
                     @Param("questionDelta") int questionDelta,
                     @Param("mockDelta") int mockDelta);
}
