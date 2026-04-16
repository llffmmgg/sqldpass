package com.sqldpass.controller.admin.dto;

import java.time.LocalDateTime;

import com.sqldpass.persistent.member.MemberEntity;

public record AdminMemberResponse(
        Long id,
        String provider,
        String nickname,
        LocalDateTime createdAt,
        /** 해당 회원의 누적 풀이 수 (모든 SolveEntity.totalCount 합) */
        int totalSolved,
        /** 해당 회원의 누적 정답 수 (모든 SolveEntity.correctCount 합) */
        int totalCorrect,
        /** 풀이가 하루라도 있었던 고유 날짜 수 */
        int activeDays,
        /** 오늘(혹은 어제)부터 연속으로 풀이가 있는 일 수 */
        int streakDays) {

    public static AdminMemberResponse from(
            MemberEntity entity,
            int totalSolved,
            int totalCorrect,
            int activeDays,
            int streakDays) {
        return new AdminMemberResponse(
                entity.getId(),
                entity.getProvider(),
                entity.getNickname(),
                entity.getCreatedAt(),
                totalSolved,
                totalCorrect,
                activeDays,
                streakDays);
    }
}
