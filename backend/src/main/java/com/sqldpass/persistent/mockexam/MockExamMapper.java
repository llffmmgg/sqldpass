package com.sqldpass.persistent.mockexam;

import java.util.List;

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.domain.mockexam.MockExamQuestion;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.service.grading.SubjectGrouping;

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
                entity.getVisibility(),
                entity.isExpertVerified(),
                entity.getKind(),
                entity.getExamYear(),
                entity.getExamRound(),
                entity.getExamDate());
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
                entity.getVisibility(),
                entity.isExpertVerified(),
                entity.getKind(),
                entity.getExamYear(),
                entity.getExamRound(),
                entity.getExamDate());
    }

    public static MockExamQuestion toDomain(QuestionEntity q) {
        // 합격 기준 과목 단위로 매핑 — 자격증마다 트리 구조가 다르므로 SubjectGrouping 사용.
        SubjectEntity leaf = q.getSubject();
        SubjectEntity shown = SubjectGrouping.groupOf(
                leaf,
                q.getMockExam() != null ? q.getMockExam().getExamType() : null);
        if (shown == null) shown = leaf;
        return new MockExamQuestion(
                q.getId(),
                q.getDisplayOrder() != null ? q.getDisplayOrder() : 0,
                q.getContent(),
                q.getQuestionType(),
                shown.getId(),
                shown.getName());
    }
}
