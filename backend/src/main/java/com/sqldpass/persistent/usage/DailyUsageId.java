package com.sqldpass.persistent.usage;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * daily_usage 복합 PK. (member_id, usage_date).
 * 동일 회원의 일자별 사용량 row 를 식별한다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyUsageId implements Serializable {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    public DailyUsageId(Long memberId, LocalDate usageDate) {
        this.memberId = memberId;
        this.usageDate = usageDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyUsageId other)) return false;
        return Objects.equals(memberId, other.memberId)
                && Objects.equals(usageDate, other.usageDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, usageDate);
    }
}
