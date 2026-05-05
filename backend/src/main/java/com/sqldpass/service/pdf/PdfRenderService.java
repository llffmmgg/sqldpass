package com.sqldpass.service.pdf;

import java.util.HashMap;
import java.util.Map;

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

    private static final String STARTUP_FAILURE_HINT =
            "Chromium 실행 환경이 준비되지 않았습니다. "
                    + "Playwright browser install 누락 또는 서버/컨테이너의 Chromium 필수 라이브러리 누락 가능성이 큽니다.";

    private Playwright playwright;
    private Browser browser;

    private synchronized void ensureStarted() {
        if (browser != null) return;
        try {
            // NodeJS 드라이버가 firefox/webkit 까지 자동 다운로드하는 동작 차단.
            // Dockerfile ENV 가 이미 설정돼 있어도 이 코드 자체로 한 번 더 보장 (배포 안전망).
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
            playwright = Playwright.create(new Playwright.CreateOptions().setEnv(env));
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            log.info("Playwright Chromium 시작됨 (PDF 렌더 준비)");
        } catch (Exception e) {
            log.error("Playwright Chromium 시작 실패 — 'playwright install chromium' 가 컨테이너/로컬에 적용됐는지 확인 필요", e);
            throw new SqldpassException(ErrorCode.INTERNAL_SERVER_ERROR,
                    STARTUP_FAILURE_HINT + " cause=" + summarizeCause(e), e);
        }
    }

    public void verifyEngine() {
        ensureStarted();
    }

    /**
     * 주어진 URL 을 열고 NetworkIdle 까지 대기 후 A4 PDF 바이너리 생성.
     * 페이지 푸터에 "현재 페이지 / 전체 페이지" 표기.
     *
     * 시그널:
     *  - body[data-print-ready="1"]    : 정상 준비 완료
     *  - body[data-print-ready="error"]: 페이지 안에서 fetch/렌더 실패 (60초 안 기다리고 즉시 실패)
     *  - body[data-print-error]         : "error" 시그널과 함께 박히는 사유 텍스트
     */
    public synchronized byte[] renderUrlToPdf(String url) {
        ensureStarted();
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions());
             Page page = context.newPage()) {

            // 페이지 콘솔 에러를 backend 로그로 흘려서 디버깅 가능하게.
            page.onConsoleMessage(msg -> {
                if ("error".equals(msg.type())) {
                    log.warn("[print page console] {}", msg.text());
                }
            });
            page.onPageError(err ->
                    log.warn("[print page error] {}", err));

            page.navigate(url, new Page.NavigateOptions().setTimeout(60_000));
            // markdown/이미지/폰트가 모두 로드되어야 결정론적이라 NETWORKIDLE 까지 기다림.
            page.waitForLoadState(LoadState.NETWORKIDLE);
            // 인쇄 페이지가 본인 준비를 마치면 body 에 data-print-ready 를 박는다 ("1" 또는 "error").
            page.waitForFunction(
                    "document.body && (document.body.getAttribute('data-print-ready') === '1' "
                            + "|| document.body.getAttribute('data-print-ready') === 'error')",
                    null, new Page.WaitForFunctionOptions().setTimeout(60_000));

            // 페이지가 명시적으로 error 시그널을 박았으면 그 사유로 즉시 실패시킨다.
            String readyState = (String) page.evaluate(
                    "document.body.getAttribute('data-print-ready')");
            if ("error".equals(readyState)) {
                String pageError = (String) page.evaluate(
                        "document.body.getAttribute('data-print-error') || ''");
                String msg = "인쇄 페이지가 에러 상태로 보고: "
                        + (pageError == null || pageError.isBlank() ? "(메시지 없음)" : pageError);
                log.error("PDF 렌더 실패 (페이지 자체 에러): url={}, pageError={}", url, pageError);
                throw new SqldpassException(ErrorCode.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다. " + msg);
            }

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
            // 진짜 원인이 응답에도 노출되도록 cause 요약을 메시지에 포함.
            // (스택트레이스는 보안상 노출하지 않고 root cause type + message 만)
            throw new SqldpassException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "PDF 생성에 실패했습니다. 원인: " + summarizeCause(e));
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

    private static String summarizeCause(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String type = root.getClass().getSimpleName();
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return type;
        }
        return type + ": " + message.replaceAll("\\s+", " ").trim();
    }
}
