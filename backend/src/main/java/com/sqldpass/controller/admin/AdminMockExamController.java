package com.sqldpass.controller.admin;

import java.time.LocalDateTime;
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
import com.sqldpass.controller.mockexam.dto.MockExamDetailResponse;
import com.sqldpass.controller.mockexam.dto.MockExamSummaryResponse;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
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
    private final QuestionRepository questionRepository;

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
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Integer> markAllVerified(@PathVariable Long id) {
        var exam = mockExamService.getById(id);
        List<Long> questionIds = exam.getQuestions().stream()
                .map(q -> q.getQuestionId())
                .toList();
        int updated = questionRepository.markVerifiedInBatch(questionIds, LocalDateTime.now());
        return Map.of("marked", updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "\uBAA8\uC758\uACE0\uC0AC \uC0AD\uC81C")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        mockExamService.delete(id);
    }
}
