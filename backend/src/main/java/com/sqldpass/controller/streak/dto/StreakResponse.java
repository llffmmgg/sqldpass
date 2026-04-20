package com.sqldpass.controller.streak.dto;

import java.time.LocalDate;

public record StreakResponse(
        int currentStreak,
        int longestStreak,
        LocalDate lastSolveDate,
        boolean solvedToday) {
}
