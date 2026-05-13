package com.sqldpass.controller.member;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.controller.member.dto.MemberMeResponse;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.member.MemberService;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

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
    @DisplayName("GET /api/members/me returns the current member profile")
    void getMe() throws Exception {
        MemberMeResponse response = new MemberMeResponse(
                1L, "tester", "google", LocalDateTime.of(2026, 4, 1, 10, 0));
        given(memberService.getMe(1L)).willReturn(response);

        mockMvc.perform(get("/api/members/me").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nickname").value("tester"))
                .andExpect(jsonPath("$.provider").value("google"));
    }

    @Test
    @DisplayName("PATCH /api/members/me/nickname updates the nickname")
    void updateNickname() throws Exception {
        MemberMeResponse response = new MemberMeResponse(
                1L, "updated-name", "google", LocalDateTime.of(2026, 4, 1, 10, 0));
        given(memberService.updateNickname(1L, "updated-name")).willReturn(response);

        mockMvc.perform(patch("/api/members/me/nickname")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"updated-name"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("updated-name"));
    }

    @Test
    @DisplayName("PATCH /api/members/me/nickname returns 400 for invalid input")
    void updateNickname_invalidInput() throws Exception {
        mockMvc.perform(patch("/api/members/me/nickname")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/members/me returns 401 without a token")
    void getMe_unauthorized() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }
}
