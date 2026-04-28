package com.sqldpass.persistent.mockexam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * NEW 뱃지 트리거 타임스탬프(publishedAt, pastExamLinkedAt) 동작 검증.
 */
class MockExamEntityTimestampsTest {

    @Test
    void newDraftEntityHasNullTimestamps() {
        MockExamEntity exam = new MockExamEntity("test", ExamType.SQLD, 1);

        assertNull(exam.getPublishedAt(), "신규 DRAFT는 publishedAt null");
        assertNull(exam.getPastExamLinkedAt(), "신규 DRAFT는 pastExamLinkedAt null");
    }

    @Test
    void changeVisibilityFromDraftToPublishedSetsPublishedAt() {
        MockExamEntity exam = new MockExamEntity("test", ExamType.SQLD, 1);

        exam.changeVisibility(MockExamVisibility.PUBLISHED);

        assertNotNull(exam.getPublishedAt(), "DRAFT->PUBLISHED 시 publishedAt 세팅");
        assertEquals(MockExamVisibility.PUBLISHED, exam.getVisibility());
    }

    @Test
    void changeVisibilityFromPublishedToPremiumPreservesPublishedAt() {
        MockExamEntity exam = new MockExamEntity("test", ExamType.SQLD, 1);
        exam.changeVisibility(MockExamVisibility.PUBLISHED);
        var firstPublishedAt = exam.getPublishedAt();

        exam.changeVisibility(MockExamVisibility.PREMIUM);

        assertEquals(firstPublishedAt, exam.getPublishedAt(),
                "PUBLISHED<->PREMIUM 사이는 publishedAt 보존 — 한 번 공개된 시점은 변하지 않음");
    }

    @Test
    void changeVisibilityBackToDraftPreservesPublishedAt() {
        MockExamEntity exam = new MockExamEntity("test", ExamType.SQLD, 1);
        exam.changeVisibility(MockExamVisibility.PUBLISHED);
        var firstPublishedAt = exam.getPublishedAt();

        exam.changeVisibility(MockExamVisibility.DRAFT);

        // DRAFT 로 되돌려도 과거에 한 번 공개된 시점은 기록으로 보존(다시 공개 시 같은 시점 사용)
        assertEquals(firstPublishedAt, exam.getPublishedAt());
    }

    @Test
    void promoteToPastExamSetsPastExamLinkedAt() {
        MockExamEntity exam = new MockExamEntity("test", ExamType.SQLD, 1);

        exam.promoteToPastExam(2024, 1, LocalDate.of(2024, 3, 15));

        assertNotNull(exam.getPastExamLinkedAt());
        assertEquals(MockExamKind.PAST_EXAM, exam.getKind());
        assertEquals(Integer.valueOf(2024), exam.getExamYear());
    }

    @Test
    void demoteToAiClearsPastExamLinkedAt() {
        MockExamEntity exam = new MockExamEntity("test", ExamType.SQLD, 1);
        exam.promoteToPastExam(2024, 1, LocalDate.of(2024, 3, 15));

        exam.demoteToAi();

        assertNull(exam.getPastExamLinkedAt());
        assertEquals(MockExamKind.AI, exam.getKind());
    }
}
