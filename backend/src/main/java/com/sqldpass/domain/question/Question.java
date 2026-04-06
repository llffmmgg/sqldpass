package com.sqldpass.domain.question;

import java.util.List;

import com.sqldpass.persistent.question.QuestionType;

import lombok.Getter;

@Getter
public class Question {

    private final Long id;
    private final Long subjectId;
    private final String content;
    private final QuestionType questionType;
    /** MCQ일 때만 의미 있음. 비-MCQ는 null */
    private final Integer correctOption;
    /** 비-MCQ의 모범답안 */
    private final String answer;
    /** SHORT_ANSWER alias 또는 DESCRIPTIVE 채점 키워드 */
    private final List<String> keywords;
    private final String explanation;
    private final String summary;
    private final String topic;
    private final Integer difficulty;

    public Question(Long id, Long subjectId, String content,
                    QuestionType questionType, Integer correctOption,
                    String answer, List<String> keywords,
                    String explanation, String summary, String topic, Integer difficulty) {
        this.id = id;
        this.subjectId = subjectId;
        this.content = content;
        this.questionType = questionType != null ? questionType : QuestionType.MCQ;
        this.correctOption = correctOption;
        this.answer = answer;
        this.keywords = keywords != null ? List.copyOf(keywords) : List.of();
        this.explanation = explanation;
        this.summary = summary;
        this.topic = topic;
        this.difficulty = difficulty;
    }

    /** 테스트/기존 SQLD 경로용 간편 생성자 (MCQ 전용) */
    public Question(Long id, Long subjectId, String content, int correctOption, String explanation) {
        this(id, subjectId, content, QuestionType.MCQ, correctOption, null, null, explanation, null, null, null);
    }
}
