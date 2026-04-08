package com.sqldpass.controller.admin.dto;

/**
 * LLM 일괄 검증 결과 — approved=false인 의심 문제만 노출.
 */
public record QuestionVerifyResultResponse(
        Long questionId,
        String subjectName,
        String summary,
        String reason
) {
}
