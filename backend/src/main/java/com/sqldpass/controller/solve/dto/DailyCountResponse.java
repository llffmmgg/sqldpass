package com.sqldpass.controller.solve.dto;

import java.time.LocalDate;

/**
 * 일자별 풀이 수 — 대시보드 라인 차트용.
 * 풀이가 없는 날은 응답에 없음 (클라이언트가 0 으로 채움).
 */
public record DailyCountResponse(LocalDate date, long count) {
}
