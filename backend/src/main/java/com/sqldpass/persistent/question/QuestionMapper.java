package com.sqldpass.persistent.question;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqldpass.domain.question.Question;

public class QuestionMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private QuestionMapper() {
    }

    public static Question toDomain(QuestionEntity entity) {
        return new Question(
                entity.getId(),
                entity.getSubject().getId(),
                entity.getContent(),
                entity.getQuestionType(),
                entity.getCorrectOption(),
                entity.getAnswer(),
                parseKeywords(entity.getKeywords()),
                entity.getExplanation(),
                entity.getSummary(),
                entity.getTopic(),
                entity.getDifficulty());
    }

    /** DB에 저장된 JSON 문자열을 List<String>으로 파싱. 파싱 실패 시 빈 리스트. */
    public static List<String> parseKeywords(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }
}
