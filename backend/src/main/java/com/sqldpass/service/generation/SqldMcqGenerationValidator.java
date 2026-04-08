package com.sqldpass.service.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sqldpass.service.generation.dto.GeneratedQuestion;

/**
 * SQLD 객관식 생성 결과의 최소 유효성 검사.
 */
public final class SqldMcqGenerationValidator {

    private SqldMcqGenerationValidator() {
    }

    public static List<String> basicIssues(GeneratedQuestion question) {
        List<String> issues = new ArrayList<>();
        if (question == null) {
            issues.add("문제 응답이 비어 있습니다");
            return issues;
        }
        if (isBlank(question.content())) {
            issues.add("문제 본문이 비어 있습니다");
        }
        if (!isValidCorrectOption(question.correctOption())) {
            issues.add("정답 번호가 1~4 범위를 벗어났습니다");
        }
        if (isBlank(question.explanation())) {
            issues.add("해설이 비어 있습니다");
        }
        if (isBlank(question.summary())) {
            issues.add("요약(summary)이 비어 있습니다");
        }
        return issues;
    }

    public static boolean isValidCorrectOption(Integer correctOption) {
        return correctOption != null && correctOption >= 1 && correctOption <= 4;
    }

    public static String normalizeSummary(String summary) {
        if (isBlank(summary)) {
            return null;
        }
        return summary.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
