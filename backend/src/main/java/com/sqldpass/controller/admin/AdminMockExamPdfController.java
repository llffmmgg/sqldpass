package com.sqldpass.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.service.pdf.MockExamPdfBackfillService;
import com.sqldpass.service.pdf.MockExamPdfService;
import com.sqldpass.service.pdf.MockExamPdfService.PdfResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 어드민 — 모의고사 PDF 생성 트리거 + 백필.
 * /api/admin/mock-exams 와 prefix 가 다른 별도 컨트롤러로 분리해서 PDF 의존성(Playwright/R2)
 * 변경이 기존 모의고사 CRUD 와 결합되지 않게 했다.
 */
@Tag(name = "관리자 - 모의고사 PDF", description = "Playwright 기반 결정론적 PDF 생성")
@RestController
@RequestMapping("/api/admin/mock-exams")
@RequiredArgsConstructor
public class AdminMockExamPdfController {

    private final MockExamPdfService mockExamPdfService;
    private final MockExamPdfBackfillService backfillService;

    public record PdfResponse(String url, String contentHash, boolean cached) {
        public static PdfResponse from(PdfResult r) {
            return new PdfResponse(r.url(), r.contentHash(), r.cached());
        }
    }

    @PostMapping("/{id}/pdf")
    @Operation(
            summary = "모의고사 PDF 생성/조회",
            description = "동일 콘텐츠는 R2 에 영구 캐시되어 재생성하지 않는다. "
                    + "응답의 url 로 다운로드. cached=true 면 캐시 hit, false 면 새로 렌더된 결과."
    )
    public PdfResponse generate(@PathVariable Long id) {
        return PdfResponse.from(mockExamPdfService.generate(id));
    }

    @PostMapping("/pdf/backfill")
    @Operation(
            summary = "PDF 일괄 백필 시작 (사용자 노출 모의고사 전부)",
            description = "expert_verified=true && visibility=PUBLISHED 인 모의고사들을 직렬로 처리. "
                    + "이미 캐시된 것은 즉시 통과. 백그라운드 실행 → 즉시 반환. "
                    + "진행 상황은 GET /pdf/backfill/status 로 폴링."
    )
    public MockExamPdfBackfillService.Status startBackfill() {
        return backfillService.start();
    }

    @GetMapping("/pdf/backfill/status")
    @Operation(summary = "PDF 백필 진행 상황 조회")
    public MockExamPdfBackfillService.Status backfillStatus() {
        return backfillService.status();
    }
}
