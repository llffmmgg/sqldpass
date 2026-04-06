package com.sqldpass.service.notification;

import java.util.List;

/**
 * Discord 웹훅 임베드 메시지 구조 (https://discord.com/developers/docs/resources/channel#embed-object)
 *
 * 색상 가이드 (10진수):
 * - 초록 (성공): 5763719 (0x57F287)
 * - 노랑 (정보): 16705372 (0xFEE75C)
 * - 빨강 (에러): 15548997 (0xED4245)
 * - 파랑 (가입): 5814783 (0x58B9FF)
 */
public record DiscordEmbed(
        String title,
        String description,
        int color,
        List<Field> fields,
        String timestamp
) {
    public record Field(String name, String value, boolean inline) {}
}
