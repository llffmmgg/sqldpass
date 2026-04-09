package com.sqldpass.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.notification.Notification;
import com.sqldpass.persistent.notification.NotificationRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private static final Long MEMBER_A = 1001L;
    private static final Long MEMBER_B = 1002L;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("알림 생성 후 본인 목록과 미읽 카운트에 잡힌다")
    void notify_andList() {
        notificationService.notify(MEMBER_A, "FEEDBACK_RESOLVED", "건의 해결", "내용", "/x", 1L);
        notificationService.notify(MEMBER_B, "FEEDBACK_RESOLVED", "다른사람", null, null, null);

        var page = notificationService.list(MEMBER_A, 0, 20);
        assertThat(page.getContent()).hasSize(1);
        Notification n = page.getContent().get(0);
        assertThat(n.getTitle()).isEqualTo("건의 해결");
        assertThat(n.getReadAt()).isNull();

        assertThat(notificationService.unreadCount(MEMBER_A)).isEqualTo(1);
        assertThat(notificationService.unreadCount(MEMBER_B)).isEqualTo(1);
    }

    @Test
    @DisplayName("markRead — 본인 알림은 읽음, 미읽 카운트 0")
    void markRead_self() {
        Notification created = notificationService.notify(MEMBER_A, "T", "t", null, null, null);

        notificationService.markRead(MEMBER_A, created.getId());

        assertThat(notificationService.unreadCount(MEMBER_A)).isZero();
    }

    @Test
    @DisplayName("markRead — 남의 알림은 FORBIDDEN")
    void markRead_otherUser() {
        Notification created = notificationService.notify(MEMBER_A, "T", "t", null, null, null);

        assertThatThrownBy(() -> notificationService.markRead(MEMBER_B, created.getId()))
                .isInstanceOf(SqldpassException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("markAllRead — 본인 미읽만 처리")
    void markAllRead_onlySelf() {
        notificationService.notify(MEMBER_A, "T", "1", null, null, null);
        notificationService.notify(MEMBER_A, "T", "2", null, null, null);
        notificationService.notify(MEMBER_B, "T", "3", null, null, null);

        int updated = notificationService.markAllRead(MEMBER_A);

        assertThat(updated).isEqualTo(2);
        assertThat(notificationService.unreadCount(MEMBER_A)).isZero();
        assertThat(notificationService.unreadCount(MEMBER_B)).isEqualTo(1);
    }
}
