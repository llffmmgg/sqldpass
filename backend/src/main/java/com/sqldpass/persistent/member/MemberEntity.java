package com.sqldpass.persistent.member;

import java.time.LocalDate;

import com.sqldpass.persistent.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member", uniqueConstraints = {
    @UniqueConstraint(name = "uk_member_provider", columnNames = {"provider", "provider_id"}),
    @UniqueConstraint(name = "uk_member_nickname", columnNames = {"nickname"})
})
public class MemberEntity extends BaseTimeEntity {

    /** 연속 학습 마일스톤. 해당 일수 도달 시 축하 알림. */
    private static final int[] STREAK_MILESTONES = { 7, 30, 100, 365 };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(nullable = false, length = 50)
    private String nickname;

    /**
     * Google OAuth email_verified=true 인 경우에만 저장.
     * V10 에서 한 번 DROP 됐다가 V82 (2026-05-11) 에 KG이니시스 customer.email 용으로 재추가.
     * UNIQUE 제약 없음 — provider/provider_id 가 유일 식별자, email 은 결제 식별 보조 용도.
     */
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak = 0;

    @Column(name = "last_solve_date")
    private LocalDate lastSolveDate;

    public MemberEntity(String provider, String providerId, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
    }

    public MemberEntity(String provider, String providerId, String nickname, String email) {
        this(provider, providerId, nickname);
        this.email = email;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * email_verified=true 통과한 값만 호출 전제. null/blank/동일 시 무동작 →
     * JPA dirty checking 에 의한 불필요 UPDATE 쿼리 생략.
     */
    public void updateEmail(String email) {
        if (email == null || email.isBlank()) return;
        if (email.equals(this.email)) return;
        this.email = email;
    }

    /**
     * 오늘 풀이 기록으로 streak 상태 갱신.
     * - 오늘 이미 풀었으면 변화 없음
     * - 어제 풀었으면 current +1
     * - 2일 이상 공백이면 current = 1 (리셋)
     * - longest = max(longest, current)
     * 이번 호출로 마일스톤(7/30/100/365) 도달 시 해당 값을 결과에 포함.
     */
    public StreakUpdateResult applyTodaySolve(LocalDate today) {
        if (lastSolveDate != null && lastSolveDate.equals(today)) {
            return new StreakUpdateResult(currentStreak, false, null);
        }

        int before = currentStreak;
        if (lastSolveDate != null && lastSolveDate.plusDays(1).equals(today)) {
            currentStreak = currentStreak + 1;
        } else {
            currentStreak = 1;
        }
        lastSolveDate = today;

        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }

        Integer milestone = null;
        for (int m : STREAK_MILESTONES) {
            if (currentStreak == m && before < m) {
                milestone = m;
                break;
            }
        }
        return new StreakUpdateResult(currentStreak, true, milestone);
    }

    public boolean hasSolvedToday(LocalDate today) {
        return lastSolveDate != null && lastSolveDate.equals(today);
    }

    public record StreakUpdateResult(int currentStreak, boolean changed, Integer milestoneReached) {
    }
}
