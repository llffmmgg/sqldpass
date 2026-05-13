package com.sqldpass.controller.wronganswer;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.wronganswer.dto.WrongAnswerPreviewResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerRetryRequest;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerRetryResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerStatsResponse;
import com.sqldpass.service.wronganswer.WrongAnswerService;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "오답", description = "오답 분석 관련 API")
@RestController
@RequiredArgsConstructor
public class WrongAnswerController {

    private final WrongAnswerService wrongAnswerService;

    @GetMapping("/api/wrong-answers")
    @Operation(summary = "오답 문제 목록 (마지막 풀이가 틀린 문제)")
    public List<WrongAnswerResponse> getWrongAnswers(
            HttpServletRequest request,
            @RequestParam(required = false) Long subjectId) {
        Long memberId = (Long) request.getAttribute("memberId");
        return wrongAnswerService.getWrongAnswers(memberId, subjectId);
    }

    @GetMapping("/api/wrong-answers/stats")
    @Operation(summary = "과목별 취약 영역 통계")
    public List<WrongAnswerStatsResponse> getStats(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        return wrongAnswerService.getStats(memberId);
    }

    @GetMapping("/api/wrong-answers/preview")
    @Operation(summary = "오답노트 잠금 화면 미리보기 (권한 무관, 제목·과목만)",
            description = "Thunder/Focus/Pro/Lifetime 미가입 사용자의 잠금 뷰에서 블러 처리되어 노출. "
                    + "본인 오답 상위 limit 개 (최대 10) 의 제목·과목만 반환하고 정답/해설은 제외.")
    public List<WrongAnswerPreviewResponse> getPreview(
            HttpServletRequest request,
            @RequestParam(defaultValue = "5") int limit) {
        Long memberId = (Long) request.getAttribute("memberId");
        return wrongAnswerService.getPreview(memberId, limit);
    }

    @PostMapping("/api/wrong-answers/{questionId}/retry")
    @Operation(summary = "오답 다시 풀기 (단일 문제). 정답 시 다음 조회부터 자동으로 목록에서 제거됨.")
    public WrongAnswerRetryResponse retry(
            HttpServletRequest request,
            @PathVariable Long questionId,
            @RequestBody WrongAnswerRetryRequest body) {
        Long memberId = (Long) request.getAttribute("memberId");
        return wrongAnswerService.retry(memberId, questionId, body.selectedOption(), body.answerText());
    }
}
