package com.sqldpass.service.feedback;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.sqldpass.controller.feedback.dto.CreateFeedbackRequest;
import com.sqldpass.domain.feedback.Feedback;
import com.sqldpass.persistent.feedback.FeedbackEntity;
import com.sqldpass.persistent.feedback.FeedbackRepository;
import com.sqldpass.persistent.feedback.FeedbackStatus;
import com.sqldpass.persistent.feedback.FeedbackType;
import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private DiscordNotifier discordNotifier;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    @DisplayName("create saves feedback and sends Discord notification context")
    void create() {
        CreateFeedbackRequest request = new CreateFeedbackRequest(
                FeedbackType.QUESTION_ERROR, 10L, "Question explanation is incorrect.", "/questions/10");
        FeedbackEntity saved = new FeedbackEntity(
                request.type(), 1L, request.questionId(), request.content(), request.pageUrl());
        setId(saved, 7L);

        SubjectEntity subject = new SubjectEntity(null, "SQL Basics", 1);
        QuestionEntity question = new QuestionEntity(subject, "Question content", 2, "Explanation");
        question.update("Question content", 2, "Explanation", "Question summary");

        given(feedbackRepository.save(any(FeedbackEntity.class))).willReturn(saved);
        given(memberRepository.findById(1L)).willReturn(Optional.of(new MemberEntity("google", "user-1", "tester")));
        given(questionRepository.findById(10L)).willReturn(Optional.of(question));

        Feedback result = feedbackService.create(1L, request);

        ArgumentCaptor<FeedbackEntity> captor = ArgumentCaptor.forClass(FeedbackEntity.class);
        then(feedbackRepository).should().save(captor.capture());
        then(discordNotifier).should().notifyFeedback(saved, "tester", "Question summary");

        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(captor.getValue().getQuestionId()).isEqualTo(10L);
        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getType()).isEqualTo(FeedbackType.QUESTION_ERROR);
    }

    @Test
    @DisplayName("updateStatus sends an in-app notification when resolved")
    void updateStatus_resolved() {
        FeedbackEntity entity = new FeedbackEntity(
                FeedbackType.BUG, 1L, null, "This issue has already been fixed on the latest page build.", "/feedback");
        setId(entity, 9L);

        given(feedbackRepository.findById(9L)).willReturn(Optional.of(entity));

        Feedback result = feedbackService.updateStatus(9L, FeedbackStatus.RESOLVED);

        then(notificationService).should().notify(
                eq(1L),
                eq("FEEDBACK_RESOLVED"),
                any(String.class),
                any(String.class),
                eq("/mypage/feedback"),
                eq(9L));
        assertThat(result.getStatus()).isEqualTo(FeedbackStatus.RESOLVED);
    }

    @Test
    @DisplayName("updateStatus throws when feedback is missing")
    void updateStatus_notFound() {
        given(feedbackRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.updateStatus(999L, FeedbackStatus.REVIEWED))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FEEDBACK_NOT_FOUND);
    }

    @Test
    @DisplayName("getAll uses the status filter when provided")
    void getAll_withStatus() {
        FeedbackEntity entity = new FeedbackEntity(FeedbackType.BUG, 1L, null, "content body", "/page");
        given(feedbackRepository.findByStatus(eq(FeedbackStatus.NEW), any()))
                .willReturn(new PageImpl<>(java.util.List.of(entity)));

        var page = feedbackService.getAll(FeedbackStatus.NEW, 0, 10);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getType()).isEqualTo(FeedbackType.BUG);
    }

    @Test
    @DisplayName("resolveNickname falls back when member is missing")
    void resolveNickname_missingMember() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThat(feedbackService.resolveNickname(1L)).isEqualTo("탈퇴한 회원");
    }

    private static void setId(FeedbackEntity entity, Long id) {
        try {
            var field = FeedbackEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
