package com.sqldpass.controller.question.dto;

import java.util.List;

import com.sqldpass.domain.question.Question;
import com.sqldpass.persistent.question.QuestionType;

/**
 * 문제 상세 응답.
 * - MCQ: correctOption 사용 (1~4), answer/keywords null
 * - SHORT_ANSWER / DESCRIPTIVE: answer(모범답안) + keywords(허용 alias 또는 채점 키워드) 사용
 */
public record QuestionDetailResponse(
        Long id,
        Long subjectId,
        String content,
        QuestionType questionType,
        Integer correctOption,
        String answer,
        List<String> keywords,
        String explanation) {

    public static QuestionDetailResponse from(Question question) {
        return new QuestionDetailResponse(
                question.getId(),
                question.getSubjectId(),
                question.getContent(),
                question.getQuestionType(),
                question.getCorrectOption(),
                question.getAnswer(),
                question.getKeywords(),
                question.getExplanation());
    }
}
