package com.sqldpass.controller.publicapi.dto;

import java.time.LocalDate;

/**
 * 비회원 풀이 일일 한도 상태.
 *
 * - used: 오늘 이 IP 가 이미 푼 문제 수
 * - limit: 일일 한도 (현재 10)
 * - remaining: 남은 풀이 수 = max(limit - used, 0)
 * - exhausted: 한도 도달 여부 (used >= limit)
 * - today: 서버 기준 오늘 날짜 (자정 리셋 기준 표시용)
 */
public record PublicSolveQuotaResponse(
        int used,
        int limit,
        int remaining,
        boolean exhausted,
        LocalDate today
) {
    public static PublicSolveQuotaResponse of(int used, int limit, LocalDate today) {
        int remaining = Math.max(limit - used, 0);
        return new PublicSolveQuotaResponse(used, limit, remaining, used >= limit, today);
    }
}
