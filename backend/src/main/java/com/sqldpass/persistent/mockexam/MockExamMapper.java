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
                entity.getSequence(),
                entity.getCreatedAt(),
                questions);
    }

    /** 문제 카운트만 (목록 조회) */
    public static MockExam toSummary(MockExamEntity entity, int totalQuestionCount) {
        return new MockExam(
                entity.getId(),
                entity.getName(),
                entity.getSequence(),
                entity.getCreatedAt(),
                totalQuestionCount);
    }

    public static MockExamQuestion toDomain(QuestionEntity q) {
        // 실제 SQLD 시험처럼 상위 과목(1과목/2과목)을 표시. parent가 없으면 본인으로 폴백.
        SubjectEntity leaf = q.getSubject();
        SubjectEntity shown = leaf.getParent() != null ? leaf.getParent() : leaf;
        return new MockExamQuestion(
                q.getId(),
                q.getDisplayOrder() != null ? q.getDisplayOrder() : 0,
                q.getContent(),
                shown.getId(),
                shown.getName());
    }
}
