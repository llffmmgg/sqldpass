package com.sqldpass.service.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sqldpass.service.generation.dto.GeneratedQuestion;

class SqldMcqGenerationValidatorTest {

    @Test
    void basicIssuesReturnsEmptyForValidQuestion() {
        GeneratedQuestion question = new GeneratedQuestion(
                "문제", 2, "해설", "요약", "JOIN", 3);

        assertTrue(SqldMcqGenerationValidator.basicIssues(question).isEmpty());
    }

    @Test
    void basicIssuesRejectsInvalidPayload() {
        GeneratedQuestion question = new GeneratedQuestion(
                " ", Integer.valueOf(5), "", null, "JOIN", 3, null, null, null);

        assertEquals(
                List.of(
                        "문제 본문이 비어 있습니다",
                        "정답 번호가 1~4 범위를 벗어났습니다",
                        "해설이 비어 있습니다",
                        "요약(summary)이 비어 있습니다"),
                SqldMcqGenerationValidator.basicIssues(question));
    }

    @Test
    void normalizeSummaryNormalizesWhitespaceAndCase() {
        assertEquals(
                "join 결과 비교",
                SqldMcqGenerationValidator.normalizeSummary("  Join   결과 비교  "));
    }
}
