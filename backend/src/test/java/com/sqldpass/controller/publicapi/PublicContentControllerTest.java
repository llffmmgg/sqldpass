package com.sqldpass.controller.publicapi;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicCategoryResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicQuestionDetailResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicSolveQuestionResponse;
import com.sqldpass.controller.publicapi.dto.PublicDtos.PublicSubjectResponse;
import com.sqldpass.controller.publicapi.dto.PublicRankingResponse;
import com.sqldpass.controller.publicapi.dto.PublicStatsResponse;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.publicapi.PastExamPublicService;
import com.sqldpass.service.publicapi.PublicContentService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicContentController.class)
class PublicContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicContentService publicContentService;

    @MockitoBean
    private PastExamPublicService pastExamPublicService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    @Test
    @DisplayName("GET /api/public/stats returns public stats")
    void getStats() throws Exception {
        given(publicContentService.getStats()).willReturn(new PublicStatsResponse(100, 2500));

        mockMvc.perform(get("/api/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(100))
                .andExpect(jsonPath("$.totalSolves").value(2500));
    }

    @Test
    @DisplayName("GET /api/public/ranking returns ranking entries")
    void getRanking() throws Exception {
        PublicRankingResponse response = new PublicRankingResponse(
                List.of(new PublicRankingResponse.Entry(1, "tester", 120)),
                LocalDateTime.of(2026, 4, 2, 12, 0));
        given(publicContentService.getTopRanking()).willReturn(response);

        mockMvc.perform(get("/api/public/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].nickname").value("tester"))
                .andExpect(jsonPath("$.entries[0].totalCorrect").value(120));
    }

    @Test
    @DisplayName("GET /api/public/certs/{certSlug}/categories returns categories")
    void listCategories() throws Exception {
        given(publicContentService.listCategoriesByCert("sqld"))
                .willReturn(List.of(new PublicCategoryResponse(7L, "SQL Basics", "SQLD", 30)));

        mockMvc.perform(get("/api/public/certs/sqld/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].questionCount").value(30));
    }

    @Test
    @DisplayName("GET /api/public/questions/{id} returns a public question detail")
    void getQuestion() throws Exception {
        PublicQuestionDetailResponse response = new PublicQuestionDetailResponse(
                10L, "sqld", "SQLD", 7L, "SQL Basics", "Question content",
                "MCQ", 2, null, List.of(), "Explanation", "joins", 2);
        given(publicContentService.getQuestionDetail(10L)).willReturn(response);

        mockMvc.perform(get("/api/public/questions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.certSlug").value("sqld"))
                .andExpect(jsonPath("$.correctOption").value(2));
    }

    @Test
    @DisplayName("GET /api/public/subjects returns subject tree")
    void listSubjects() throws Exception {
        PublicSubjectResponse leaf = new PublicSubjectResponse(7L, "SQL Basics", 1, List.of());
        PublicSubjectResponse root = new PublicSubjectResponse(1L, "SQLD", 0, List.of(leaf));
        given(publicContentService.getSubjectTree()).willReturn(List.of(root));

        mockMvc.perform(get("/api/public/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("SQLD"))
                .andExpect(jsonPath("$[0].children[0].id").value(7));
    }

    @Test
    @DisplayName("GET /api/public/subjects/{id}/random-questions returns random questions")
    void getRandomQuestions() throws Exception {
        given(publicContentService.getRandomSolveQuestions(eq(7L), eq(10), anyString()))
                .willReturn(List.of(new PublicSolveQuestionResponse(100L, 7L, "Q1", "MCQ")));

        mockMvc.perform(get("/api/public/subjects/7/random-questions?size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].subjectId").value(7))
                .andExpect(jsonPath("$[0].questionType").value("MCQ"));
    }
}
