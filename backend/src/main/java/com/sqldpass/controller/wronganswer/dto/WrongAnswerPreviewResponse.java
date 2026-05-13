package com.sqldpass.controller.wronganswer.dto;

import com.sqldpass.persistent.solve.WrongAnswerProjection;

/**
 * 오답노트 잠금 화면 미리보기용 — 권한 없는 사용자에게 노출되는 최소 정보.
 * 정답/해설/키워드 는 보내지 않는다(블러 처리해도 inspect 로 노출 우려 방지).
 */
public record WrongAnswerPreviewResponse(
        Long questionId,
        String questionContent,
        String subjectName
) {
    public static WrongAnswerPreviewResponse from(WrongAnswerProjection p) {
        return new WrongAnswerPreviewResponse(
                p.getQuestionId(),
                p.getQuestionContent(),
                p.getSubjectName()
        );
    }
}
