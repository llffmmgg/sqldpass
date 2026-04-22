package com.sqldpass.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.dto.AdminStatsResponse;
import com.sqldpass.controller.admin.dto.AdminStatsResponse.SubjectSolveStats;
import com.sqldpass.controller.admin.dto.AdminTrendResponse;
import com.sqldpass.controller.admin.dto.AdminTrendResponse.DailyPoint;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.solve.AnonymousSolveCountRepository;
import com.sqldpass.persistent.solve.SolveRepository;

@Service
@Transactional(readOnly = true)
public class AdminStatsService {

    private final AdminQuestionService adminQuestionService;
    private final MemberRepository memberRepository;
    private final SolveRepository solveRepository;
    private final AnonymousSolveCountRepository anonymousSolveCountRepository;

    public AdminStatsService(AdminQuestionService adminQuestionService,
                             MemberRepository memberRepository,
                             SolveRepository solveRepository,
                             AnonymousSolveCountRepository anonymousSolveCountRepository) {
        this.adminQuestionService = adminQuestionService;
        this.memberRepository = memberRepository;
        this.solveRepository = solveRepository;
        this.anonymousSolveCountRepository = anonymousSolveCountRepository;
    }

    public AdminStatsResponse getStats() {
        List<SubjectSolveStats> subjectStats = solveRepository.findSubjectSolveStats().stream()
                .map(row -> new SubjectSolveStats(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()))
                .toList();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        long totalAnonymousSolves = anonymousSolveCountRepository.sumAll();
        Long todayAnonymousRaw = anonymousSolveCountRepository.countByDate(LocalDate.now());
        long todayAnonymousSolves = todayAnonymousRaw != null ? todayAnonymousRaw : 0L;

        return new AdminStatsResponse(
                adminQuestionService.countAll(),
                adminQuestionService.countVerified(),
                adminQuestionService.countUnverified(),
                memberRepository.count(),
                solveRepository.count(),
                totalAnonymousSolves,
                adminQuestionService.countToday(),
                memberRepository.countByCreatedAtAfter(startOfToday),
                solveRepository.countByCreatedAtAfter(startOfToday),
                todayAnonymousSolves,
                subjectStats);
    }

    public AdminTrendResponse getTrend(int days) {
        int safe = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(safe - 1L);
        LocalDateTime since = start.atStartOfDay();

        Map<LocalDate, long[]> bucket = new HashMap<>();
        for (int i = 0; i < safe; i++) {
            bucket.put(start.plusDays(i), new long[] { 0L, 0L });
        }

        for (Object[] row : memberRepository.countByDaySince(since)) {
            LocalDate d = toLocalDate(row[0]);
            if (bucket.containsKey(d)) {
                bucket.get(d)[0] = ((Number) row[1]).longValue();
            }
        }
        for (Object[] row : solveRepository.countByDaySince(since)) {
            LocalDate d = toLocalDate(row[0]);
            if (bucket.containsKey(d)) {
                bucket.get(d)[1] = ((Number) row[1]).longValue();
            }
        }

        List<DailyPoint> points = bucket.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DailyPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        return new AdminTrendResponse(safe, points);
    }

    private static LocalDate toLocalDate(Object raw) {
        if (raw instanceof java.sql.Date sql) {
            return sql.toLocalDate();
        }
        if (raw instanceof LocalDate ld) {
            return ld;
        }
        return LocalDate.parse(raw.toString());
    }
}
