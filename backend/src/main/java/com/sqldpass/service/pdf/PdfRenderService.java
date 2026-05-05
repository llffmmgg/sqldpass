package com.sqldpass.service.pdf;

import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Margin;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Playwright 헤드리스 Chromium 으로 임의 URL 을 렌더해 PDF 바이너리를 만든다.
 *
 * 결정론 보장:
 *  - Chromium 버전이 Playwright 버전에 고정 → 동일 입력 → 동일 출력
 *  - 페이지 자체에 시각/난수 의존 요소가 없어야 함 (인쇄 페이지에서 통제)
 *
 * 동시성:
 *  - Playwright Browser/Context 는 thread-safe 하지 않음.
 *  - 어드민 본인만 가끔 호출하는 워크로드라 synchronized 로 직렬화.
 */
@Slf4j
@Service
public class PdfRenderService {

    private Playwright playwright;
    private Browser browser;

    private synchronized void ensureStarted() {
        if (browser != null) return;
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            log.info("Playwright Chromium 시작됨 (PDF 렌더 준비)");
        } catch (Exception e) {
            log.error("Playwright Chromium 시작 실패 — 'playwright install chromium' 가 컨테이너/로컬에 적용됐는지 확인 필요", e);
            throw new SqldpassException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "PDF 렌더 엔진(Chromium) 을 시작할 수 없습니다.");
        }
    }

    /**
     * 주어진 URL 을 열고 NetworkIdle 까지 대기 후 A4 PDF 바이너리 생성.
     * 페이지 푸터에 "현재 페이지 / 전체 페이지" 표기.
     */
    public synchronized byte[] renderUrlToPdf(String url) {
        ensureStarted();
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions());
             Page page = context.newPage()) {

            page.navigate(url, new Page.NavigateOptions().setTimeout(60_000));
            // markdown/이미지/폰트가 모두 로드되어야 결정론적이라 NETWORKIDLE 까지 기다림.
            page.waitForLoadState(LoadState.NETWORKIDLE);
            // 인쇄 페이지가 본인 준비를 마치면 body 에 data-print-ready="1" 을 박는다.
            page.waitForFunction("document.body && document.body.getAttribute('data-print-ready') === '1'",
                    null, new Page.WaitForFunctionOptions().setTimeout(60_000));

            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("16mm").setBottom("16mm").setLeft("14mm").setRight("14mm"))
                    .setDisplayHeaderFooter(true)
                    .setHeaderTemplate("<div></div>")
                    .setFooterTemplate(
                            "<div style='font-size:9px;width:100%;text-align:center;color:#888;padding:0 14mm;'>"
                                    + "<span class='pageNumber'></span> / <span class='totalPages'></span>"
                                    + "</div>"));
        } catch (SqldpassException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF 렌더 실패: url={}", url, e);
            throw new SqldpassException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "PDF 생성에 실패했습니다.");
        }
    }

    @PreDestroy
    void shutdown() {
        try {
            if (browser != null) browser.close();
        } catch (Exception ignored) {}
        try {
            if (playwright != null) playwright.close();
        } catch (Exception ignored) {}
    }
}
