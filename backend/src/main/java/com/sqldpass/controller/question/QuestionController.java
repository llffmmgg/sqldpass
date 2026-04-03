package com.sqldpass.controller.question;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.question.dto.QuestionDetailResponse;
import com.sqldpass.controller.question.dto.QuestionResponse;
import com.sqldpass.service.question.QuestionService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제", description = "문제 관련 API")
@Validated
@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/api/questions")
    @Operation(summary = "과목별 랜덤 문제 조회", description = "정답과 해설은 포함하지 않는다")
    public List<QuestionResponse> getQuestions(
            @RequestParam Long subjectId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return questionService.getRandomQuestions(subjectId, size).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @GetMapping("/api/questions/{id}")
    @Operation(summary = "문제 상세 조회", description = "정답과 해설을 포함한다")
    public QuestionDetailResponse getQuestion(@PathVariable Long id) {
        return QuestionDetailResponse.from(questionService.getQuestion(id));
    }
}
