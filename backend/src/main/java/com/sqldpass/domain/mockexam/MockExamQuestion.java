package com.sqldpass.domain.mockexam;

import com.sqldpass.persistent.question.QuestionType;

import lombok.Getter;

@Getter
public class MockExamQuestion {

    private final Long questionId;
    private final int displayOrder;
    private final String content;
    private final QuestionType questionType;
    private final Long subjectId;
    private final String subjectName;

    public MockExamQuestion(Long questionId, int displayOrder, String content,
                            QuestionType questionType, Long subjectId, String subjectName) {
        this.questionId = questionId;
        this.displayOrder = displayOrder;
        this.content = content;
        this.questionType = questionType != null ? questionType : QuestionType.MCQ;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
    }
}
