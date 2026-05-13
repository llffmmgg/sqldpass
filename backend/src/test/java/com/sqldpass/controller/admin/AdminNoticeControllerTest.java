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

import com.sqldpass.domain.notice.SiteNotice;
import com.sqldpass.persistent.notice.NoticeDisplayType;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notice.SiteNoticeService;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminNoticeController.class)
class AdminNoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteNoticeService noticeService;

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
    @DisplayName("GET /api/admin/notices returns notices")
    void list() throws Exception {
        SiteNotice notice = new SiteNotice(
                1L,
                NoticeDisplayType.BANNER,
                "Maintenance",
                "Service will restart soon.",
                true,
                3,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 2, 9, 0));
        given(noticeService.listAll()).willReturn(List.of(notice));

        mockMvc.perform(get("/api/admin/notices").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].displayType").value("BANNER"))
                .andExpect(jsonPath("$[0].version").value(3));
    }

    @Test
    @DisplayName("POST /api/admin/notices creates a notice")
    void create() throws Exception {
        SiteNotice created = new SiteNotice(
                2L,
                NoticeDisplayType.MODAL,
                "Exam Update",
                "New mock exam is available.",
                true,
                1,
                LocalDateTime.of(2026, 4, 3, 9, 0),
                LocalDateTime.of(2026, 4, 3, 9, 0));
        given(noticeService.create(
                NoticeDisplayType.MODAL,
                "Exam Update",
                "New mock exam is available.",
                true)).willReturn(created);

        mockMvc.perform(post("/api/admin/notices")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayType":"MODAL",
                                  "title":"Exam Update",
                                  "body":"New mock exam is available.",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.displayType").value("MODAL"));
    }

    @Test
    @DisplayName("PUT /api/admin/notices/{id} updates a notice")
    void update() throws Exception {
        SiteNotice updated = new SiteNotice(
                2L,
                NoticeDisplayType.BANNER,
                "Updated title",
                "Updated body",
                false,
                2,
                LocalDateTime.of(2026, 4, 3, 9, 0),
                LocalDateTime.of(2026, 4, 4, 9, 0));
        given(noticeService.update(2L, NoticeDisplayType.BANNER, "Updated title", "Updated body", false))
                .willReturn(updated);

        mockMvc.perform(put("/api/admin/notices/2")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayType":"BANNER",
                                  "title":"Updated title",
                                  "body":"Updated body",
                                  "active":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("PATCH /api/admin/notices/{id}/active toggles notice activity")
    void setActive() throws Exception {
        SiteNotice updated = new SiteNotice(
                2L,
                NoticeDisplayType.BANNER,
                "Updated title",
                "Updated body",
                true,
                3,
                LocalDateTime.of(2026, 4, 3, 9, 0),
                LocalDateTime.of(2026, 4, 4, 9, 30));
        given(noticeService.setActive(2L, true)).willReturn(updated);

        mockMvc.perform(patch("/api/admin/notices/2/active")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @DisplayName("DELETE /api/admin/notices/{id} returns no content")
    void deleteNotice() throws Exception {
        mockMvc.perform(delete("/api/admin/notices/2").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());

        then(noticeService).should().delete(2L);
    }

    @Test
    @DisplayName("POST /api/admin/notices returns 400 for blank body")
    void create_invalidInput() throws Exception {
        mockMvc.perform(post("/api/admin/notices")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayType":"BANNER",
                                  "title":"Too short",
                                  "body":"   ",
                                  "active":true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
