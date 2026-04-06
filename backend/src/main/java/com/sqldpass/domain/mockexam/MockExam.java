package com.sqldpass.domain.mockexam;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.mockexam.ExamType;

import lombok.Getter;

@Getter
public class MockExam {

    private final Long id;
    private final String name;
    private final ExamType examType;
    private final int sequence;
    private final int totalQuestions;
    private final LocalDateTime createdAt;
    private final List<MockExamQuestion> questions;

    /** 상세 조회용 — 문제 목록 포함 */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions) {
        this.id = id;
        this.name = name;
        this.examType = examType != null ? examType : ExamType.SQLD;
        this.sequence = sequence;
        this.createdAt = createdAt;
        this.questions = questions != null ? questions : List.of();
        this.totalQuestions = this.questions.size();
    }

    /** 목록 조회용 — 문제 카운트만 */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions) {
        this.id = id;
        this.name = name;
        this.examType = examType != null ? examType : ExamType.SQLD;
        this.sequence = sequence;
        this.createdAt = createdAt;
        this.questions = List.of();
        this.totalQuestions = totalQuestions;
    }
}
