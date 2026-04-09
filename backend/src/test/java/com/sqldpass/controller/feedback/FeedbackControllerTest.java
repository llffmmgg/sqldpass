package com.sqldpass.controller.feedback;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

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
    @DisplayName("POST /api/feedback creates feedback")
    void create() throws Exception {
        Feedback feedback = new Feedback(
                1L,
                FeedbackType.BUG,
                1L,
                10L,
                "There is a bug on the page.",
                "/questions/10",
                FeedbackStatus.NEW,
                null,
                null,
                LocalDateTime.of(2026, 4, 2, 11, 0),
                LocalDateTime.of(2026, 4, 2, 11, 0));
        given(feedbackService.create(eq(1L), any())).willReturn(feedback);
        given(feedbackService.resolveNickname(1L)).willReturn("tester");

        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"BUG","questionId":10,"content":"There is a bug on the page.","pageUrl":"/questions/10"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("BUG"))
                .andExpect(jsonPath("$.memberNickname").value("tester"));
    }

    @Test
    @DisplayName("POST /api/feedback returns 400 for invalid content")
    void create_invalidInput() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"BUG","content":"bad"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
