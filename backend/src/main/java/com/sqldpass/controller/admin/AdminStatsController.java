package com.sqldpass.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.AdminStatsResponse;
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
}
