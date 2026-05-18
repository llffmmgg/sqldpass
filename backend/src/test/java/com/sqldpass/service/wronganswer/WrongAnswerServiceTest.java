package com.sqldpass.service.wronganswer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.controller.solve.dto.SolveRequest;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerPreviewResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerRetryResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerStatsResponse;
import com.sqldpass.domain.solve.Solve;
import com.sqldpass.persistent.member.MemberEntity.StreakUpdateResult;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.solve.SolveAnswerRepository;
import com.sqldpass.persistent.solve.WrongAnswerProjection;
import com.sqldpass.persistent.solve.WrongAnswerStatsProjection;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.payment.SubscriptionService;
import com.sqldpass.service.solve.SolveService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class WrongAnswerServiceTest {

    @Mock
    private SolveAnswerRepository solveAnswerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SolveService solveService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private WrongAnswerService wrongAnswerService;

    @Test
    @DisplayName("getWrongAnswers maps repository projections")
    void getWrongAnswers() {
        given(subscriptionService.hasLibraryAccess(1L)).willReturn(true);
        WrongAnswerProjection projection = mock(WrongAnswerProjection.class);
        given(projection.getQuestionId()).willReturn(10L);
        given(projection.getQuestionContent()).willReturn("Question content");
        given(projection.getQuestionType()).willReturn("MCQ");
        given(projection.getSubjectName()).willReturn("SQL Basics");
        given(projection.getWrongCount()).willReturn(3);
        given(projection.getLastWrongAt()).willReturn(LocalDateTime.of(2026, 4, 2, 11, 0));
        given(solveAnswerRepository.findWrongAnswers(1L, 7L)).willReturn(List.of(projection));

        List<WrongAnswerResponse> responses = wrongAnswerService.getWrongAnswers(1L, 7L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).questionId()).isEqualTo(10L);
        assertThat(responses.get(0).questionContent()).isEqualTo("Question content");
        assertThat(responses.get(0).questionType()).isEqualTo("MCQ");
        assertThat(responses.get(0).wrongCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("getWrongAnswers throws when member has no library access")
    void getWrongAnswers_forbidden() {
        given(subscriptionService.hasLibraryAccess(1L)).willReturn(false);

        assertThatThrownBy(() -> wrongAnswerService.getWrongAnswers(1L, null))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.WRONG_ANSWER_REQUIRES_SUBSCRIPTION);
    }

    @Test
    @DisplayName("getStats maps repository projections (권한 가드 없음 — 대시보드 통계 노출용)")
    void getStats() {
        WrongAnswerStatsProjection projection = mock(WrongAnswerStatsProjection.class);
        given(projection.getSubjectId()).willReturn(5L);
        given(projection.getSubjectName()).willReturn("SQL Basics");
        given(projection.getTotalSolved()).willReturn(20);
        given(projection.getWrongCount()).willReturn(8);
        given(projection.getWrongRate()).willReturn(40);
        given(solveAnswerRepository.findWrongAnswerStats(1L)).willReturn(List.of(projection));

        List<WrongAnswerStatsResponse> responses = wrongAnswerService.getStats(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).subjectId()).isEqualTo(5L);
        assertThat(responses.get(0).wrongRate()).isEqualTo(40);
    }

    @Test
    @DisplayName("getPreview returns top N items without subscription check")
    void getPreview_noAccessCheck() {
        WrongAnswerProjection projection = mock(WrongAnswerProjection.class);
        given(projection.getQuestionId()).willReturn(10L);
        given(projection.getQuestionContent()).willReturn("Preview content");
        given(projection.getQuestionType()).willReturn("SHORT_ANSWER");
        given(projection.getSubjectName()).willReturn("SQL Basics");
        given(solveAnswerRepository.findWrongAnswers(1L, null)).willReturn(List.of(projection));

        List<WrongAnswerPreviewResponse> preview = wrongAnswerService.getPreview(1L, 5);

        assertThat(preview).hasSize(1);
        assertThat(preview.get(0).questionId()).isEqualTo(10L);
        assertThat(preview.get(0).questionContent()).isEqualTo("Preview content");
        assertThat(preview.get(0).questionType()).isEqualTo("SHORT_ANSWER");
    }

    @Test
    @DisplayName("getPreview returns empty list for anonymous user")
    void getPreview_anonymous() {
        List<WrongAnswerPreviewResponse> preview = wrongAnswerService.getPreview(null, 5);
        assertThat(preview).isEmpty();
    }

    @Test
    @DisplayName("retry delegates to SolveService and returns answer details")
    void retry() {
        SubjectEntity subject = mock(SubjectEntity.class);
        QuestionEntity question = mock(QuestionEntity.class);
        Solve solve = new Solve(1L, 1L, 99L, null, 1, 1, 100, LocalDateTime.now(), List.of());
        SolveService.SolveWithStreak solveWithStreak = new SolveService.SolveWithStreak(
                solve, new StreakUpdateResult(0, false, null));

        given(subject.getId()).willReturn(99L);
        given(question.getSubject()).willReturn(subject);
        given(question.getCorrectOption()).willReturn(2);
        given(question.getAnswer()).willReturn(null);
        given(question.getExplanation()).willReturn("Explanation");
        given(questionRepository.findById(10L)).willReturn(Optional.of(question));
        given(solveService.solve(eq(1L), any(SolveRequest.class))).willReturn(solveWithStreak);

        WrongAnswerRetryResponse response = wrongAnswerService.retry(1L, 10L, 2, null);

        ArgumentCaptor<SolveRequest> captor = ArgumentCaptor.forClass(SolveRequest.class);
        then(solveService).should().solve(eq(1L), captor.capture());

        assertThat(captor.getValue().subjectId()).isEqualTo(subject.getId());
        assertThat(captor.getValue().answers()).hasSize(1);
        assertThat(captor.getValue().answers().get(0).questionId()).isEqualTo(10L);
        assertThat(captor.getValue().answers().get(0).selectedOption()).isEqualTo(2);
        assertThat(response.correct()).isTrue();
        assertThat(response.correctOption()).isEqualTo(2);
        assertThat(response.explanation()).isEqualTo("Explanation");
    }

    @Test
    @DisplayName("retry throws when the question does not exist")
    void retry_questionNotFound() {
        given(questionRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wrongAnswerService.retry(1L, 10L, 2, null))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.QUESTION_NOT_FOUND);
    }
}
