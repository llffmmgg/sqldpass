package com.sqldpass.controller.subject;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.domain.subject.Subject;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.subject.SubjectService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubjectController.class)
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubjectService subjectService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    @Test
    @DisplayName("GET /api/subjects 200 OK - 트리 구조로 반환")
    void getSubjects() throws Exception {
        Subject child = new Subject(3L, 1L, "데이터 모델링의 이해", 1, List.of());
        Subject root = new Subject(1L, null, "1과목: 데이터 모델링의 이해", 1, List.of(child));
        given(subjectService.getSubjectTree()).willReturn(List.of(root));

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("1과목: 데이터 모델링의 이해"))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[0].children[0].id").value(3))
                .andExpect(jsonPath("$[0].children[0].name").value("데이터 모델링의 이해"))
                .andExpect(jsonPath("$[0].children[0].children").isEmpty());
    }

    @Test
    @DisplayName("GET /api/subjects 200 OK - 과목이 없으면 빈 배열")
    void getSubjects_empty() throws Exception {
        given(subjectService.getSubjectTree()).willReturn(List.of());

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
