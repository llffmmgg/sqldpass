package com.sqldpass.controller.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.controller.admin.dto.AdminMemberDashboardResponse;
import com.sqldpass.controller.admin.dto.AdminMemberResponse;
import com.sqldpass.service.admin.AdminMemberService;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.solve.SolveService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMemberController.class)
class AdminMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMemberService adminMemberService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    @MockitoBean
    private SolveService solveService;

    private static final String AUTH_HEADER = "Bearer admin-token";

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken("admin-token")).willReturn(true);
    }

    @Test
    @DisplayName("GET /api/admin/members returns paged member summaries")
    void getMembers() throws Exception {
        AdminMemberResponse member = new AdminMemberResponse(
                1L,
                "google",
                "tester",
                LocalDateTime.of(2026, 4, 1, 9, 0),
                120,
                80,
                30,
                7);
        given(adminMemberService.getMembers(0, 20, "default", "desc", null))
                .willReturn(new PageImpl<>(List.of(member)));

        mockMvc.perform(get("/api/admin/members").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nickname").value("tester"))
                .andExpect(jsonPath("$.content[0].streakDays").value(7));
    }

    @Test
    @DisplayName("GET /api/admin/members/{memberId}/dashboard returns member dashboard details")
    void getMemberDashboard() throws Exception {
        AdminMemberDashboardResponse response = new AdminMemberDashboardResponse(
                new AdminMemberDashboardResponse.MemberInfo(1L, "tester", "google",
                        LocalDateTime.of(2026, 4, 1, 9, 0)),
                new AdminMemberDashboardResponse.Stats(120, 90, 75, 7, 12),
                List.of(new AdminMemberDashboardResponse.DailyActivity("2026-04-08", 15)),
                List.of(new AdminMemberDashboardResponse.SubjectStat(10L, "SQL Basics", 40, 32, 80)),
                List.of(new AdminMemberDashboardResponse.WeakSubject(11L, "Normalization", 5, 50)),
                List.of(new AdminMemberDashboardResponse.RecentSolve(
                        100L, LocalDateTime.of(2026, 4, 8, 18, 0), 10, 8, 10L, null)));
        given(adminMemberService.getDashboard(1L)).willReturn(response);

        mockMvc.perform(get("/api/admin/members/1/dashboard").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(1))
                .andExpect(jsonPath("$.stats.totalSolved").value(120))
                .andExpect(jsonPath("$.weakSubjects[0].subjectName").value("Normalization"))
                .andExpect(jsonPath("$.recentSolves[0].id").value(100));
    }
}
