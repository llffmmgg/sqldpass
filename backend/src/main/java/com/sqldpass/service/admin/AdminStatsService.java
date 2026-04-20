package com.sqldpass.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.dto.AdminStatsResponse;
import com.sqldpass.controller.admin.dto.AdminStatsResponse.SubjectSolveStats;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.solve.SolveRepository;

@Service
@Transactional(readOnly = true)
public class AdminStatsService {

    private final AdminQuestionService adminQuestionService;
    private final MemberRepository memberRepository;
    private final SolveRepository solveRepository;

    public AdminStatsService(AdminQuestionService adminQuestionService,
                             MemberRepository memberRepository,
                             SolveRepository solveRepository) {
        this.adminQuestionService = adminQuestionService;
        this.memberRepository = memberRepository;
        this.solveRepository = solveRepository;
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

        return new AdminStatsResponse(
                adminQuestionService.countAll(),
                adminQuestionService.countVerified(),
                adminQuestionService.countUnverified(),
                memberRepository.count(),
                solveRepository.count(),
                adminQuestionService.countToday(),
                memberRepository.countByCreatedAtAfter(startOfToday),
                solveRepository.countByCreatedAtAfter(startOfToday),
                subjectStats);
    }
}
