package com.sqldpass.domain.mockexam;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamVisibility;

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

    /** 정처기 실기 분포 템플릿. SQLD/컴활/구 정처기는 null. */
    private final EngineerExamTemplate template;

    /** 공개 상태 (DRAFT/PUBLISHED/PREMIUM) */
    private final MockExamVisibility visibility;

    /** 전문가 검증 완료 여부 */
    private final boolean expertVerified;

    /** 상세 조회용 — 문제 목록 포함. 난이도 통계는 questions에서 직접 계산. */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions) {
        this(id, name, examType, sequence, createdAt, questions, null, MockExamVisibility.PUBLISHED, false);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions, EngineerExamTemplate template) {
        this(id, name, examType, sequence, createdAt, questions, template, MockExamVisibility.PUBLISHED, false);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions, EngineerExamTemplate template,
                    MockExamVisibility visibility) {
        this(id, name, examType, sequence, createdAt, questions, template, visibility, false);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions, EngineerExamTemplate template,
                    MockExamVisibility visibility, boolean expertVerified) {
        this.id = id;
        this.name = name;
        this.examType = examType != null ? examType : ExamType.SQLD;
        this.sequence = sequence;
        this.createdAt = createdAt;
        this.questions = questions != null ? questions : List.of();
        this.totalQuestions = this.questions.size();
        this.avgDifficulty = null;
        this.minDifficulty = null;
        this.maxDifficulty = null;
        this.template = template;
        this.visibility = visibility != null ? visibility : MockExamVisibility.PUBLISHED;
        this.expertVerified = expertVerified;
    }

    /** 목록 조회용 — 문제 카운트 + 난이도 통계 */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty) {
        this(id, name, examType, sequence, createdAt, totalQuestions, avgDifficulty, minDifficulty, maxDifficulty, null, MockExamVisibility.PUBLISHED, false);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty,
                    EngineerExamTemplate template) {
        this(id, name, examType, sequence, createdAt, totalQuestions, avgDifficulty, minDifficulty, maxDifficulty, template, MockExamVisibility.PUBLISHED, false);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty,
                    EngineerExamTemplate template, MockExamVisibility visibility) {
        this(id, name, examType, sequence, createdAt, totalQuestions, avgDifficulty, minDifficulty, maxDifficulty, template, visibility, false);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty,
                    EngineerExamTemplate template, MockExamVisibility visibility, boolean expertVerified) {
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
        this.template = template;
        this.visibility = visibility != null ? visibility : MockExamVisibility.PUBLISHED;
        this.expertVerified = expertVerified;
    }
}
