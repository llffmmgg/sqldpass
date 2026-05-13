package com.sqldpass.controller.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.auth.AuthService;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    @Test
    @DisplayName("POST /api/auth/login/google returns a JWT response")
    void loginWithGoogle() throws Exception {
        given(authService.loginWithGoogle("oauth-code", "https://app.example/callback"))
                .willReturn(new AuthService.AuthResult("jwt-token", "tester", true));

        mockMvc.perform(post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"oauth-code","redirectUri":"https://app.example/callback"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.nickname").value("tester"))
                .andExpect(jsonPath("$.isNew").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/login/google returns 400 for invalid input")
    void loginWithGoogle_invalidInput() throws Exception {
        mockMvc.perform(post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"","redirectUri":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
