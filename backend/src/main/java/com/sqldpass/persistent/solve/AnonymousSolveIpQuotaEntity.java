package com.sqldpass.persistent.solve;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비회원 풀이 일일 한도용 IP 단위 quota 카운터.
 * (ip, solve_date) 복합 PK 라 새 날짜에 자동으로 새 행이 생기고, 어제 row 는 그대로 남아 자정 리셋 효과를 만든다.
 *
 * 기존 {@link AnonymousSolveCountEntity}(V41 일별 전체 합계)는 admin 통계 입력으로 보존하고,
 * quota 추적은 본 엔티티가 전담한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "anonymous_solve_ip_quota", indexes = {
        @Index(name = "idx_ip_quota_date", columnList = "solve_date")
})
public class AnonymousSolveIpQuotaEntity {

    @EmbeddedId
    private IpQuotaId id;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IpQuotaId implements Serializable {

        @Column(name = "ip", nullable = false, length = 64)
        private String ip;

        @Column(name = "solve_date", nullable = false)
        private LocalDate solveDate;

        public IpQuotaId(String ip, LocalDate solveDate) {
            this.ip = ip;
            this.solveDate = solveDate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IpQuotaId other)) return false;
            return Objects.equals(ip, other.ip) && Objects.equals(solveDate, other.solveDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ip, solveDate);
        }
    }
}
