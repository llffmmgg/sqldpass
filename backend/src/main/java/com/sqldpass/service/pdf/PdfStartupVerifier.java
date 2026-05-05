package com.sqldpass.service.pdf;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 기동 시점에 Playwright Chromium 을 실제로 띄워 본다.
 * 배포 후 첫 PDF 요청에서 터지지 않고 startup 단계에서 환경 문제를 드러내기 위한 fail-fast 검증.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sqldpass.pdf", name = "verify-on-startup", havingValue = "true")
public class PdfStartupVerifier implements ApplicationRunner {

    private final PdfRenderService pdfRenderService;

    @Override
    public void run(ApplicationArguments args) {
        pdfRenderService.verifyEngine();
        log.info("PDF 렌더 엔진 startup 검증 완료");
    }
}
