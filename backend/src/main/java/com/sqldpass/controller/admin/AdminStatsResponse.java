package com.sqldpass.controller.admin;

public record AdminStatsResponse(
        long totalQuestions,
        long totalMembers,
        long totalSolves,
        long todayQuestions) {
}
