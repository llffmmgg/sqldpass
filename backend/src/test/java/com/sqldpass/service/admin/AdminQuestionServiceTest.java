package com.sqldpass.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.sqldpass.controller.admin.dto.QuestionVerifyRunResponse;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.question.QuestionVerificationRunEntity;
import com.sqldpass.persistent.question.QuestionVerificationRunRepository;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;
import com.sqldpass.service.generation.AiProvider;
import com.sqldpass.service.generation.dto.AiVerificationResponse;

@ExtendWith(MockitoExtension.class)
class AdminQuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private QuestionVerificationRunRepository questionVerificationRunRepository;

    @Mock
    private AiProvider verifier;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private AdminQuestionService adminQuestionService;

    @Test
    void verifyAllBatchApproved() {
        SubjectEntity root = new SubjectEntity(null, "\uC815\uBCF4\uCC98\uB9AC\uAE30\uC0AC \uC2E4\uAE30", 1);
        SubjectEntity child = new SubjectEntity(root, "\uC571 \uD14C\uC2A4\uD2B8", 1);
        QuestionEntity question = new QuestionEntity(
                child,
                "descriptive question",
                QuestionType.DESCRIPTIVE,
                "model answer",
                "[\"alpha\",\"beta\"]",
                "explanation",
                "summary",
                "topic",
                3);
        ReflectionTestUtils.setField(question, "id", 7L);

        when(questionRepository.findTriageIdsForVerification(
                eq("\uC815\uBCF4\uCC98\uB9AC\uAE30\uC0AC \uC2E4\uAE30"),
                eq(null),
                eq(null),
                eq(true),
                eq(100)))
                .thenReturn(List.of(7L));
        when(questionRepository.findByIdInWithSubjectAndParent(List.of(7L)))
                .thenReturn(List.of(question));
        when(questionRepository.markVerifiedInBatch(any(), any())).thenReturn(1);
        when(questionVerificationRunRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionVerificationRunRepository.findRecentRuns(PageRequest.of(0, 5))).thenReturn(List.of());
        when(verifier.verifyQuestionsBatch(anyList()))
                .thenReturn(List.of(AiVerificationResponse.ofApproved()));

        QuestionVerifyRunResponse response = adminQuestionService.verifyAll(
                ExamType.ENGINEER_PRACTICAL, null, 100, false);

        assertThat(response.processedCount()).isEqualTo(1);
        assertThat(response.suspiciousCount()).isZero();
        assertThat(response.fixedCount()).isZero();
        assertThat(response.unfixableCount()).isZero();
        assertThat(response.errorCount()).isZero();
    }

    @Test
    void verifyAllBatchRejectedWithAutoFix() {
        SubjectEntity root = new SubjectEntity(null, "SQL 기본", 1);
        SubjectEntity child = new SubjectEntity(root, "JOIN", 1);
        QuestionEntity question = new QuestionEntity(child, "mcq question", 2, "explanation", "summary", "topic", 2);
        ReflectionTestUtils.setField(question, "id", 11L);

        when(subjectRepository.findById(10L)).thenReturn(Optional.of(child));
        when(questionRepository.findTriageIdsForVerification(
                any(), any(), eq(10L), eq(true), eq(20)))
                .thenReturn(List.of(11L));
        when(questionRepository.findByIdInWithSubjectAndParent(List.of(11L)))
                .thenReturn(List.of(question));
        when(questionVerificationRunRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionVerificationRunRepository.findRecentRuns(PageRequest.of(0, 5))).thenReturn(List.of());

        // 거절 + fixable
        when(verifier.verifyQuestionsBatch(anyList()))
                .thenReturn(List.of(AiVerificationResponse.ofRejected("정답이 2가 아니라 3", true)));
        // 자동 fix 성공
        when(verifier.fixQuestion(any(), any(), any(), any()))
                .thenReturn(new com.sqldpass.service.generation.dto.GeneratedQuestion(
                        "fixed content", 3, "fixed explanation", "summary", "topic", 2));

        QuestionVerifyRunResponse response = adminQuestionService.verifyAll(
                ExamType.SQLD, 10L, 20, false);

        ArgumentCaptor<QuestionVerificationRunEntity> runCaptor =
                ArgumentCaptor.forClass(QuestionVerificationRunEntity.class);
        verify(questionVerificationRunRepository).save(runCaptor.capture());

        assertThat(runCaptor.getValue().getSuspiciousCount()).isEqualTo(1);
        assertThat(runCaptor.getValue().getFixedCount()).isEqualTo(1);
        assertThat(runCaptor.getValue().getUnfixableCount()).isZero();
        assertThat(response.suspiciousQuestions()).hasSize(1);
        assertThat(response.suspiciousQuestions().get(0).reason()).contains("[자동수정]");
    }

    @Test
    void verifyAllBatchUnknownStaysUnverified() {
        SubjectEntity root = new SubjectEntity(null, "SQL 기본", 1);
        SubjectEntity child = new SubjectEntity(root, "JOIN", 1);
        QuestionEntity question = new QuestionEntity(child, "mcq question", 2, "explanation", "summary", "topic", 2);
        ReflectionTestUtils.setField(question, "id", 22L);

        when(questionRepository.findTriageIdsForVerification(any(), any(), any(), eq(true), eq(50)))
                .thenReturn(List.of(22L));
        when(questionRepository.findByIdInWithSubjectAndParent(List.of(22L)))
                .thenReturn(List.of(question));
        when(questionVerificationRunRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionVerificationRunRepository.findRecentRuns(PageRequest.of(0, 5))).thenReturn(List.of());

        when(verifier.verifyQuestionsBatch(anyList()))
                .thenReturn(List.of(AiVerificationResponse.ofUnknown("LLM 빈 응답")));

        QuestionVerifyRunResponse response = adminQuestionService.verifyAll(null, null, 50, false);

        assertThat(response.errorCount()).isEqualTo(1);
        assertThat(response.suspiciousCount()).isZero();
        // verifiedAt should NOT be marked
        verify(questionRepository, org.mockito.Mockito.never()).markVerifiedInBatch(anyList(), any());
    }
}
