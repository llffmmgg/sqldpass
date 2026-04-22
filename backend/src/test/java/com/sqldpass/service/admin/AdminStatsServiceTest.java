package com.sqldpass.service.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.controller.admin.dto.AdminStatsResponse;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.solve.AnonymousSolveCountRepository;
import com.sqldpass.persistent.solve.SolveRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    private AdminQuestionService adminQuestionService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SolveRepository solveRepository;

    @Mock
    private AnonymousSolveCountRepository anonymousSolveCountRepository;

    @InjectMocks
    private AdminStatsService adminStatsService;

    @Test
    @DisplayName("getStats aggregates counts from question, member, and solve sources")
    void getStats() {
        given(adminQuestionService.countAll()).willReturn(100L);
        given(memberRepository.count()).willReturn(20L);
        given(solveRepository.count()).willReturn(300L);
        given(adminQuestionService.countToday()).willReturn(5L);
        given(anonymousSolveCountRepository.sumAll()).willReturn(150L);
        given(anonymousSolveCountRepository.countByDate(org.mockito.ArgumentMatchers.any())).willReturn(12L);

        AdminStatsResponse response = adminStatsService.getStats();

        assertThat(response.totalQuestions()).isEqualTo(100L);
        assertThat(response.totalMembers()).isEqualTo(20L);
        assertThat(response.totalSolves()).isEqualTo(300L);
        assertThat(response.todayQuestions()).isEqualTo(5L);
        assertThat(response.totalAnonymousSolves()).isEqualTo(150L);
        assertThat(response.todayAnonymousSolves()).isEqualTo(12L);
    }
}
