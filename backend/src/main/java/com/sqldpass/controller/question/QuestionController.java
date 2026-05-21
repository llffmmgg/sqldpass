package com.sqldpass.controller.question;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.question.dto.QuestionDetailResponse;
import com.sqldpass.controller.question.dto.QuestionResponse;
import com.sqldpass.service.question.QuestionService;
import com.sqldpass.service.usage.DailyUsageService;

import jakarta.servlet.http.HttpServletRequest;
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
    private final DailyUsageService dailyUsageService;

    @GetMapping("/api/questions")
    @Operation(summary = "과목별 랜덤 문제 조회", description = "정답과 해설은 포함하지 않는다. 로그인 사용자의 경우 푼 문제는 풀 맨 뒤로 밀린다.")
    public List<QuestionResponse> getQuestions(
            HttpServletRequest request,
            @RequestParam Long subjectId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        Long memberId = (Long) request.getAttribute("memberId");
        // 무료 회원 일일 한도 가드 — 활성 구독자/비로그인은 service 안에서 면제 처리
        dailyUsageService.consumeQuestion(memberId, size);
        return questionService.getRandomQuestions(subjectId, memberId, size).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @GetMapping("/api/questions/{id}")
    @Operation(summary = "문제 상세 조회", description = "정답과 해설을 포함한다")
    public QuestionDetailResponse getQuestion(@PathVariable Long id) {
        return QuestionDetailResponse.from(questionService.getQuestion(id));
    }
}
