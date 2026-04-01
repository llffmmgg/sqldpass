package com.sqldpass.persistent.question;

import com.sqldpass.domain.question.Question;

public class QuestionMapper {

    private QuestionMapper() {
    }

    public static Question toDomain(QuestionEntity entity) {
        return new Question(
                entity.getId(),
                entity.getSubject().getId(),
                entity.getContent(),
                entity.getCorrectOption(),
                entity.getExplanation()
        );
    }
}
