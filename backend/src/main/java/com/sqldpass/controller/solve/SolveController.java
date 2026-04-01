package com.sqldpass.controller.solve;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.service.solve.SolveService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "풀이", description = "풀이/채점 관련 API")
@RestController
@RequestMapping("/api/solves")
public class SolveController {

    private final SolveService solveService;

    public SolveController(SolveService solveService) {
        this.solveService = solveService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "답안 제출 및 채점")
    public SolveResponse submit(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody SolveRequest request) {
        return SolveResponse.from(solveService.solve(memberId, request));
    }

    @GetMapping
    @Operation(summary = "내 풀이 기록 목록")
    public List<SolveSummaryResponse> getSolves(@RequestHeader("X-Member-Id") Long memberId) {
        return solveService.getMySolves(memberId).stream()
                .map(SolveSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "풀이 상세 조회")
    public SolveResponse getSolve(@PathVariable Long id) {
        return SolveResponse.from(solveService.getSolve(id));
    }
}
