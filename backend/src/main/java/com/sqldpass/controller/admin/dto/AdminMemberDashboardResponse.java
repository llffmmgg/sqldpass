package com.sqldpass.controller.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 어드민이 특정 멤버의 학습 현황을 한눈에 보기 위한 응답 DTO.
 * 풀이/통계/취약 영역/최근 활동을 한 번의 호출로 묶어서 반환.
 */
public record AdminMemberDashboardResponse(
        MemberInfo member,
        Stats stats,
        List<DailyActivity> recentActivity,
        List<SubjectStat> subjectStats,
        List<WeakSubject> weakSubjects,
        List<RecentSolve> recentSolves
) {

    public record MemberInfo(
            Long id,
            String nickname,
            String provider,
            LocalDateTime createdAt
    ) {}

    /** 사용자가 강조한 핵심 지표: totalSolved + streakDays */
    public record Stats(
            int totalSolved,
            int totalCorrect,
            int overallRate,
            int streakDays,
            int totalSessions
    ) {}

    /** 최근 14일 활동 (date = "YYYY-MM-DD") */
    public record DailyActivity(
            String date,
            int count
    ) {}

    public record SubjectStat(
            Long subjectId,
            String subjectName,
            int total,
            int correct,
            int rate
    ) {}

    public record WeakSubject(
            Long subjectId,
            String subjectName,
            int wrongCount,
            int wrongRate
    ) {}

    public record RecentSolve(
            Long id,
            LocalDateTime solvedAt,
            int totalCount,
            int correctCount,
            Long subjectId,
            Long mockExamId
    ) {}
}
