package com.sqldpass.controller.mockexam;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.MockExamPurchaseRepository;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.mockexam.MockExamService;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.payment.PaymentProperties;
import com.sqldpass.service.payment.SubscriptionService;
import com.sqldpass.service.pdf.MockExamPdfService;
import com.sqldpass.service.usage.DailyUsageService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MockExamController.class)
class MockExamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MockExamService mockExamService;

    @MockitoBean
    private SolveRepository solveRepository;

    @MockitoBean
    private MockExamPurchaseRepository mockExamPurchaseRepository;

    @MockitoBean
    private MockExamPdfService mockExamPdfService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private PaymentProperties paymentProperties;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    @MockitoBean
    private DailyUsageService dailyUsageService;

    private void mockAuth(long memberId) {
        given(jwtProvider.validateToken("test-token")).willReturn(true);
        given(jwtProvider.extractMemberId("test-token")).willReturn(memberId);
    }

    private MemberEntity memberWithNickname(String nickname) {
        MemberEntity m = org.mockito.Mockito.mock(MemberEntity.class);
        given(m.getNickname()).willReturn(nickname);
        return m;
    }

    @Test
    @DisplayName("GET /pdf/eligibility — 화이트리스트 비어있고 UNLIMITED 구독자면 true (정식 오픈 모드)")
    void eligibility_openMode_unlimited_true() throws Exception {
        mockAuth(1L);
        given(paymentProperties.reviewerNicknameSet()).willReturn(Set.of());
        given(subscriptionService.allowsPdf(1L)).willReturn(true);

        mockMvc.perform(get("/api/mock-exams/pdf/eligibility")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    @DisplayName("GET /pdf/eligibility — 화이트리스트 비어있고 UNLIMITED 미구독이면 false (정식 오픈 모드)")
    void eligibility_openMode_unlimited_false() throws Exception {
        mockAuth(1L);
        given(paymentProperties.reviewerNicknameSet()).willReturn(Set.of());
        given(subscriptionService.allowsPdf(1L)).willReturn(false);

        mockMvc.perform(get("/api/mock-exams/pdf/eligibility")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false));
    }

    @Test
    @DisplayName("GET /pdf/eligibility — 베타 기간(화이트리스트 있음) + 화이트리스트 닉네임이면 UNLIMITED 무관하게 true")
    void eligibility_betaMode_whitelist_match() throws Exception {
        mockAuth(1L);
        MemberEntity member = memberWithNickname("heehunjung");
        given(paymentProperties.reviewerNicknameSet()).willReturn(Set.of("heehunjung"));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        mockMvc.perform(get("/api/mock-exams/pdf/eligibility")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    @DisplayName("GET /pdf/eligibility — 베타 기간 + 화이트리스트 닉네임이 아니면 false")
    void eligibility_betaMode_whitelist_nomatch() throws Exception {
        mockAuth(1L);
        MemberEntity member = memberWithNickname("randomguy");
        given(paymentProperties.reviewerNicknameSet()).willReturn(Set.of("heehunjung"));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        mockMvc.perform(get("/api/mock-exams/pdf/eligibility")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false));
    }

    @Test
    @DisplayName("GET /{id}/pdf/download — UNLIMITED 미구독이면 베타 화이트리스트 회원이라도 403 PDF_REQUIRES_SUBSCRIPTION")
    void download_unlimited_required_even_for_whitelist() throws Exception {
        mockAuth(1L);
        given(subscriptionService.allowsPdf(1L)).willReturn(false);

        mockMvc.perform(get("/api/mock-exams/42/pdf/download")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PDF_REQUIRES_SUBSCRIPTION"));
    }
}
