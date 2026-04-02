package com.sqldpass.controller.wronganswer;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.wronganswer.dto.WrongAnswerResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerStatsResponse;
import com.sqldpass.service.wronganswer.WrongAnswerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "오답", description = "오답 분석 관련 API")
@RestController
@RequiredArgsConstructor
public class WrongAnswerController {

    private final WrongAnswerService wrongAnswerService;

    @GetMapping("/api/wrong-answers")
    @Operation(summary = "오답 문제 목록")
    public List<WrongAnswerResponse> getWrongAnswers(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestParam(required = false) Long subjectId) {
        return wrongAnswerService.getWrongAnswers(memberId, subjectId);
    }

    @GetMapping("/api/wrong-answers/stats")
    @Operation(summary = "과목별 취약 영역 통계")
    public List<WrongAnswerStatsResponse> getStats(@RequestHeader("X-Member-Id") Long memberId) {
        return wrongAnswerService.getStats(memberId);
    }
}
