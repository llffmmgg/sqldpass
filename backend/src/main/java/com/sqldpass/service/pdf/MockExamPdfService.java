package com.sqldpass.service.pdf;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.config.PdfProperties;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamRepository;
import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.upload.R2UploadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 모의고사 1세트 → PDF 변환 + R2 영구 캐시.
 *
 * 흐름:
 *   1. mockExamId → MockExamEntity (questions 포함) 로드
 *   2. 콘텐츠 SHA-256 해시 계산 → R2 캐시 키 결정
 *   3. R2 에 같은 키 객체 존재 → publicUrl 즉시 반환 (캐시 hit)
 *   4. 없으면: 단기 토큰 발급 → 인쇄 페이지 URL 빌드 → Playwright 렌더 → R2 업로드 → publicUrl 반환
 *
 * 결정론:
 *   - 같은 콘텐츠 → 같은 해시 → 같은 R2 객체 → 모든 사용자/모든 다운로드 동일.
 *   - 콘텐츠 수정 시 해시가 바뀌어 자동 재생성.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockExamPdfService {

    private final MockExamRepository mockExamRepository;
    private final PdfRenderService pdfRenderService;
    private final R2UploadService r2UploadService;
    private final PrintTokenService printTokenService;
    private final PdfProperties pdfProperties;

    public record PdfResult(String url, String contentHash, boolean cached) {}

    @Transactional(readOnly = true)
    public PdfResult generate(Long mockExamId) {
        if (!r2UploadService.isEnabled()) {
            throw new SqldpassException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "R2 가 설정되지 않아 PDF 캐시를 저장할 수 없습니다.");
        }

        MockExamEntity exam = mockExamRepository.findByIdWithQuestions(mockExamId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MOCK_EXAM_NOT_FOUND));

        String contentHash = computeContentHash(exam);
        String key = pdfProperties.getCacheKeyPrefix() + "/" + mockExamId + "/" + contentHash + ".pdf";

        String existing = r2UploadService.publicUrlIfExists(key);
        if (existing != null) {
            log.info("PDF 캐시 hit: mockExamId={}, key={}", mockExamId, key);
            return new PdfResult(existing, contentHash, true);
        }

        String token = printTokenService.issue(mockExamId);
        String url = pdfProperties.getPrintPageBaseUrl()
                + "/print/mock-exams/" + mockExamId
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("PDF 캐시 miss → Playwright 렌더 시작: mockExamId={}, url={}", mockExamId, url);
        long t0 = System.currentTimeMillis();
        byte[] bytes = pdfRenderService.renderUrlToPdf(url);
        long renderMs = System.currentTimeMillis() - t0;
        log.info("PDF 렌더 완료: mockExamId={}, bytes={}, renderMs={}", mockExamId, bytes.length, renderMs);

        String publicUrl = r2UploadService.uploadBytes(key, bytes, "application/pdf");
        return new PdfResult(publicUrl, contentHash, false);
    }

    /**
     * 콘텐츠 해시 — 모의고사 메타 + 문제별 본문/정답/해설/답변/키워드 를 모두 포함.
     * 동일 입력 → 동일 해시 → R2 키 1대1.
     */
    private String computeContentHash(MockExamEntity exam) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 버전 prefix — PDF 레이아웃이 바뀌면 v 를 올려서 강제 재생성.
            update(md, "v1");
            update(md, String.valueOf(exam.getId()));
            update(md, nullToEmpty(exam.getName()));
            update(md, exam.getExamType() != null ? exam.getExamType().name() : "");
            update(md, String.valueOf(exam.getSequence()));
            update(md, exam.getKind() != null ? exam.getKind().name() : "");
            update(md, exam.getExamYear() != null ? String.valueOf(exam.getExamYear()) : "");
            update(md, exam.getExamRound() != null ? String.valueOf(exam.getExamRound()) : "");
            update(md, exam.getExamDate() != null ? exam.getExamDate().toString() : "");
            update(md, String.valueOf(exam.isExpertVerified()));

            // displayOrder 오름차순으로 정렬해 결정론 보장.
            List<QuestionEntity> sorted = exam.getQuestions().stream()
                    .sorted((a, b) -> {
                        Integer ao = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
                        Integer bo = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
                        return ao.compareTo(bo);
                    })
                    .toList();
            for (QuestionEntity q : sorted) {
                update(md, "Q");
                update(md, String.valueOf(q.getId()));
                update(md, q.getDisplayOrder() != null ? String.valueOf(q.getDisplayOrder()) : "");
                update(md, q.getQuestionType() != null ? q.getQuestionType().name() : "");
                update(md, nullToEmpty(q.getContent()));
                update(md, q.getCorrectOption() != null ? String.valueOf(q.getCorrectOption()) : "");
                update(md, nullToEmpty(q.getAnswer()));
                update(md, nullToEmpty(q.getKeywords()));
                update(md, nullToEmpty(q.getExplanation()));
                update(md, nullToEmpty(q.getSummary()));
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }

    private static void update(MessageDigest md, String s) {
        md.update((byte) 0x1F);  // unit separator — 필드 경계 모호성 제거
        md.update(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
