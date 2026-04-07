package com.sqldpass.service.generation.dto;

import java.util.List;

/**
 * AI가 생성한 단일 문제. SQLD MCQ와 정처기 단답/약술형을 모두 표현한다.
 *
 * - SQLD MCQ: content, correctOption, explanation, summary, (topic, difficulty)
 * - 정처기 SHORT_ANSWER/DESCRIPTIVE: content, questionType, answerText, keywords,
 *                                   explanation, summary, (topic, difficulty)
 *
 * correctOption은 Integer(nullable). 정처기 응답에는 이 필드가 없을 수 있어 null 허용.
 */
public record GeneratedQuestion(String content, Integer correctOption, String explanation, String summary,
                                String topic, Integer difficulty,
                                String questionType, String answerText, List<String> keywords) {

    /** SQLD 기존 호출용 6-인자 생성자 (호환 유지, int → Integer 자동 박싱) */
    public GeneratedQuestion(String content, int correctOption, String explanation, String summary,
                             String topic, Integer difficulty) {
        this(content, Integer.valueOf(correctOption), explanation, summary, topic, difficulty, null, null, null);
    }

    /** SQLD 기존 호출용 4-인자 생성자 (호환 유지) */
    public GeneratedQuestion(String content, int correctOption, String explanation, String summary) {
        this(content, Integer.valueOf(correctOption), explanation, summary, null, null, null, null, null);
    }
}
