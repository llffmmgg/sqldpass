package com.sqldpass.controller.wronganswer;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.controller.wronganswer.dto.WrongAnswerResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerRetryResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerStatsResponse;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.wronganswer.WrongAnswerService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WrongAnswerController.class)
class WrongAnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WrongAnswerService wrongAnswerService;

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
    @DisplayName("GET /api/wrong-answers returns the wrong-answer list")
    void getWrongAnswers() throws Exception {
        WrongAnswerResponse response = new WrongAnswerResponse(
                10L, "Question content", "MCQ", 7L, "SQL Basics", 3, LocalDateTime.of(2026, 4, 2, 11, 0));
        given(wrongAnswerService.getWrongAnswers(1L, 7L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/wrong-answers")
                        .header("Authorization", AUTH_HEADER)
                        .param("subjectId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionId").value(10))
                .andExpect(jsonPath("$[0].questionType").value("MCQ"))
                .andExpect(jsonPath("$[0].subjectName").value("SQL Basics"))
                .andExpect(jsonPath("$[0].wrongCount").value(3));
    }

    @Test
    @DisplayName("GET /api/wrong-answers/stats returns subject statistics")
    void getStats() throws Exception {
        WrongAnswerStatsResponse response = new WrongAnswerStatsResponse(7L, "SQL Basics", 20, 8, 40);
        given(wrongAnswerService.getStats(1L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/wrong-answers/stats").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectId").value(7))
                .andExpect(jsonPath("$[0].wrongRate").value(40));
    }

    @Test
    @DisplayName("POST /api/wrong-answers/{questionId}/retry returns retry result")
    void retry() throws Exception {
        WrongAnswerRetryResponse response = new WrongAnswerRetryResponse(true, 2, null, "Explanation");
        given(wrongAnswerService.retry(1L, 10L, 2, null)).willReturn(response);

        mockMvc.perform(post("/api/wrong-answers/10/retry")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOption":2,"answerText":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.correctOption").value(2))
                .andExpect(jsonPath("$.explanation").value("Explanation"));
    }

    @Test
    @DisplayName("GET /api/wrong-answers returns 401 without a token")
    void getWrongAnswers_unauthorized() throws Exception {
        mockMvc.perform(get("/api/wrong-answers"))
                .andExpect(status().isUnauthorized());
    }
}
