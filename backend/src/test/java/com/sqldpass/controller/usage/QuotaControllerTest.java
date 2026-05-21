package com.sqldpass.controller.usage;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.usage.DailyUsageService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuotaController.class)
class QuotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DailyUsageService dailyUsageService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    private static final String AUTH_HEADER = "Bearer test-token";

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken("test-token")).willReturn(true);
        given(jwtProvider.extractMemberId("test-token")).willReturn(1L);
    }

    @Test
    @DisplayName("GET /api/quota — 무료 회원: 사용량/한도/resetAt 반환")
    void quota_free_member() throws Exception {
        given(dailyUsageService.getQuota(1L)).willReturn(new DailyUsageService.Quota(
                18, DailyUsageService.DAILY_QUESTION_LIMIT,
                0, DailyUsageService.DAILY_MOCK_SESSION_LIMIT,
                LocalDateTime.of(2026, 5, 22, 0, 0, 0)));

        mockMvc.perform(get("/api/quota").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionUsed").value(18))
                .andExpect(jsonPath("$.questionLimit").value(30))
                .andExpect(jsonPath("$.mockUsed").value(0))
                .andExpect(jsonPath("$.mockLimit").value(1))
                .andExpect(jsonPath("$.resetAt").value("2026-05-22T00:00:00"));
    }

    @Test
    @DisplayName("GET /api/quota — 활성 구독자: limit=null(무제한)")
    void quota_subscriber_unlimited() throws Exception {
        given(dailyUsageService.getQuota(1L)).willReturn(DailyUsageService.Quota.unlimited(
                LocalDateTime.of(2026, 5, 22, 0, 0, 0)));

        mockMvc.perform(get("/api/quota").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionLimit").doesNotExist())
                .andExpect(jsonPath("$.mockLimit").doesNotExist())
                .andExpect(jsonPath("$.questionUsed").value(0))
                .andExpect(jsonPath("$.mockUsed").value(0));
    }

    @Test
    @DisplayName("GET /api/quota — 토큰 없으면 401")
    void quota_unauthorized() throws Exception {
        mockMvc.perform(get("/api/quota"))
                .andExpect(status().isUnauthorized());
    }
}
