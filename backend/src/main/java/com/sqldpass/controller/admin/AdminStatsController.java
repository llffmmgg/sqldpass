package com.sqldpass.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.AdminStatsResponse;
import com.sqldpass.controller.admin.dto.AdminTrendResponse;
import com.sqldpass.service.admin.AdminStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 - 통계", description = "통계 API")
@RestController
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/api/admin/stats")
    @Operation(summary = "전체 통계 조회")
    public AdminStatsResponse getStats() {
        return adminStatsService.getStats();
    }

    @GetMapping("/api/admin/stats/trend")
    @Operation(summary = "일별 회원/풀이 추이 조회 (기본 7일, 최대 90일)")
    public AdminTrendResponse getTrend(@RequestParam(defaultValue = "7") int days) {
        return adminStatsService.getTrend(days);
    }
}
