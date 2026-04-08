package com.sqldpass.controller.publicapi.dto;

/**
 * 랜딩 페이지 노출용 공개 통계.
 *
 * - totalMembers: 전체 회원 수
 * - totalSolves: 누적 풀이 답변 수 (한 사람이 한 문제 푼 횟수의 총합)
 *
 * 호출 빈도: 프론트엔드 ISR로 1시간에 1번만 호출되므로 캐시 불필요.
 */
public record PublicStatsResponse(
        long totalMembers,
        long totalSolves
) {
}
