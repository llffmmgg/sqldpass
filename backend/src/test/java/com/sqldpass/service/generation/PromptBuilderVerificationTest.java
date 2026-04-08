package com.sqldpass.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.service.generation.dto.AiVerificationRequest;
import com.sqldpass.service.generation.dto.GeneratedQuestion;

class PromptBuilderVerificationTest {

    @Test
    void engineerVerificationPromptIncludesAnswerAndKeywords() {
        GeneratedQuestion question = new GeneratedQuestion(
                "설명형 문제",
                null,
                "해설 내용",
                "요약",
                "운영체제",
                4,
                "DESCRIPTIVE",
                "모범 답안",
                List.of("스케줄링", "교착상태"));

        AiVerificationRequest request = new AiVerificationRequest(
                ExamType.ENGINEER_PRACTICAL, "운영체제", question);

        String systemPrompt = PromptBuilder.buildVerificationSystemPrompt(request);
        String userPrompt = PromptBuilder.buildVerificationPrompt(request);

        assertThat(systemPrompt).contains("questionType");
        assertThat(systemPrompt).contains("answerText");
        assertThat(userPrompt).contains("ENGINEER_PRACTICAL");
        assertThat(userPrompt).contains("DESCRIPTIVE");
        assertThat(userPrompt).contains("모범 답안");
        assertThat(userPrompt).contains("스케줄링, 교착상태");
        assertThat(userPrompt).contains("난이도: 4");
    }

    @Test
    void sqldVerificationPromptIncludesCorrectOptionAndDifficulty() {
        GeneratedQuestion question = new GeneratedQuestion(
                "객관식 문제",
                3,
                "해설 내용",
                "요약",
                "JOIN",
                2,
                "MCQ",
                null,
                null);

        AiVerificationRequest request = new AiVerificationRequest(
                ExamType.SQLD, "SQL 활용", question);

        String systemPrompt = PromptBuilder.buildVerificationSystemPrompt(request);
        String userPrompt = PromptBuilder.buildVerificationPrompt(request);

        assertThat(systemPrompt).contains("SQLD");
        assertThat(userPrompt).contains("SQLD");
        assertThat(userPrompt).contains("MCQ");
        assertThat(userPrompt).contains("정답 번호: 3");
        assertThat(userPrompt).contains("난이도: 2");
    }
}
