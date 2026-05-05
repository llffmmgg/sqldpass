package com.sqldpass.service.pdf;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 모의고사가 사용자 노출 상태로 전이되면(MockExamPublishedEvent) 백그라운드에서
 * PDF 를 미리 만들어 R2 캐시에 저장한다 (사용자 다운로드 시 항상 캐시 hit 보장).
 *
 * - 트랜잭션 커밋 이후에만 실행 (TransactionPhase.AFTER_COMMIT)
 * - 단일 스레드 풀 → PdfRenderService 의 synchronized 와 직렬화 정합
 * - 실패해도 로그만 남기고 본 흐름 막지 않음 (fire-and-forget)
 * - 동일 콘텐츠 캐시 hit 시 Playwright 안 돌고 즉시 끝 (R2 HEAD 1회)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockExamPdfPrewarmer {

    private final MockExamPdfService mockExamPdfService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mock-exam-pdf-prewarm");
        t.setDaemon(true);
        return t;
    });

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublished(MockExamPublishedEvent event) {
        Long id = event.mockExamId();
        executor.submit(() -> {
            try {
                MockExamPdfService.PdfResult r = mockExamPdfService.generate(id);
                log.info("PDF prewarm 완료: mockExamId={}, cached={}", id, r.cached());
            } catch (Exception e) {
                log.warn("PDF prewarm 실패 (사용자 다운로드 시 lazy 생성됨): mockExamId={}", id, e);
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
