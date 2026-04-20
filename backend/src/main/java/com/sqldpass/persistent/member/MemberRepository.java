package com.sqldpass.persistent.member;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByProviderAndProviderId(String provider, String providerId);

    Optional<MemberEntity> findByNickname(String nickname);

    /** 어드민 목록에서 더미(provider='SEED') 사용자를 제외 */
    Page<MemberEntity> findByProviderNot(String provider, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 일자별 신규 가입자 수 — 대시보드 추이 그래프용.
     * 결과 row: [java.sql.Date date, Long count]
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT DATE(created_at) AS d, COUNT(*) AS cnt "
                  + "FROM member WHERE created_at >= :since "
                  + "GROUP BY DATE(created_at) ORDER BY d",
            nativeQuery = true)
    java.util.List<Object[]> countByDaySince(@org.springframework.data.repository.query.Param("since") LocalDateTime since);
}
