package com.sqldpass.service.feedback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.feedback.Feedback;
import com.sqldpass.persistent.feedback.FeedbackEntity;
import com.sqldpass.persistent.feedback.FeedbackRepository;
import com.sqldpass.persistent.feedback.FeedbackStatus;
import com.sqldpass.persistent.feedback.FeedbackType;
import com.sqldpass.persistent.notification.NotificationRepository;
import com.sqldpass.service.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class FeedbackServiceReplyTest {

    @Autowired private FeedbackService feedbackService;
    @Autowired private FeedbackRepository feedbackRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private jakarta.persistence.EntityManager em;

    private static final Long MEMBER_ID = 9001L;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllByMemberId(MEMBER_ID);
        em.flush();
    }

    @Test
    @DisplayName("reply — adminReply, repliedAt 세팅 + status RESOLVED + 알림 1건 (body=reply)")
    void reply_firstTime() {
        FeedbackEntity saved = feedbackRepository.save(
                new FeedbackEntity(FeedbackType.OTHER, MEMBER_ID, null, "원본 건의", null));
        em.flush();

        Feedback updated = feedbackService.reply(saved.getId(), "이미 반영했습니다 — 다음 배포에 포함됩니다.");
        em.flush();

        assertThat(updated.getStatus()).isEqualTo(FeedbackStatus.RESOLVED);
        assertThat(updated.getAdminReply()).isEqualTo("이미 반영했습니다 — 다음 배포에 포함됩니다.");
        assertThat(updated.getRepliedAt()).isNotNull();

        var notifs = notificationService.list(MEMBER_ID, 0, 20);
        assertThat(notifs.getContent()).hasSize(1);
        assertThat(notifs.getContent().get(0).getBody()).isEqualTo("이미 반영했습니다 — 다음 배포에 포함됩니다.");
        assertThat(notifs.getContent().get(0).getTitle()).isEqualTo("건의사항에 답변이 도착했습니다");
    }

    @Test
    @DisplayName("reply — 이미 RESOLVED 인 피드백에 답변 수정 시 알림 중복 생성 안 됨")
    void reply_noDuplicateNotification() {
        FeedbackEntity saved = feedbackRepository.save(
                new FeedbackEntity(FeedbackType.OTHER, MEMBER_ID, null, "원본", null));
        em.flush();

        feedbackService.reply(saved.getId(), "첫 답변");
        em.flush();
        feedbackService.reply(saved.getId(), "수정된 답변");
        em.flush();

        long count = notificationRepository.countByMemberIdAndReadAtIsNull(MEMBER_ID);
        assertThat(count).isEqualTo(1);

        FeedbackEntity reloaded = feedbackRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAdminReply()).isEqualTo("수정된 답변");
    }
}
