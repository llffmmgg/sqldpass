package com.sqldpass.controller.admin.dto;

import java.util.List;

public record AdminStatsResponse(
        long totalQuestions,
        long verifiedQuestions,
        long unverifiedQuestions,
        long totalMembers,
        long totalSolves,
        long totalAnonymousSolves,
        long todayQuestions,
        long todayMembers,
        long todaySolves,
        long todayAnonymousSolves,
        List<SubjectSolveStats> subjectStats,
        /**
         * 자격증별 풀이 활동 (모의고사 / 기출 복원 분리, 누적 + 오늘자).
         * subjectStats 가 free-style 과목 풀이 디테일이라면 이쪽은 자격증 단위 합산.
         * SQLD 의 root subject 가 둘로 나뉘는 문제를 ExamType 단위 집계로 해소한다.
         */
        List<CertActivity> certActivity) {

    public record SubjectSolveStats(
            long subjectId,
            String subjectName,
            long uniqueUsers,
            long solveCount,
            long totalQuestions) {
    }

    public record CertActivity(
            String certSlug,
            String certName,
            ActivityBucket mockExam,
            ActivityBucket pastExam) {
    }

    public record ActivityBucket(
            long totalSolves,
            long totalQuestions,
            long uniqueMembers,
            long todaySolves,
            long todayQuestions,
            long todayUniqueMembers) {

        public static ActivityBucket empty() {
            return new ActivityBucket(0, 0, 0, 0, 0, 0);
        }
    }
}
