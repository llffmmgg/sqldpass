package com.sqldpass.controller.mockexam.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.domain.mockexam.MockExamQuestion;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.question.QuestionType;

public record MockExamDetailResponse(
        Long id,
        String name,
        ExamType examType,
        int sequence,
        int totalQuestions,
        LocalDateTime createdAt,
        boolean expertVerified,
        List<Question> questions
) {
    public record Question(
            Long id,
            int displayOrder,
            String content,
            QuestionType questionType,
            Long subjectId,
            String subjectName
    ) {
        public static Question from(MockExamQuestion q) {
            return new Question(
                    q.getQuestionId(),
                    q.getDisplayOrder(),
                    q.getContent(),
                    q.getQuestionType(),
                    q.getSubjectId(),
                    q.getSubjectName());
        }
    }

    public static MockExamDetailResponse from(MockExam mockExam) {
        return new MockExamDetailResponse(
                mockExam.getId(),
                mockExam.getName(),
                mockExam.getExamType(),
                mockExam.getSequence(),
                mockExam.getTotalQuestions(),
                mockExam.getCreatedAt(),
                mockExam.isExpertVerified(),
                mockExam.getQuestions().stream().map(Question::from).toList());
    }
}
