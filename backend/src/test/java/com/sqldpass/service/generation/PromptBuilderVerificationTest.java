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
                "Explain this program",
                null,
                "Detailed explanation",
                "Summary",
                "OS",
                4,
                "DESCRIPTIVE",
                "model answer",
                List.of("alpha", "beta"));

        AiVerificationRequest request = new AiVerificationRequest(
                ExamType.ENGINEER_PRACTICAL, "OS", question);

        String systemPrompt = PromptBuilder.buildVerificationSystemPrompt(request);
        String userPrompt = PromptBuilder.buildVerificationPrompt(request);

        assertThat(systemPrompt).contains("questionType");
        assertThat(systemPrompt).contains("answerText");
        assertThat(userPrompt).contains("ENGINEER_PRACTICAL");
        assertThat(userPrompt).contains("DESCRIPTIVE");
        assertThat(userPrompt).contains("model answer");
        assertThat(userPrompt).contains("alpha, beta");
        assertThat(userPrompt).contains("4");
    }

    @Test
    void sqldVerificationPromptIncludesCorrectOptionAndDifficulty() {
        GeneratedQuestion question = new GeneratedQuestion(
                "Multiple choice question",
                3,
                "Explanation",
                "Summary",
                "JOIN",
                2,
                "MCQ",
                null,
                null);

        AiVerificationRequest request = new AiVerificationRequest(
                ExamType.SQLD, "SQL", question);

        String systemPrompt = PromptBuilder.buildVerificationSystemPrompt(request);
        String userPrompt = PromptBuilder.buildVerificationPrompt(request);

        assertThat(systemPrompt).contains("SQLD");
        assertThat(userPrompt).contains("SQLD");
        assertThat(userPrompt).contains("MCQ");
        assertThat(userPrompt).contains("3");
        assertThat(userPrompt).contains("2");
    }
}
