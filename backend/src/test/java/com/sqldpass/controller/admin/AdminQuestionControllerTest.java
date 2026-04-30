package com.sqldpass.controller.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.controller.admin.dto.AdminQuestionResponse;
import com.sqldpass.controller.admin.dto.QuestionVerifyHistoryResponse;
import com.sqldpass.controller.admin.dto.QuestionVerifyRunResponse;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.question.VerificationCategory;
import com.sqldpass.service.admin.AdminQuestionService;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.admin.QuestionExportService;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminQuestionController.class)
class AdminQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminQuestionService adminQuestionService;

    @MockitoBean
    private QuestionExportService questionExportService;

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
    @DisplayName("GET /api/admin/questions returns paged questions")
    void getQuestions() throws Exception {
        AdminQuestionResponse question = new AdminQuestionResponse(
                1L,
                10L,
                "SQL Basics",
                "What does JOIN do?",
                QuestionType.MCQ,
                2,
                null,
                null,
                "It combines rows.",
                "JOIN basics",
                LocalDateTime.of(2026, 4, 1, 9, 0),
                null,
                VerificationCategory.NONE);
        given(adminQuestionService.getQuestions(10L, null, 0, 20)).willReturn(new PageImpl<>(List.of(question)));

        mockMvc.perform(get("/api/admin/questions")
                        .header("Authorization", AUTH_HEADER)
                        .param("subjectId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].questionType").value("MCQ"))
                .andExpect(jsonPath("$.content[0].subjectName").value("SQL Basics"));
    }

    @Test
    @DisplayName("GET /api/admin/questions/{id} returns question details")
    void getQuestion() throws Exception {
        AdminQuestionResponse question = new AdminQuestionResponse(
                1L,
                10L,
                "SQL Basics",
                "What does JOIN do?",
                QuestionType.MCQ,
                2,
                null,
                null,
                "It combines rows.",
                "JOIN basics",
                LocalDateTime.of(2026, 4, 1, 9, 0),
                null,
                VerificationCategory.NONE);
        given(adminQuestionService.getQuestion(1L)).willReturn(question);

        mockMvc.perform(get("/api/admin/questions/1").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("What does JOIN do?"));
    }

    @Test
    @DisplayName("PUT /api/admin/questions/{id} updates a question")
    void updateQuestion() throws Exception {
        AdminQuestionResponse updated = new AdminQuestionResponse(
                1L,
                10L,
                "SQL Basics",
                "Updated question",
                QuestionType.MCQ,
                3,
                null,
                null,
                "Updated explanation",
                "Updated summary",
                LocalDateTime.of(2026, 4, 1, 9, 0),
                null,
                VerificationCategory.NONE);
        given(adminQuestionService.updateQuestion(eq(1L), any())).willReturn(updated);

        mockMvc.perform(put("/api/admin/questions/1")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Updated question",
                                  "questionType":"MCQ",
                                  "correctOption":3,
                                  "explanation":"Updated explanation",
                                  "summary":"Updated summary"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctOption").value(3))
                .andExpect(jsonPath("$.summary").value("Updated summary"));
    }

    @Test
    @DisplayName("PUT /api/admin/questions/{id} returns 400 for blank content")
    void updateQuestion_invalidInput() throws Exception {
        mockMvc.perform(put("/api/admin/questions/1")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":" ",
                                  "questionType":"MCQ",
                                  "correctOption":3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("DELETE /api/admin/questions/{id} deletes a question")
    void deleteQuestion() throws Exception {
        mockMvc.perform(delete("/api/admin/questions/1").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());

        then(adminQuestionService).should().deleteQuestion(1L);
    }

    @Test
    @DisplayName("GET /api/admin/questions/export returns markdown export with headers")
    void exportQuestions() throws Exception {
        given(questionExportService.export("SQLD", true))
                .willReturn(new QuestionExportService.ExportResult("# SQLD export\n\ncontent", 12));

        mockMvc.perform(get("/api/admin/questions/export")
                        .header("Authorization", AUTH_HEADER)
                        .param("examType", "SQLD")
                        .param("force", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Export-Count", "12"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("sqldpass-sqld-")))
                .andExpect(content().string("# SQLD export\n\ncontent"));
    }

    @Test
    @DisplayName("POST /api/admin/questions/verify returns verification summary")
    void verifyAll() throws Exception {
        QuestionVerifyRunResponse response = new QuestionVerifyRunResponse(
                ExamType.SQLD,
                10L,
                "SQL Basics",
                100,
                false,
                20,
                2,
                1,
                1,
                0,
                LocalDateTime.of(2026, 4, 3, 18, 0),
                List.of(),
                List.of(new QuestionVerifyHistoryResponse(
                        99L,
                        ExamType.SQLD,
                        10L,
                        "SQL Basics",
                        100,
                        false,
                        20,
                        2,
                        1,
                        1,
                        0,
                        LocalDateTime.of(2026, 4, 3, 18, 0))),
                Map.of());
        given(adminQuestionService.verifyAll(ExamType.SQLD, 10L, null, 100, false)).willReturn(response);

        mockMvc.perform(post("/api/admin/questions/verify")
                        .header("Authorization", AUTH_HEADER)
                        .param("examType", "SQLD")
                        .param("subjectId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examType").value("SQLD"))
                .andExpect(jsonPath("$.processedCount").value(20))
                .andExpect(jsonPath("$.recentRuns[0].runId").value(99));
    }

    @Test
    @DisplayName("GET /api/admin/questions/verify/history returns recent runs")
    void getVerifyHistory() throws Exception {
        given(adminQuestionService.getVerifyHistory(3)).willReturn(List.of(
                new QuestionVerifyHistoryResponse(
                        99L,
                        ExamType.SQLD,
                        10L,
                        "SQL Basics",
                        100,
                        false,
                        20,
                        2,
                        1,
                        1,
                        0,
                        LocalDateTime.of(2026, 4, 3, 18, 0))));

        mockMvc.perform(get("/api/admin/questions/verify/history")
                        .header("Authorization", AUTH_HEADER)
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value(99))
                .andExpect(jsonPath("$[0].subjectName").value("SQL Basics"));
    }

    @Test
    @DisplayName("POST /api/admin/questions/export/reset returns reset count")
    void resetExportMark() throws Exception {
        given(questionExportService.resetMark("SQLD")).willReturn(7);

        mockMvc.perform(post("/api/admin/questions/export/reset")
                        .header("Authorization", AUTH_HEADER)
                        .param("examType", "SQLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reset").value(7));
    }
}
