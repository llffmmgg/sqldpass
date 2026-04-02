package com.sqldpass.service.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.admin.dto.AdminStatsResponse;
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
        return new AdminStatsResponse(
                adminQuestionService.countAll(),
                memberRepository.count(),
                solveRepository.count(),
                adminQuestionService.countToday());
    }
}
