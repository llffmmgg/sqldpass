package com.sqldpass.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.service.generation.AiProvider;
import com.sqldpass.service.generation.dto.AiVerificationRequest;
import com.sqldpass.service.generation.dto.AiVerificationResponse;

@ExtendWith(MockitoExtension.class)
class AdminQuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

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
                "설명형 문제",
                QuestionType.DESCRIPTIVE,
                "모범 답안",
                "[\"alpha\",\"beta\"]",
                "해설",
                "요약",
                "토픽",
                3);

        when(questionRepository.findAllWithSubject(PageRequest.of(0, 100))).thenReturn(new PageImpl<>(List.of(question)));
        when(verifier.verifyQuestion(any())).thenReturn(new AiVerificationResponse(true, "ok"));

        adminQuestionService.verifyAll(null, 100);

        ArgumentCaptor<AiVerificationRequest> captor = ArgumentCaptor.forClass(AiVerificationRequest.class);
        verify(verifier).verifyQuestion(captor.capture());

        AiVerificationRequest request = captor.getValue();
        assertThat(request.examType()).isEqualTo(ExamType.ENGINEER_PRACTICAL);
        assertThat(request.question().correctOption()).isNull();
        assertThat(request.question().answerText()).isEqualTo("모범 답안");
        assertThat(request.question().keywords()).containsExactly("alpha", "beta");
    }
}
