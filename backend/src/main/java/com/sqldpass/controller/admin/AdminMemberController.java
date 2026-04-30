package com.sqldpass.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.AdminMemberDashboardResponse;
import com.sqldpass.controller.admin.dto.AdminMemberResponse;
import com.sqldpass.controller.admin.dto.AdminSolveDetailResponse;
import com.sqldpass.service.admin.AdminMemberService;
import com.sqldpass.service.solve.SolveService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "관리자 - 회원", description = "회원 관리 API")
@Validated
@RestController
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;
    private final SolveService solveService;

    @GetMapping("/api/admin/members")
    @Operation(summary = "회원 목록 조회 (sort: default|totalSolved|totalCorrect|activeDays|streakDays, order: desc|asc, q: 닉네임 LIKE)")
    public Page<AdminMemberResponse> getMembers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String q) {
        return adminMemberService.getMembers(page, size, sort, order, q);
    }

    @GetMapping("/api/admin/members/{memberId}/dashboard")
    @Operation(summary = "특정 회원 학습 대시보드 (어드민용)")
    public AdminMemberDashboardResponse getMemberDashboard(@PathVariable Long memberId) {
        return adminMemberService.getDashboard(memberId);
    }

    @GetMapping("/api/admin/solves/{solveId}")
    @Operation(summary = "풀이 상세 조회 (어드민용 — 문제 내용 + 사용자 답)")
    public AdminSolveDetailResponse getSolveDetail(@PathVariable Long solveId) {
        return AdminSolveDetailResponse.from(solveService.getSolveEntityForAdmin(solveId));
    }
}
