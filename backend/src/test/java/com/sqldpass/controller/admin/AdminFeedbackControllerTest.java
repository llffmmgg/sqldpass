package com.sqldpass.controller.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.domain.feedback.Feedback;
import com.sqldpass.persistent.feedback.FeedbackStatus;
import com.sqldpass.persistent.feedback.FeedbackType;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.feedback.FeedbackService;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminFeedbackController.class)
class AdminFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

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
    @DisplayName("GET /api/admin/feedback returns paged feedback responses")
    void list() throws Exception {
        Feedback feedback = new Feedback(
                1L,
                FeedbackType.BUG,
                2L,
                10L,
                "Broken question wording",
                "/questions/10",
                FeedbackStatus.NEW,
                null,
                null,
                LocalDateTime.of(2026, 4, 2, 10, 0),
                LocalDateTime.of(2026, 4, 2, 10, 0));
        given(feedbackService.getAll(FeedbackStatus.NEW, 0, 20))
                .willReturn(new PageImpl<>(List.of(feedback)));
        given(feedbackService.resolveNickname(2L)).willReturn("review-target");

        mockMvc.perform(get("/api/admin/feedback")
                        .header("Authorization", AUTH_HEADER)
                        .param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].memberNickname").value("review-target"))
                .andExpect(jsonPath("$.content[0].status").value("NEW"));
    }

    @Test
    @DisplayName("PATCH /api/admin/feedback/{id}/status updates the feedback status")
    void updateStatus() throws Exception {
        Feedback updated = new Feedback(
                1L,
                FeedbackType.BUG,
                2L,
                10L,
                "Broken question wording",
                "/questions/10",
                FeedbackStatus.RESOLVED,
                null,
                null,
                LocalDateTime.of(2026, 4, 2, 10, 0),
                LocalDateTime.of(2026, 4, 2, 12, 0));
        given(feedbackService.updateStatus(1L, FeedbackStatus.RESOLVED)).willReturn(updated);
        given(feedbackService.resolveNickname(2L)).willReturn("review-target");

        mockMvc.perform(patch("/api/admin/feedback/1/status")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("PATCH /api/admin/feedback/{id}/status returns 400 for null status")
    void updateStatus_invalidInput() throws Exception {
        mockMvc.perform(patch("/api/admin/feedback/1/status")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
