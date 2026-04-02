package com.sqldpass.controller.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.solve.SolveRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "관리자 - 통계", description = "통계 API")
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final QuestionRepository questionRepository;
    private final MemberRepository memberRepository;
    private final SolveRepository solveRepository;

    public AdminStatsController(QuestionRepository questionRepository,
                                MemberRepository memberRepository,
                                SolveRepository solveRepository) {
        this.questionRepository = questionRepository;
        this.memberRepository = memberRepository;
        this.solveRepository = solveRepository;
    }

    @GetMapping
    @Operation(summary = "전체 통계 조회")
    public AdminStatsResponse getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return new AdminStatsResponse(
                questionRepository.count(),
                memberRepository.count(),
                solveRepository.count(),
                questionRepository.countByCreatedAtAfter(todayStart));
    }
}
