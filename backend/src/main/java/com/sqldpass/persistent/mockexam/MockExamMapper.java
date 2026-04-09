package com.sqldpass.persistent.mockexam;

import java.util.List;

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.domain.mockexam.MockExamQuestion;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.subject.SubjectEntity;

public class MockExamMapper {

    private MockExamMapper() {
    }

    /** 문제 목록 포함 (상세 조회) */
    public static MockExam toDomain(MockExamEntity entity) {
        List<MockExamQuestion> questions = entity.getQuestions().stream()
                .map(MockExamMapper::toDomain)
                .toList();
        return new MockExam(
                entity.getId(),
                entity.getName(),
                entity.getExamType(),
                entity.getSequence(),
                entity.getCreatedAt(),
                questions,
                entity.getTemplate(),
                entity.getVisibility());
    }

    /** 문제 카운트 + 난이도 통계 (목록 조회용) */
    public static MockExam toSummary(MockExamEntity entity, int totalQuestionCount,
                                     Double avgDifficulty, Integer minDifficulty, Integer maxDifficulty) {
        return new MockExam(
                entity.getId(),
                entity.getName(),
                entity.getExamType(),
                entity.getSequence(),
                entity.getCreatedAt(),
                totalQuestionCount,
                avgDifficulty,
                minDifficulty,
                maxDifficulty,
                entity.getTemplate(),
                entity.getVisibility());
    }

    public static MockExamQuestion toDomain(QuestionEntity q) {
        // 상위 과목 표시 — SQLD는 "1과목:..", 정보처리기사는 "정보처리기사 실기". parent 없으면 본인.
        SubjectEntity leaf = q.getSubject();
        SubjectEntity shown = leaf.getParent() != null ? leaf.getParent() : leaf;
        return new MockExamQuestion(
                q.getId(),
                q.getDisplayOrder() != null ? q.getDisplayOrder() : 0,
                q.getContent(),
                q.getQuestionType(),
                shown.getId(),
                shown.getName());
    }
}
