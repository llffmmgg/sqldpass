package com.sqldpass.domain.mockexam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamKind;
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

    /** AI 생성 / PAST_EXAM (기출 복원) 구분 */
    private final MockExamKind kind;

    /** 기출 연도 (PAST_EXAM 만 유효) */
    private final Integer examYear;

    /** 기출 회차 (PAST_EXAM 만 유효) */
    private final Integer examRound;

    /** 시험 실시일 (PAST_EXAM 만 유효) */
    private final LocalDate examDate;

    /** DRAFT->PUBLISHED 첫 전환 시각 (NEW 뱃지 트리거) */
    private final LocalDateTime publishedAt;

    /** 기출 복원 승격 시각 (NEW 뱃지 트리거) */
    private final LocalDateTime pastExamLinkedAt;

    /** 상세 조회용 — 문제 목록 포함. 난이도 통계는 questions에서 직접 계산. */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions) {
        this(id, name, examType, sequence, createdAt, questions, null, MockExamVisibility.PUBLISHED, false,
                MockExamKind.AI, null, null, null, null, null);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions, EngineerExamTemplate template) {
        this(id, name, examType, sequence, createdAt, questions, template, MockExamVisibility.PUBLISHED, false,
                MockExamKind.AI, null, null, null, null, null);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions, EngineerExamTemplate template,
                    MockExamVisibility visibility) {
        this(id, name, examType, sequence, createdAt, questions, template, visibility, false,
                MockExamKind.AI, null, null, null, null, null);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions, EngineerExamTemplate template,
                    MockExamVisibility visibility, boolean expertVerified) {
        this(id, name, examType, sequence, createdAt, questions, template, visibility, expertVerified,
                MockExamKind.AI, null, null, null, null, null);
    }

    /** 상세 조회 + 기출 메타까지 포함 */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    List<MockExamQuestion> questions, EngineerExamTemplate template,
                    MockExamVisibility visibility, boolean expertVerified,
                    MockExamKind kind, Integer examYear, Integer examRound, LocalDate examDate,
                    LocalDateTime publishedAt, LocalDateTime pastExamLinkedAt) {
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
        this.kind = kind != null ? kind : MockExamKind.AI;
        this.examYear = examYear;
        this.examRound = examRound;
        this.examDate = examDate;
        this.publishedAt = publishedAt;
        this.pastExamLinkedAt = pastExamLinkedAt;
    }

    /** 목록 조회용 — 문제 카운트 + 난이도 통계 */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty) {
        this(id, name, examType, sequence, createdAt, totalQuestions, avgDifficulty, minDifficulty, maxDifficulty,
                null, MockExamVisibility.PUBLISHED, false, MockExamKind.AI, null, null, null, null, null);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty,
                    EngineerExamTemplate template) {
        this(id, name, examType, sequence, createdAt, totalQuestions, avgDifficulty, minDifficulty, maxDifficulty,
                template, MockExamVisibility.PUBLISHED, false, MockExamKind.AI, null, null, null, null, null);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty,
                    EngineerExamTemplate template, MockExamVisibility visibility) {
        this(id, name, examType, sequence, createdAt, totalQuestions, avgDifficulty, minDifficulty, maxDifficulty,
                template, visibility, false, MockExamKind.AI, null, null, null, null, null);
    }

    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty,
                    EngineerExamTemplate template, MockExamVisibility visibility, boolean expertVerified) {
        this(id, name, examType, sequence, createdAt, totalQuestions, avgDifficulty, minDifficulty, maxDifficulty,
                template, visibility, expertVerified, MockExamKind.AI, null, null, null, null, null);
    }

    /** 목록 조회 + 기출 메타까지 포함 */
    public MockExam(Long id, String name, ExamType examType, int sequence, LocalDateTime createdAt,
                    int totalQuestions, Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty,
                    EngineerExamTemplate template, MockExamVisibility visibility, boolean expertVerified,
                    MockExamKind kind, Integer examYear, Integer examRound, LocalDate examDate,
                    LocalDateTime publishedAt, LocalDateTime pastExamLinkedAt) {
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
        this.kind = kind != null ? kind : MockExamKind.AI;
        this.examYear = examYear;
        this.examRound = examRound;
        this.examDate = examDate;
        this.publishedAt = publishedAt;
        this.pastExamLinkedAt = pastExamLinkedAt;
    }
}
