package com.sqldpass.controller.admin;

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

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamDifficulty;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.mockexam.MockExamService;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMockExamController.class)
class AdminMockExamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MockExamService mockExamService;

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
    @DisplayName("GET /api/admin/mock-exams returns mock-exam summaries")
    void list() throws Exception {
        MockExam exam = new MockExam(
                1L,
                "SQLD Mock 1",
                ExamType.SQLD,
                1,
                LocalDateTime.of(2026, 4, 3, 12, 0),
                50,
                2.0,
                1,
                4);
        given(mockExamService.getAll()).willReturn(List.of(exam));

        mockMvc.perform(get("/api/admin/mock-exams").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].difficultyLabel").value("보통"))
                .andExpect(jsonPath("$[0].solved").value(false));
    }

    @Test
    @DisplayName("POST /api/admin/mock-exams uses SQLD defaults when the body is omitted")
    void create_defaultBody() throws Exception {
        MockExam created = new MockExam(
                2L,
                "SQLD Mock 2",
                ExamType.SQLD,
                2,
                LocalDateTime.of(2026, 4, 3, 13, 0),
                50,
                3.0,
                1,
                4);
        given(mockExamService.create(ExamType.SQLD, null, null)).willReturn(created);

        mockMvc.perform(post("/api/admin/mock-exams").header("Authorization", AUTH_HEADER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.examType").value("SQLD"));
    }

    @Test
    @DisplayName("POST /api/admin/mock-exams creates an engineer practical mock exam")
    void create() throws Exception {
        MockExam created = new MockExam(
                3L,
                "Engineer Mock 1",
                ExamType.ENGINEER_PRACTICAL,
                1,
                LocalDateTime.of(2026, 4, 3, 14, 0),
                20,
                4.0,
                2,
                4,
                EngineerExamTemplate.DB_HEAVY);
        given(mockExamService.create(
                ExamType.ENGINEER_PRACTICAL,
                MockExamDifficulty.HARD,
                EngineerExamTemplate.DB_HEAVY)).willReturn(created);

        mockMvc.perform(post("/api/admin/mock-exams")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examType":"ENGINEER_PRACTICAL",
                                  "difficulty":"HARD",
                                  "engineerTemplate":"DB_HEAVY"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.templateKey").value("DB_HEAVY"));
    }

    @Test
    @DisplayName("DELETE /api/admin/mock-exams/{id} deletes the mock exam")
    void deleteMockExam() throws Exception {
        mockMvc.perform(delete("/api/admin/mock-exams/3").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());

        then(mockExamService).should().delete(3L);
    }
}
