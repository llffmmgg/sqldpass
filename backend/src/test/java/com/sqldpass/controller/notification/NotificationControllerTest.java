package com.sqldpass.controller.notification;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.domain.notification.Notification;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.notification.NotificationService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    private static final String AUTH_HEADER = "Bearer member-token";

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken("member-token")).willReturn(true);
        given(jwtProvider.extractMemberId("member-token")).willReturn(1L);
    }

    @Test
    @DisplayName("GET /api/notifications returns the current member notifications")
    void list() throws Exception {
        Notification notification = new Notification(
                1L, 1L, "FEEDBACK_RESOLVED", "Resolved", "Body", "/mypage/feedback", 10L,
                null, LocalDateTime.of(2026, 4, 2, 12, 0));
        given(notificationService.list(1L, 0, 20))
                .willReturn(new PageImpl<>(List.of(notification)));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", AUTH_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].type").value("FEEDBACK_RESOLVED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count returns the unread count")
    void unreadCount() throws Exception {
        given(notificationService.unreadCount(1L)).willReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read marks a notification as read")
    void read() throws Exception {
        mockMvc.perform(patch("/api/notifications/1/read").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all marks all notifications as read")
    void readAll() throws Exception {
        given(notificationService.markAllRead(1L)).willReturn(5);

        mockMvc.perform(patch("/api/notifications/read-all").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(5));
    }

    @Test
    @DisplayName("GET /api/notifications returns 401 when Authorization header is missing")
    void list_unauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
