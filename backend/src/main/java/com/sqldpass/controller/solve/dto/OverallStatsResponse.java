package com.sqldpass.controller.solve.dto;

/**
 * 전체 사용자의 최근 14일 일평균 풀이 수.
 * 대시보드 차트에서 "내 평균 vs 전체 평균" 비교용.
 */
public record OverallStatsResponse(double avgDailyCount) {
}
