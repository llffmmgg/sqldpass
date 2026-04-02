package com.sqldpass.controller.admin;

import java.time.LocalDateTime;

import com.sqldpass.persistent.member.MemberEntity;

public record AdminMemberResponse(
        Long id,
        String provider,
        String nickname,
        String email,
        LocalDateTime createdAt) {

    public static AdminMemberResponse from(MemberEntity entity) {
        return new AdminMemberResponse(
                entity.getId(),
                entity.getProvider(),
                entity.getNickname(),
                entity.getEmail(),
                entity.getCreatedAt());
    }
}
