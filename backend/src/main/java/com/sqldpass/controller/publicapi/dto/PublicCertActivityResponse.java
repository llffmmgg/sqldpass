package com.sqldpass.controller.publicapi.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 자격증별 풀이 활동 현황 — 랜딩 페이지의 정교한 통계 섹션용.
 *
 * 모의고사(MockExamKind=AI) 와 기출 복원(MockExamKind=PAST_EXAM) 을
 * 자격증(ExamType) 단위로 분리해 누적·오늘자 집계를 노출한다.
 *
 * - totalSolves: 풀이 세션 수 (한 사람이 한 모의고사를 한 번 푸는 단위)
 * - totalQuestions: 누적 답변한 문항 수의 합
 * - uniqueMembers: 한 번이라도 푼 회원의 수
 * - today*: 서버 기준 자정 이후
 */
public record PublicCertActivityResponse(
        LocalDateTime generatedAt,
        LocalDate today,
        List<CertActivityItem> items
) {
    public record CertActivityItem(
            String certSlug,
            String certName,
            ActivityBucket mockExam,
            ActivityBucket pastExam
    ) {}

    public record ActivityBucket(
            long totalSolves,
            long totalQuestions,
            long uniqueMembers,
            long todaySolves,
            long todayQuestions,
            long todayUniqueMembers
    ) {
        public static ActivityBucket empty() {
            return new ActivityBucket(0, 0, 0, 0, 0, 0);
        }
    }
}
