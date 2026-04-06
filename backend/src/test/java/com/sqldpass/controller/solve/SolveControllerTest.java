package com.sqldpass.controller.solve;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.controller.solve.dto.SolveRequest;
import com.sqldpass.domain.solve.Solve;
import com.sqldpass.domain.solve.SolveAnswer;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.solve.SolveService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolveController.class)
class SolveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolveService solveService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    private void mockAuth() {
        given(jwtProvider.validateToken("test-token")).willReturn(true);
        given(jwtProvider.extractMemberId("test-token")).willReturn(1L);
    }

    @Test
    @DisplayName("POST /api/solves 201 Created - 채점 결과 반환")
    void submit() throws Exception {
        mockAuth();
        SolveAnswer answer = new SolveAnswer(1L, 42L, 1, 1, true);
        Solve solve = new Solve(1L, 1L, 5L, null, 1, 1, 100, LocalDateTime.now(), List.of(answer));
        given(solveService.solve(eq(1L), any(SolveRequest.class))).willReturn(solve);

        mockMvc.perform(post("/api/solves")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId":5,"answers":[{"questionId":42,"selectedOption":1}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.answers[0].correct").value(true));
    }

    @Test
    @DisplayName("GET /api/solves 200 OK - 풀이 기록 목록")
    void getSolves() throws Exception {
        mockAuth();
        Solve solve = new Solve(1L, 1L, 5L, null, 10, 7, 70, LocalDateTime.now(), List.of());
        given(solveService.getMySolves(1L)).willReturn(List.of(solve));

        mockMvc.perform(get("/api/solves")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(70))
                .andExpect(jsonPath("$[0].totalCount").value(10));
    }
}
