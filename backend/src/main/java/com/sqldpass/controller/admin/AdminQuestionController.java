package com.sqldpass.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.AdminQuestionResponse;
import com.sqldpass.controller.admin.dto.AdminQuestionUpdateRequest;
import com.sqldpass.service.admin.AdminQuestionService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 - 문제", description = "문제 관리 API")
@RestController
@RequiredArgsConstructor
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    @GetMapping("/api/admin/questions")
    @Operation(summary = "문제 목록 조회")
    public Page<AdminQuestionResponse> getQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
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
}
