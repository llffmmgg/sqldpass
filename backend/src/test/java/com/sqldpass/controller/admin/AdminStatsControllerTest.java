package com.sqldpass.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import com.sqldpass.controller.admin.dto.AdminStatsResponse;
import com.sqldpass.service.admin.AdminStatsService;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStatsController.class)
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStatsService adminStatsService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    private static final String AUTH_HEADER = "Bearer admin-token";

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken("admin-token")).willReturn(true);
    }

    @Test
    @DisplayName("GET /api/admin/stats returns admin statistics")
    void getStats() throws Exception {
        given(adminStatsService.getStats()).willReturn(
                new AdminStatsResponse(100L, 80L, 20L, 20L, 300L, 150L, 5L, 3L, 50L, 12L, List.of()));

        mockMvc.perform(get("/api/admin/stats").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(100))
                .andExpect(jsonPath("$.totalMembers").value(20))
                .andExpect(jsonPath("$.totalSolves").value(300))
                .andExpect(jsonPath("$.todayQuestions").value(5));
    }

    @Test
    @DisplayName("GET /api/admin/stats returns 401 without an admin token")
    void getStats_unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }
}
