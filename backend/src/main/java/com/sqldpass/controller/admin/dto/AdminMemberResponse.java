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
        /** 오늘(혹은 어제)부터 연속으로 풀이가 있는 일 수 */
        int streakDays) {

    public static AdminMemberResponse from(MemberEntity entity, int totalSolved, int streakDays) {
        return new AdminMemberResponse(
                entity.getId(),
                entity.getProvider(),
                entity.getNickname(),
                entity.getCreatedAt(),
                totalSolved,
                streakDays);
    }
}
