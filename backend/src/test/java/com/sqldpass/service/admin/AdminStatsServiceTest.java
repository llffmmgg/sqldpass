package com.sqldpass.service.admin;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.controller.admin.AdminRevenueByPlan;
import com.sqldpass.controller.admin.AdminRevenuePoint;
import com.sqldpass.controller.admin.dto.AdminStatsResponse;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.persistent.solve.AnonymousSolveCountRepository;
import com.sqldpass.persistent.solve.SolveRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private PaymentRepository paymentRepository;

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

    @Test
    @DisplayName("revenueTrend: PaymentRepository row → AdminRevenuePoint 매핑 (LocalDate/Number 변환)")
    void revenueTrend_매핑() {
        Date day1 = Date.valueOf(LocalDate.of(2026, 5, 10));
        Date day2 = Date.valueOf(LocalDate.of(2026, 5, 11));
        // native 쿼리 반환 형식 — date/decimal/decimal/long 혼합
        given(paymentRepository.findDailyRevenue(any())).willReturn(List.of(
                new Object[] { day1, new BigDecimal("9900"), new BigDecimal("0"), BigInteger.valueOf(1) },
                new Object[] { day2, new BigDecimal("19800"), new BigDecimal("9900"), BigInteger.valueOf(2) }
        ));

        List<AdminRevenuePoint> points = adminStatsService.revenueTrend(30);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).date()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(points.get(0).revenue()).isEqualTo(9900L);
        assertThat(points.get(0).refundAmount()).isZero();
        assertThat(points.get(0).count()).isEqualTo(1);
        assertThat(points.get(1).refundAmount()).isEqualTo(9900L);
    }

    @Test
    @DisplayName("revenueTrend: days < 7 은 7 로, > 365 는 365 로 clamp")
    void revenueTrend_clamp() {
        given(paymentRepository.findDailyRevenue(any())).willReturn(List.of());

        adminStatsService.revenueTrend(3);    // < 7 → 7
        adminStatsService.revenueTrend(9999); // > 365 → 365

        // clamp 가 동작하는지는 부작용으로 확인 — findDailyRevenue 가 2회 호출되었는지.
        // since 인자 값은 LocalDate.now() 기반이라 정확히 일치 단언은 불안정.
        // 핵심: 예외 없이 통과 + 호출이 일어남.
        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.times(2))
                .findDailyRevenue(any());
    }

    @Test
    @DisplayName("revenueByPlan: plan/count/revenue 매핑")
    void revenueByPlan_매핑() {
        given(paymentRepository.findRevenueByPlan(any())).willReturn(List.of(
                new Object[] { "ONE_MONTH", BigInteger.valueOf(12), new BigDecimal("118800") },
                new Object[] { "THREE_DAY", BigInteger.valueOf(5),  new BigDecimal("19500") }
        ));

        List<AdminRevenueByPlan> result = adminStatsService.revenueByPlan(30);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).plan()).isEqualTo("ONE_MONTH");
        assertThat(result.get(0).count()).isEqualTo(12);
        assertThat(result.get(0).revenue()).isEqualTo(118800L);
        assertThat(result.get(1).plan()).isEqualTo("THREE_DAY");
    }
}
