package com.sqldpass.controller.admin.dto;

import java.util.List;

public record AdminStatsResponse(
        long totalQuestions,
        long verifiedQuestions,
        long unverifiedQuestions,
        long totalMembers,
        long totalSolves,
        long todayQuestions,
        long todayMembers,
        long todaySolves,
        List<SubjectSolveStats> subjectStats) {

    public record SubjectSolveStats(
            long subjectId,
            String subjectName,
            long uniqueUsers,
            long solveCount,
            long totalQuestions) {
    }
}
