package com.sqldpass.controller.admin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.CreateMockExamRequest;
import com.sqldpass.controller.admin.dto.ManualMockExamRequest;
import com.sqldpass.controller.mockexam.dto.MockExamDetailResponse;
import com.sqldpass.controller.mockexam.dto.MockExamSummaryResponse;
import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.mockexam.MiniMockExamCreator;
import com.sqldpass.service.mockexam.MockExamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "\uAD00\uB9AC\uC790 - \uBAA8\uC758\uACE0\uC0AC", description = "\uBAA8\uC758\uACE0\uC0AC \uC0DD\uC131/\uAD00\uB9AC API")
@RestController
@RequestMapping("/api/admin/mock-exams")
@RequiredArgsConstructor
public class AdminMockExamController {

    private final MockExamService mockExamService;

    @GetMapping
    @Operation(summary = "\uBAA8\uC758\uACE0\uC0AC \uBAA9\uB85D (\uAD00\uB9AC\uC790)")
    public List<MockExamSummaryResponse> list() {
        return mockExamService.getAll().stream()
                .map(MockExamSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "모의고사 상세 (관리자)")
    public MockExamDetailResponse get(@PathVariable Long id) {
        return MockExamDetailResponse.from(mockExamService.getById(id));
    }

    @PostMapping
    @Operation(
            summary = "\uC2E0\uADDC \uBAA8\uC758\uACE0\uC0AC \uC0DD\uC131",
            description = "body.examType \uC0DD\uB7B5 \uC2DC SQLD. difficulty(EASY/NORMAL/HARD/VERY_HARD)\uB294 SQLD, \uC815\uCC98\uAE30 \uC2E4\uAE30, \uCEF4\uD65C 1\uAE09 \uBAA8\uB450\uC5D0 \uC801\uC6A9\uB429\uB2C8\uB2E4."
    )
    @ResponseStatus(HttpStatus.CREATED)
    public MockExamSummaryResponse create(@RequestBody(required = false) CreateMockExamRequest body) {
        ExamType type = (body != null && body.examType() != null) ? body.examType() : ExamType.SQLD;
        MockExamDifficulty difficulty = (body != null) ? body.difficulty() : null;
        EngineerExamTemplate engineerTemplate = (body != null) ? body.engineerTemplate() : null;
        return MockExamSummaryResponse.from(mockExamService.create(type, difficulty, engineerTemplate));
    }

    @PostMapping("/manual")
    @Operation(
            summary = "수동 모의고사 등록 (JSON 한 통으로 메타 + 문제 N개 적재)",
            description = "AI 생성 없이 어드민이 직접 JSON 으로 모의고사를 적재한다. PAST_EXAM 승격 / 전문가 검수 플래그도 같은 트랜잭션에서 처리."
    )
    @ResponseStatus(HttpStatus.CREATED)
    public MockExamSummaryResponse createManual(@RequestBody ManualMockExamRequest body) {
        return MockExamSummaryResponse.from(mockExamService.createManual(body));
    }

    @PostMapping("/mini")
    @Operation(
            summary = "미니 모의고사 일괄 생성",
            description = "현재 풀(includedInMiniAt IS NULL 인 검수 완료 문제)에서 기출:AI:프리미엄 = 1:1:1, 과목 분포 보존하며 가능한 회차를 모두 생성한다. visibility=PREMIUM + kind=MINI 로 발급."
    )
    @ResponseStatus(HttpStatus.CREATED)
    public MiniMockExamGenerationResponse createMini(@RequestBody CreateMiniMockExamRequest body) {
        if (body == null || body.examType() == null) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "examType 은 필수입니다.");
        }
        return MiniMockExamGenerationResponse.from(mockExamService.createMiniBatch(body.examType()));
    }

    public record CreateMiniMockExamRequest(ExamType examType) {}

    public record MiniMockExamGenerationResponse(
            ExamType examType,
            int createdCount,
            List<Long> createdMockExamIds,
            /** PAST_EXAM / AI_PUBLISHED / AI_PREMIUM 별 잔여 풀 수 (해당 examType 전체 과목 합) */
            Map<String, Long> remainingPoolBySource
    ) {
        public static MiniMockExamGenerationResponse from(MiniMockExamCreator.GenerationResult r) {
            LinkedHashMap<String, Long> remaining = new LinkedHashMap<>();
            r.remainingBySource().forEach((k, v) -> remaining.put(k.name(), v));
            return new MiniMockExamGenerationResponse(
                    r.examType(), r.createdCount(),
                    r.createdMockExamIds(), remaining);
        }
    }

    @PatchMapping("/{id}/visibility")
    @Operation(summary = "모의고사 공개 상태 변경 (DRAFT/PUBLISHED/PREMIUM)")
    public MockExamSummaryResponse changeVisibility(
            @PathVariable Long id,
            @RequestBody ChangeVisibilityRequest body) {
        return MockExamSummaryResponse.from(
                mockExamService.changeVisibility(id, body.visibility()));
    }

    public record ChangeVisibilityRequest(MockExamVisibility visibility) {}

    @PostMapping("/{id}/mark-verified")
    @Operation(summary = "모의고사 전체 문제 수동 검수 완료 처리")
    public Map<String, Integer> markAllVerified(@PathVariable Long id) {
        int updated = mockExamService.markAllQuestionsVerified(id);
        return Map.of("marked", updated);
    }

    @PostMapping("/{id}/toggle-expert-verified")
    @Operation(summary = "전문가 검증 완료 토글")
    public Map<String, Boolean> toggleExpertVerified(@PathVariable Long id) {
        boolean result = mockExamService.toggleExpertVerified(id);
        return Map.of("expertVerified", result);
    }

    @PatchMapping("/{id}/past-exam-meta")
    @Operation(summary = "기출 복원 메타 설정 (kind, 연도, 회차, 시험일)")
    public MockExamSummaryResponse setPastExamMeta(
            @PathVariable Long id,
            @RequestBody PastExamMetaRequest body) {
        return MockExamSummaryResponse.from(
                mockExamService.setPastExamMeta(id, body.promote(), body.examYear(), body.examRound(), body.examDate()));
    }

    public record PastExamMetaRequest(
            boolean promote,
            Integer examYear,
            Integer examRound,
            java.time.LocalDate examDate) {
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "\uBAA8\uC758\uACE0\uC0AC \uC0AD\uC81C")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        mockExamService.delete(id);
    }
}
