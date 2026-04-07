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

    /** 난이도 통계 (목록 조회 시 그룹 쿼리에서 채워짐). 모든 문제의 difficulty가 null 이면 null. */
    private final Double avgDifficulty;
    private final Integer minDifficulty;
    private final Integer maxDifficulty;

    /** 상세 조회용 — 문제 목록 포함. 난이도 통계는 questions에서 직접 계산. */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions) {
        this.id = id;
        this.name = name;
        this.examType = examType != null ? examType : ExamType.SQLD;
        this.sequence = sequence;
        this.createdAt = createdAt;
        this.questions = questions != null ? questions : List.of();
        this.totalQuestions = this.questions.size();
        // 상세 조회 경로에서는 MockExamQuestion에 difficulty가 포함되지 않으므로 null로 둔다.
        // (필요해지면 MockExamQuestion 에 difficulty 필드를 추가할 것)
        this.avgDifficulty = null;
        this.minDifficulty = null;
        this.maxDifficulty = null;
    }

    /** 목록 조회용 — 문제 카운트 + 난이도 통계 */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty) {
        this.id = id;
        this.name = name;
        this.examType = examType != null ? examType : ExamType.SQLD;
        this.sequence = sequence;
        this.createdAt = createdAt;
        this.questions = List.of();
        this.totalQuestions = totalQuestions;
        this.avgDifficulty = avgDifficulty;
        this.minDifficulty = minDifficulty;
        this.maxDifficulty = maxDifficulty;
    }
}
