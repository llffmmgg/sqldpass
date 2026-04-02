package com.sqldpass.controller.subject;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.service.subject.SubjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "과목", description = "과목 관련 API")
@RestController
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping("/api/subjects")
    @Operation(summary = "과목 목록 조회", description = "과목을 트리 구조로 조회한다")
    public List<SubjectResponse> getSubjects() {
        return subjectService.getSubjectTree().stream()
                .map(SubjectResponse::from)
                .toList();
    }
}
