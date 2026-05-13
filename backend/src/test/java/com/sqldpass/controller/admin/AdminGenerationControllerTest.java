package com.sqldpass.controller.admin;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.controller.admin.dto.GenerationStatusResponse;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.generation.GenerationLockService;
import com.sqldpass.service.generation.QuestionGenerationService;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.timeout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminGenerationController.class)
class AdminGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionGenerationService questionGenerationService;

    @MockitoBean
    private GenerationLockService generationLockService;

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
    @DisplayName("GET /api/admin/generate/status returns the generation status")
    void getStatus() throws Exception {
        given(generationLockService.getStatus())
                .willReturn(new GenerationStatusResponse("RUNNING", null, LocalDateTime.of(2026, 4, 2, 12, 0)));

        mockMvc.perform(get("/api/admin/generate/status").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    @DisplayName("POST /api/admin/generate/reset resets the generation status")
    void reset() throws Exception {
        mockMvc.perform(post("/api/admin/generate/reset").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());

        then(generationLockService).should().reset();
    }

    @Test
    @DisplayName("POST /api/admin/generate accepts a valid count")
    void generate() throws Exception {
        mockMvc.perform(post("/api/admin/generate")
                        .header("Authorization", AUTH_HEADER)
                        .param("count", "5"))
                .andExpect(status().isAccepted());

        then(questionGenerationService).should(timeout(1000)).generateAll(eq(5), any());
    }

    @Test
    @DisplayName("POST /api/admin/generate returns 400 for an invalid count")
    void generate_invalidCount() throws Exception {
        mockMvc.perform(post("/api/admin/generate")
                        .header("Authorization", AUTH_HEADER)
                        .param("count", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
