package com.sqldpass.domain.mockexam;

import lombok.Getter;

@Getter
public class MockExamQuestion {

    private final Long questionId;
    private final int displayOrder;
    private final String content;
    private final Long subjectId;
    private final String subjectName;

    public MockExamQuestion(Long questionId, int displayOrder, String content, Long subjectId, String subjectName) {
        this.questionId = questionId;
        this.displayOrder = displayOrder;
        this.content = content;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
    }
}
