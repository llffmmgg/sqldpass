package com.sqldpass.controller.question;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.domain.question.Question;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.question.QuestionService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionController.class)
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    @Test
    @DisplayName("GET /api/questions 200 OK - 정답과 해설이 포함되지 않는다")
    void getQuestions() throws Exception {
        Question q = new Question(1L, 5L, "문제 내용", 2, "해설");
        given(questionService.getRandomQuestions(5L, 10)).willReturn(List.of(q));

        mockMvc.perform(get("/api/questions").param("subjectId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].content").value("문제 내용"))
                .andExpect(jsonPath("$[0].correctOption").doesNotExist())
                .andExpect(jsonPath("$[0].explanation").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/questions/{id} 200 OK - 정답과 해설이 포함된다")
    void getQuestion() throws Exception {
        Question q = new Question(1L, 5L, "문제 내용", 2, "해설입니다");
        given(questionService.getQuestion(1L)).willReturn(q);

        mockMvc.perform(get("/api/questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctOption").value(2))
                .andExpect(jsonPath("$.explanation").value("해설입니다"));
    }

    @Test
    @DisplayName("GET /api/questions/{id} 404 - 존재하지 않는 문제")
    void getQuestion_notFound() throws Exception {
        given(questionService.getQuestion(999L))
                .willThrow(new SqldpassException(ErrorCode.QUESTION_NOT_FOUND));

        mockMvc.perform(get("/api/questions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
    }
}
