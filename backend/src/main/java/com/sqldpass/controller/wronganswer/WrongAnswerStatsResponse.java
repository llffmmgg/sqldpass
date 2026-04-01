package com.sqldpass.controller.wronganswer;

import com.sqldpass.persistent.solve.WrongAnswerStatsProjection;

public record WrongAnswerStatsResponse(
        Long subjectId,
        String subjectName,
        int totalSolved,
        int wrongCount,
        int wrongRate
) {

    public static WrongAnswerStatsResponse from(WrongAnswerStatsProjection projection) {
        return new WrongAnswerStatsResponse(
                projection.getSubjectId(),
                projection.getSubjectName(),
                projection.getTotalSolved(),
                projection.getWrongCount(),
                projection.getWrongRate()
        );
    }
}
