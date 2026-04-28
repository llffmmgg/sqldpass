package com.sqldpass.controller.publicapi.dto;

import java.util.List;

/**
 * 인사이트 (Insights) 공개 API DTO 모음.
 *
 * 현재 항목:
 * - 오답률 best N (과목별)
 *
 * 통계 신뢰도를 위한 표본 필터는 InsightsService 의 상수로 관리.
 */
public final class InsightsDtos {

    private InsightsDtos() {}

    /** 오답률 best N — 응답. */
    public record HardestQuestionsResponse(
            Long subjectId,
            String subjectName,
            int totalSamples,         // 통계에 포함된 question 수
            List<HardestQuestionItem> items
    ) {}

    public record HardestQuestionItem(
            Long questionId,
            String questionPreview,   // 본문 앞 120자 미리보기
            long attempts,            // 시도 수
            long wrongCount,          // 오답 수
            double wrongRate          // 오답률 (0~100)
    ) {}
}
