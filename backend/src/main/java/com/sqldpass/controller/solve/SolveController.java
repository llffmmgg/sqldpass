package com.sqldpass.controller.solve;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.solve.dto.OverallStatsResponse;
import com.sqldpass.controller.solve.dto.SolveRequest;
import com.sqldpass.controller.solve.dto.SolveResponse;
import com.sqldpass.controller.solve.dto.SolveSummaryResponse;
import com.sqldpass.service.solve.SolveService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "풀이", description = "풀이/채점 관련 API")
@RestController
@RequiredArgsConstructor
public class SolveController {

    private final SolveService solveService;

    @PostMapping("/api/solves")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "답안 제출 및 채점")
    public SolveResponse submit(
            HttpServletRequest request,
            @Valid @RequestBody SolveRequest body) {
        Long memberId = (Long) request.getAttribute("memberId");
        SolveService.SolveWithStreak result = solveService.solve(memberId, body);
        Integer current = result.streakUpdate() != null ? result.streakUpdate().currentStreak() : null;
        Integer milestone = result.streakUpdate() != null ? result.streakUpdate().milestoneReached() : null;
        return SolveResponse.from(result.solve(), current, milestone);
    }

    @GetMapping("/api/solves")
    @Operation(summary = "내 풀이 기록 목록")
    public List<SolveSummaryResponse> getSolves(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        return solveService.getMySolves(memberId).stream()
                .map(SolveSummaryResponse::from)
                .toList();
    }

    @GetMapping("/api/solves/{id}")
    @Operation(summary = "풀이 상세 조회 (본인 것만)")
    public SolveResponse getSolve(HttpServletRequest request, @PathVariable Long id) {
        Long memberId = (Long) request.getAttribute("memberId");
        return SolveResponse.from(solveService.getSolve(id, memberId));
    }

    @GetMapping("/api/solves/stats/overall-avg")
    @Operation(summary = "전체 사용자의 14일 일평균 풀이 수 (대시보드 비교선용)")
    public OverallStatsResponse getOverallStats() {
        return solveService.getOverallStats();
    }

    @GetMapping("/api/solves/me/last-cert")
    @Operation(summary = "내 마지막 풀이 자격증 slug (Daily Question 기본 탭 계산용)")
    public java.util.Map<String, String> getLastSolvedCert(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        String slug = solveService.findLastSolvedCertSlug(memberId);
        return java.util.Collections.singletonMap("cert", slug);
    }
}
