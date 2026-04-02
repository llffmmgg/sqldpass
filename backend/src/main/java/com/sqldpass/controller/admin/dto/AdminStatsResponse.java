package com.sqldpass.controller.admin.dto;

public record AdminStatsResponse(
        long totalQuestions,
        long totalMembers,
        long totalSolves,
        long todayQuestions) {
}
