package com.sqldpass.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.sqldpass.service.generation.dto.AiVerificationRequest;
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

    @InjectMocks
    private AdminQuestionService adminQuestionService;

    @Test
    void verifyAllResolvesEngineerExamTypeAndParsesKeywords() {
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

        when(questionRepository.findByRootNameForVerification(
                eq("\uC815\uBCF4\uCC98\uB9AC\uAE30\uC0AC \uC2E4\uAE30"),
                eq(null),
                eq(true),
                eq(PageRequest.of(0, 100))))
                .thenReturn(List.of(question));
        when(questionVerificationRunRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionVerificationRunRepository.findRecentRuns(PageRequest.of(0, 5))).thenReturn(List.of());
        when(verifier.verifyQuestion(any())).thenReturn(new AiVerificationResponse(true, "ok"));

        QuestionVerifyRunResponse response = adminQuestionService.verifyAll(ExamType.ENGINEER_PRACTICAL, null, 100, false);

        ArgumentCaptor<AiVerificationRequest> captor = ArgumentCaptor.forClass(AiVerificationRequest.class);
        verify(verifier).verifyQuestion(captor.capture());

        AiVerificationRequest request = captor.getValue();
        assertThat(request.examType()).isEqualTo(ExamType.ENGINEER_PRACTICAL);
        assertThat(request.question().correctOption()).isNull();
        assertThat(request.question().answerText()).isEqualTo("model answer");
        assertThat(request.question().keywords()).containsExactly("alpha", "beta");
        assertThat(response.processedCount()).isEqualTo(1);
        assertThat(response.suspiciousCount()).isZero();
        assertThat(question.getVerifiedAt()).isNotNull();
    }

    @Test
    void verifyAllStoresRunWithSubjectWhenFiltered() {
        SubjectEntity root = new SubjectEntity(null, "SQL 기본", 1);
        SubjectEntity child = new SubjectEntity(root, "JOIN", 1);
        QuestionEntity question = new QuestionEntity(child, "mcq question", 2, "explanation", "summary", "topic", 2);

        when(subjectRepository.findById(10L)).thenReturn(Optional.of(child));
        when(questionRepository.findSqldForVerification(any(), eq(10L), eq(true), eq(PageRequest.of(0, 20))))
                .thenReturn(List.of(question));
        when(questionVerificationRunRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionVerificationRunRepository.findRecentRuns(PageRequest.of(0, 5))).thenReturn(List.of());
        when(verifier.verifyQuestion(any())).thenReturn(new AiVerificationResponse(false, "의심"));

        QuestionVerifyRunResponse response = adminQuestionService.verifyAll(ExamType.SQLD, 10L, 20, false);

        ArgumentCaptor<QuestionVerificationRunEntity> runCaptor =
                ArgumentCaptor.forClass(QuestionVerificationRunEntity.class);
        verify(questionVerificationRunRepository).save(runCaptor.capture());

        assertThat(runCaptor.getValue().getSubject()).isEqualTo(child);
        assertThat(runCaptor.getValue().getSuspiciousCount()).isEqualTo(1);
        assertThat(response.subjectName()).isEqualTo("JOIN");
        assertThat(response.suspiciousQuestions()).hasSize(1);
    }
}
