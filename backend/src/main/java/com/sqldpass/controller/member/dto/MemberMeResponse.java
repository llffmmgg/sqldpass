package com.sqldpass.controller.member.dto;

import java.time.LocalDateTime;

public record MemberMeResponse(
        Long id,
        String nickname,
        String provider,
        LocalDateTime createdAt
) {
}
