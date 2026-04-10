package com.sqldpass.controller.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.solve.SolveAnswerEntity;
import com.sqldpass.persistent.solve.SolveEntity;

public record AdminSolveDetailResponse(
        Long solveId,
        Long memberId,
        String memberNickname,
        Long mockExamId,
        int totalCount,
        int correctCount,
        int score,
        LocalDateTime solvedAt,
        List<AnswerDetail> answers) {

    public record AnswerDetail(
            Long questionId,
            String questionContent,
            String subjectName,
            String questionType,
            Integer selectedOption,
            Integer correctOption,
            String userAnswerText,
            String correctAnswer,
            boolean correct,
            String explanation) {}

    public static AdminSolveDetailResponse from(SolveEntity entity) {
        int total = entity.getAnswers().size();
        int correct = (int) entity.getAnswers().stream().filter(SolveAnswerEntity::isCorrect).count();
        int score = total > 0 ? Math.round((float) correct / total * 100) : 0;

        List<AnswerDetail> answers = entity.getAnswers().stream()
                .map(a -> new AnswerDetail(
                        a.getQuestion().getId(),
                        a.getQuestion().getContent(),
                        a.getQuestion().getSubject().getName(),
                        a.getQuestion().getQuestionType() != null
                                ? a.getQuestion().getQuestionType().name() : "MCQ",
                        a.getSelectedOption(),
                        a.getCorrectOption(),
                        a.getUserAnswerText(),
                        a.getQuestion().getAnswer(),
                        a.isCorrect(),
                        a.getQuestion().getExplanation()))
                .toList();

        return new AdminSolveDetailResponse(
                entity.getId(),
                entity.getMember().getId(),
                entity.getMember().getNickname(),
                entity.getMockExam() != null ? entity.getMockExam().getId() : null,
                total, correct, score,
                entity.getCreatedAt(),
                answers);
    }
}
