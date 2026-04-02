package com.sqldpass.service.generation;

public record GeneratedQuestion(String content, int correctOption, String explanation, String summary,
                                String topic, Integer difficulty) {

    public GeneratedQuestion(String content, int correctOption, String explanation, String summary) {
        this(content, correctOption, explanation, summary, null, null);
    }
}
