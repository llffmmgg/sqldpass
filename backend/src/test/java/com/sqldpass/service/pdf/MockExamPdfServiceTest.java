package com.sqldpass.service.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sqldpass.config.PdfProperties;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.upload.R2UploadService;

@ExtendWith(MockitoExtension.class)
class MockExamPdfServiceTest {

    @Mock private MockExamRepository mockExamRepository;
    @Mock private PdfRenderService pdfRenderService;
    @Mock private R2UploadService r2UploadService;

    private final PrintTokenService printTokenService =
            new PrintTokenService("test-secret-test-secret-test-secret-test-secret");

    @InjectMocks private MockExamPdfService service;

    private MockExamEntity exam;

    @BeforeEach
    void setup() {
        PdfProperties props = new PdfProperties();
        props.setPrintPageBaseUrl("http://localhost:3000");
        props.setCacheKeyPrefix("pdf/mock-exams");
        ReflectionTestUtils.setField(service, "pdfProperties", props);
        ReflectionTestUtils.setField(service, "printTokenService", printTokenService);

        exam = buildExam(11L, "SQLD 모의고사 1회", List.of(
                buildQuestion(101L, 1, "Q1 본문", 2, "Q1 해설"),
                buildQuestion(102L, 2, "Q2 본문", 4, "Q2 해설")));
    }

    @Test
    void cacheHit_returnsExistingUrlWithoutRendering() {
        when(r2UploadService.isEnabled()).thenReturn(true);
        when(mockExamRepository.findByIdWithQuestions(11L)).thenReturn(Optional.of(exam));
        when(r2UploadService.publicUrlIfExists(anyString()))
                .thenReturn("https://cdn.example/pdf.pdf");

        MockExamPdfService.PdfResult result = service.generate(11L);

        assertThat(result.url()).isEqualTo("https://cdn.example/pdf.pdf");
        assertThat(result.cached()).isTrue();
        verify(pdfRenderService, never()).renderUrlToPdf(anyString());
        verify(r2UploadService, never()).uploadBytes(anyString(), any(), anyString());
    }

    @Test
    void cacheMiss_rendersAndUploads() {
        when(r2UploadService.isEnabled()).thenReturn(true);
        when(mockExamRepository.findByIdWithQuestions(11L)).thenReturn(Optional.of(exam));
        when(r2UploadService.publicUrlIfExists(anyString())).thenReturn(null);
        when(pdfRenderService.renderUrlToPdf(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(r2UploadService.uploadBytes(anyString(), any(), eq("application/pdf")))
                .thenReturn("https://cdn.example/new.pdf");

        MockExamPdfService.PdfResult result = service.generate(11L);

        assertThat(result.url()).isEqualTo("https://cdn.example/new.pdf");
        assertThat(result.cached()).isFalse();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(pdfRenderService, times(1)).renderUrlToPdf(urlCaptor.capture());
        // Playwright URL 에 인쇄 페이지 베이스 + mockExamId + token 쿼리가 포함되어야 함.
        assertThat(urlCaptor.getValue())
                .startsWith("http://localhost:3000/print/mock-exams/11?token=");
    }

    @Test
    void contentHashIsDeterministicForSameContent() {
        when(r2UploadService.isEnabled()).thenReturn(true);
        when(mockExamRepository.findByIdWithQuestions(11L)).thenReturn(Optional.of(exam));
        when(r2UploadService.publicUrlIfExists(anyString())).thenReturn("https://cdn/dup.pdf");

        String hash1 = service.generate(11L).contentHash();
        String hash2 = service.generate(11L).contentHash();

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex
    }

    @Test
    void contentHashChangesWhenQuestionContentChanges() {
        when(r2UploadService.isEnabled()).thenReturn(true);
        when(mockExamRepository.findByIdWithQuestions(11L)).thenReturn(Optional.of(exam));
        when(r2UploadService.publicUrlIfExists(anyString())).thenReturn("https://cdn/x.pdf");
        String originalHash = service.generate(11L).contentHash();

        // 같은 ID 의 모의고사이지만 첫 문제의 본문을 다른 텍스트로 교체
        MockExamEntity mutated = buildExam(11L, "SQLD 모의고사 1회", List.of(
                buildQuestion(101L, 1, "Q1 본문 *수정됨*", 2, "Q1 해설"),
                buildQuestion(102L, 2, "Q2 본문", 4, "Q2 해설")));
        when(mockExamRepository.findByIdWithQuestions(11L)).thenReturn(Optional.of(mutated));

        String mutatedHash = service.generate(11L).contentHash();

        assertThat(mutatedHash).isNotEqualTo(originalHash);
    }

    @Test
    void cacheKeyContainsMockExamIdAndContentHash() {
        when(r2UploadService.isEnabled()).thenReturn(true);
        when(mockExamRepository.findByIdWithQuestions(11L)).thenReturn(Optional.of(exam));
        when(r2UploadService.publicUrlIfExists(anyString())).thenReturn("https://cdn/x.pdf");

        service.generate(11L);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(r2UploadService).publicUrlIfExists(keyCaptor.capture());
        assertThat(keyCaptor.getValue())
                .startsWith("pdf/mock-exams/11/")
                .endsWith(".pdf");
    }

    @Test
    void r2DisabledThrows() {
        when(r2UploadService.isEnabled()).thenReturn(false);

        assertThat(catchThrowable(() -> service.generate(11L)))
                .isInstanceOf(SqldpassException.class);
    }

    private static Throwable catchThrowable(Runnable r) {
        try { r.run(); return null; } catch (Throwable t) { return t; }
    }

    /* ---------------- helpers ---------------- */

    private MockExamEntity buildExam(Long id, String name, List<QuestionEntity> questions) {
        MockExamEntity entity = new MockExamEntity(name, ExamType.SQLD, 1, null);
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "questions", new java.util.ArrayList<>(questions));
        return entity;
    }

    private QuestionEntity buildQuestion(Long id, int order, String content, int correctOption,
                                         String explanation) {
        SubjectEntity subject = new SubjectEntity(null, "SQLD-기본", 0);
        ReflectionTestUtils.setField(subject, "id", 1L);
        QuestionEntity q = new QuestionEntity(subject, content, correctOption, explanation,
                null, "topic", 2);
        ReflectionTestUtils.setField(q, "id", id);
        ReflectionTestUtils.setField(q, "displayOrder", order);
        ReflectionTestUtils.setField(q, "questionType", QuestionType.MCQ);
        return q;
    }
}
