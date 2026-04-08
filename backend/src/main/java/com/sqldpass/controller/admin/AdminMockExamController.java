package com.sqldpass.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.CreateMockExamRequest;
import com.sqldpass.controller.mockexam.dto.MockExamSummaryResponse;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
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
    @Operation(summary = "신규 모의고사 생성", description = "body.examType 생략 시 SQLD (4지선다 50문항). ENGINEER_PRACTICAL 지정 시 정보처리기사 실기 (단답/서술 20문항). difficulty(EASY/NORMAL/HARD)는 정처기에만 적용.")
    @ResponseStatus(HttpStatus.CREATED)
    public MockExamSummaryResponse create(@RequestBody(required = false) CreateMockExamRequest body) {
        ExamType type = (body != null && body.examType() != null) ? body.examType() : ExamType.SQLD;
        MockExamDifficulty difficulty = (body != null) ? body.difficulty() : null;
        return MockExamSummaryResponse.from(mockExamService.create(type, difficulty));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "모의고사 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        mockExamService.delete(id);
    }
}
