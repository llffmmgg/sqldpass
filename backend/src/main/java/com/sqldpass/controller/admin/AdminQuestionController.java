package com.sqldpass.controller.admin;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.sqldpass.controller.admin.dto.AdminQuestionResponse;
import com.sqldpass.controller.admin.dto.AdminQuestionUpdateRequest;
import com.sqldpass.controller.admin.dto.QuestionVerifyHistoryResponse;
import com.sqldpass.controller.admin.dto.QuestionVerifyRunResponse;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.service.admin.AdminQuestionService;
import com.sqldpass.service.admin.QuestionExportService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 - 문제", description = "문제 관리 API")
@Validated
@RestController
@RequiredArgsConstructor
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;
    private final QuestionExportService questionExportService;

    @GetMapping("/api/admin/questions")
    @Operation(summary = "문제 목록 조회")
    public Page<AdminQuestionResponse> getQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminQuestionService.getQuestions(subjectId, page, size);
    }

    @GetMapping("/api/admin/questions/{id}")
    @Operation(summary = "문제 상세 조회")
    public AdminQuestionResponse getQuestion(@PathVariable Long id) {
        return adminQuestionService.getQuestion(id);
    }

    @PutMapping("/api/admin/questions/{id}")
    @Operation(summary = "문제 수정")
    public AdminQuestionResponse updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody AdminQuestionUpdateRequest request) {
        return adminQuestionService.updateQuestion(id, request);
    }

    @DeleteMapping("/api/admin/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "문제 삭제")
    public void deleteQuestion(@PathVariable Long id) {
        adminQuestionService.deleteQuestion(id);
    }

    // ----------------------------------------------------------
    // LLM 검증용 Markdown export
    // ----------------------------------------------------------

    @GetMapping("/api/admin/questions/export")
    @Operation(summary = "문제 Markdown export (LLM 검증용)")
    public ResponseEntity<byte[]> exportQuestions(
            @RequestParam("examType") String examType,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        QuestionExportService.ExportResult result = questionExportService.export(examType, force);
        String filename = "sqldpass-" + examType.toLowerCase() + "-" + LocalDate.now() + ".md";
        byte[] body = result.markdown().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.set("X-Export-Count", String.valueOf(result.count()));

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    @PostMapping("/api/admin/questions/verify")
    @Operation(summary = "LLM 일괄 검증 — 의심 문제 ID 리스트 반환")
    public QuestionVerifyRunResponse verifyAll(
            @RequestParam(required = false) ExamType examType,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(2000) int limit,
            @RequestParam(defaultValue = "false") boolean force) {
        return adminQuestionService.verifyAll(examType, subjectId, limit, force);
    }

    @GetMapping("/api/admin/questions/verify/history")
    @Operation(summary = "LLM 직검증 최근 실행 이력")
    public List<QuestionVerifyHistoryResponse> getVerifyHistory(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        return adminQuestionService.getVerifyHistory(limit);
    }

    @PostMapping("/api/admin/questions/export/reset")
    @Operation(summary = "Export 마크 리셋")
    public Map<String, Integer> resetExportMark(@RequestParam("examType") String examType) {
        int reset = questionExportService.resetMark(examType);
        return Map.of("reset", reset);
    }
}
