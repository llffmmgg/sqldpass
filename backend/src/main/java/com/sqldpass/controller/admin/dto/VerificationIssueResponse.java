package com.sqldpass.controller.admin.dto;

import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.VerificationCategory;

/**
 * 어드민 검증 카테고리별 문제 목록 한 줄 — 카드 표시 + 수정 페이지로 링크.
 */
public record VerificationIssueResponse(
        Long id,
        String subjectName,
        String summary,
        String contentPreview,
        VerificationCategory category
) {
    public static VerificationIssueResponse from(QuestionEntity q) {
        String content = q.getContent();
        String preview = content == null ? null
                : (content.length() > 120 ? content.substring(0, 120) + "…" : content);
        String subjectName = null;
        if (q.getSubject() != null) {
            subjectName = q.getSubject().getParent() != null
                    ? q.getSubject().getParent().getName()
                    : q.getSubject().getName();
        }
        return new VerificationIssueResponse(
                q.getId(),
                subjectName,
                q.getSummary(),
                preview,
                q.getVerificationCategory());
    }
}
