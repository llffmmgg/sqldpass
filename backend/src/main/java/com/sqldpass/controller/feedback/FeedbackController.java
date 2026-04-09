package com.sqldpass.controller.feedback;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.feedback.dto.CreateFeedbackRequest;
import com.sqldpass.controller.feedback.dto.FeedbackResponse;
import com.sqldpass.domain.feedback.Feedback;
import com.sqldpass.service.feedback.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "피드백", description = "사용자 피드백/오류 신고")
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/api/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "피드백 제출 (로그인 필수)")
    public FeedbackResponse create(HttpServletRequest request,
                                   @Valid @RequestBody CreateFeedbackRequest body) {
        Long memberId = (Long) request.getAttribute("memberId");
        Feedback created = feedbackService.create(memberId, body);
        return FeedbackResponse.from(created, feedbackService.resolveNickname(memberId));
    }

    @GetMapping("/api/feedback/me")
    @Operation(summary = "내가 작성한 피드백 목록 (어드민 답변 포함)")
    public List<FeedbackResponse> getMine(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        String nickname = feedbackService.resolveNickname(memberId);
        return feedbackService.getMine(memberId).stream()
                .map(f -> FeedbackResponse.from(f, nickname))
                .toList();
    }
}
