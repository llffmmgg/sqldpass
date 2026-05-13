package com.sqldpass.controller.notice;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.domain.notice.SiteNotice;
import com.sqldpass.persistent.notice.NoticeDisplayType;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notice.SiteNoticeService;
import com.sqldpass.service.notification.DiscordNotifier;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoticeController.class)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteNoticeService siteNoticeService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    @Test
    @DisplayName("GET /api/notices/active returns the active notice")
    void active() throws Exception {
        SiteNotice notice = new SiteNotice(
                1L,
                NoticeDisplayType.BANNER,
                "Scheduled maintenance",
                "We will be back shortly.",
                true,
                2,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 2, 9, 0));
        given(siteNoticeService.getActive(NoticeDisplayType.BANNER)).willReturn(Optional.of(notice));

        mockMvc.perform(get("/api/notices/active").param("type", "BANNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.displayType").value("BANNER"))
                .andExpect(jsonPath("$.title").value("Scheduled maintenance"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("GET /api/notices/active returns 204 when no active notice exists")
    void active_noContent() throws Exception {
        given(siteNoticeService.getActive(NoticeDisplayType.MODAL)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/notices/active").param("type", "MODAL"))
                .andExpect(status().isNoContent());
    }
}
