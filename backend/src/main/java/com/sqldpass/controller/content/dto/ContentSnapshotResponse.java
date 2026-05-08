package com.sqldpass.controller.content.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 안드로이드 앱 첫 부트 prefetch 응답.
 * - {@code version}: ETag 본체 (max(updatedAt) + count 조합).
 *   클라이언트는 응답 헤더의 ETag 와 If-None-Match 로 304 처리.
 * - {@code mockExams}: visibility != DRAFT + expert_verified=true 인 모든 회차(AI/PAST_EXAM)
 *   + 그 안의 모든 문제. 풀이/오답/해설/과목 메타까지 포함해 오프라인 풀이가 가능하다.
 */
public record ContentSnapshotResponse(
        String version,
        LocalDateTime generatedAt,
        int mockExamCount,
        int questionCount,
        List<MockExamSnapshot> mockExams) {

    public record MockExamSnapshot(
            Long id,
            String name,
            String examType,
            int sequence,
            String visibility,
            boolean expertVerified,
            String kind,
            Integer examYear,
            Integer examRound,
            LocalDate examDate,
            String template,
            List<QuestionSnapshot> questions) {}

    public record QuestionSnapshot(
            Long id,
            Integer displayOrder,
            Long subjectId,
            String subjectName,
            String subjectParentName,
            String content,
            String questionType,
            Integer correctOption,
            String answer,
            List<String> keywords,
            String explanation,
            String summary,
            String topic,
            Integer difficulty) {}
}
