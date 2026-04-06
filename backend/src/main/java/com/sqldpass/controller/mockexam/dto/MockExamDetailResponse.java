package com.sqldpass.controller.mockexam.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.domain.mockexam.MockExamQuestion;

public record MockExamDetailResponse(
        Long id,
        String name,
        int sequence,
        int totalQuestions,
        LocalDateTime createdAt,
        List<Question> questions
) {
    public record Question(
            Long id,
            int displayOrder,
            String content,
            Long subjectId,
            String subjectName
    ) {
        public static Question from(MockExamQuestion q) {
            return new Question(
                    q.getQuestionId(),
                    q.getDisplayOrder(),
                    q.getContent(),
                    q.getSubjectId(),
                    q.getSubjectName());
        }
    }

    public static MockExamDetailResponse from(MockExam mockExam) {
        return new MockExamDetailResponse(
                mockExam.getId(),
                mockExam.getName(),
                mockExam.getSequence(),
                mockExam.getTotalQuestions(),
                mockExam.getCreatedAt(),
                mockExam.getQuestions().stream().map(Question::from).toList());
    }
}
