package com.sqldpass.controller.admin;

import java.util.List;

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

    @GetMapping("/api/admin/stats/revenue")
    @Operation(summary = "일별 매출/환불 추이 (기본 30일, 7~365일)",
            description = "archived 구독에 연결된 결제는 집계에서 제외. 환불액은 status=CANCELLED 합산.")
    public List<AdminRevenuePoint> getRevenue(@RequestParam(defaultValue = "30") int days) {
        return adminStatsService.revenueTrend(days);
    }

    @GetMapping("/api/admin/stats/revenue/by-plan")
    @Operation(summary = "플랜별 매출 분포 (기본 30일)",
            description = "PAID 만 집계. archived 제외. revenue DESC 정렬.")
    public List<AdminRevenueByPlan> getRevenueByPlan(@RequestParam(defaultValue = "30") int days) {
        return adminStatsService.revenueByPlan(days);
    }
}
