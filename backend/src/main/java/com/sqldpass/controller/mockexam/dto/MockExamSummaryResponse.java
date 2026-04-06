package com.sqldpass.controller.mockexam.dto;

import java.time.LocalDateTime;

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.persistent.mockexam.ExamType;

public record MockExamSummaryResponse(
        Long id,
        String name,
        ExamType examType,
        int sequence,
        int totalQuestions,
        LocalDateTime createdAt
) {
    public static MockExamSummaryResponse from(MockExam mockExam) {
        return new MockExamSummaryResponse(
                mockExam.getId(),
                mockExam.getName(),
                mockExam.getExamType(),
                mockExam.getSequence(),
                mockExam.getTotalQuestions(),
                mockExam.getCreatedAt());
    }
}
