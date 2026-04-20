package com.sqldpass.controller.admin.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminTrendResponse(int days, List<DailyPoint> points) {

    public record DailyPoint(LocalDate date, long newMembers, long newSolves) {
    }
}
