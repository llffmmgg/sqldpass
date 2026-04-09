package com.sqldpass.controller.admin.dto;

public record AdminStatsResponse(
        long totalQuestions,
        long verifiedQuestions,
        long unverifiedQuestions,
        long totalMembers,
        long totalSolves,
        long todayQuestions) {
}
