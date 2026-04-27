package com.sqldpass.controller.mockexam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.mockexam.dto.MockExamDetailResponse;
import com.sqldpass.controller.mockexam.dto.MockExamSummaryResponse;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.service.mockexam.MockExamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "모의고사", description = "모의고사 조회 API (사용자)")
@RestController
@RequestMapping("/api/mock-exams")
@RequiredArgsConstructor
public class MockExamController {

    private final MockExamService mockExamService;
    private final SolveRepository solveRepository;

    @GetMapping
    @Operation(summary = "모의고사 목록", description = "로그인 사용자는 풀이 완료 마킹 + 최고 점수가 함께 응답된다.")
    public List<MockExamSummaryResponse> list(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");

        // 로그인 사용자: 한 번에 mockExamId → (bestCorrect, bestTotal) 맵 조회
        Map<Long, int[]> bestScoreMap = new HashMap<>();
        if (memberId != null) {
            for (Object[] row : solveRepository.findBestScoresByMember(memberId)) {
                Long mockExamId = (Long) row[0];
                Integer bestCorrect = ((Number) row[1]).intValue();
                Integer bestTotal = ((Number) row[2]).intValue();
                bestScoreMap.put(mockExamId, new int[]{bestCorrect, bestTotal});
            }
        }

        // 사용자 노출은 DRAFT 제외 (PUBLISHED + PREMIUM만 — PREMIUM은 프론트에서 잠금 표시)
        return mockExamService.getAllForUser().stream()
                .map(exam -> {
                    int[] best = bestScoreMap.get(exam.getId());
                    return best != null
                            ? MockExamSummaryResponse.from(exam, best[0], best[1])
                            : MockExamSummaryResponse.from(exam);
                })
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "모의고사 상세 (50문항 포함, 정답 미포함)")
    public MockExamDetailResponse get(@PathVariable Long id) {
        // PREMIUM이면 403 LOCKED, DRAFT면 404
        return MockExamDetailResponse.from(mockExamService.getForUser(id));
    }

    /**
     * 회원의 모의고사·기출 best score 맵.
     * 기출 카탈로그 (/past-exams/{cert}) 가 SSR + ISR 캐시라 회원별 점수를
     * SSR 응답에 못 실음. 클라이언트에서 별도 호출해 카드에 머지하는 용도.
     *
     * 응답: { "{mockExamId}": { "correct": N, "total": M }, ... }
     */
    @GetMapping("/best-scores")
    @Operation(summary = "내 모의고사·기출 best score 맵 (mockExamId → {correct, total})")
    public Map<Long, Map<String, Integer>> getMyBestScores(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        Map<Long, Map<String, Integer>> result = new HashMap<>();
        if (memberId == null) return result;
        for (Object[] row : solveRepository.findBestScoresByMember(memberId)) {
            Long mockExamId = (Long) row[0];
            int bestCorrect = ((Number) row[1]).intValue();
            int bestTotal = ((Number) row[2]).intValue();
            result.put(mockExamId, Map.of("correct", bestCorrect, "total", bestTotal));
        }
        return result;
    }
}
