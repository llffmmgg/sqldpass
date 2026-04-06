package com.sqldpass.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.mockexam.dto.MockExamSummaryResponse;
import com.sqldpass.service.mockexam.MockExamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 — 모의고사", description = "모의고사 생성/관리 API")
@RestController
@RequestMapping("/api/admin/mock-exams")
@RequiredArgsConstructor
public class AdminMockExamController {

    private final MockExamService mockExamService;

    @GetMapping
    @Operation(summary = "모의고사 목록 (관리자)")
    public List<MockExamSummaryResponse> list() {
        return mockExamService.getAll().stream()
                .map(MockExamSummaryResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "신규 모의고사 생성")
    @ResponseStatus(HttpStatus.CREATED)
    public MockExamSummaryResponse create() {
        return MockExamSummaryResponse.from(mockExamService.create());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "모의고사 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        mockExamService.delete(id);
    }
}
