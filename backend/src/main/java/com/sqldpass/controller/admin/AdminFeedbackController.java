package com.sqldpass.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.admin.dto.UpdateFeedbackStatusRequest;
import com.sqldpass.controller.feedback.dto.FeedbackResponse;
import com.sqldpass.domain.feedback.Feedback;
import com.sqldpass.persistent.feedback.FeedbackStatus;
import com.sqldpass.service.feedback.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 - 피드백", description = "사용자 피드백 조회 및 상태 관리")
@Validated
@RestController
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/api/admin/feedback")
    @Operation(summary = "피드백 목록 조회")
    public Page<FeedbackResponse> list(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<Feedback> result = feedbackService.getAll(status, page, size);
        return result.map(f -> FeedbackResponse.from(f, feedbackService.resolveNickname(f.getMemberId())));
    }

    @PatchMapping("/api/admin/feedback/{id}/status")
    @Operation(summary = "피드백 상태 변경")
    public FeedbackResponse updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateFeedbackStatusRequest body) {
        Feedback updated = feedbackService.updateStatus(id, body.status());
        return FeedbackResponse.from(updated, feedbackService.resolveNickname(updated.getMemberId()));
    }
}
