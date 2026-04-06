package com.sqldpass.controller.mockexam.dto;

import java.time.LocalDateTime;

import com.sqldpass.domain.mockexam.MockExam;

public record MockExamSummaryResponse(
        Long id,
        String name,
        int sequence,
        int totalQuestions,
        LocalDateTime createdAt
) {
    public static MockExamSummaryResponse from(MockExam mockExam) {
        return new MockExamSummaryResponse(
                mockExam.getId(),
                mockExam.getName(),
                mockExam.getSequence(),
                mockExam.getTotalQuestions(),
                mockExam.getCreatedAt());
    }
}
