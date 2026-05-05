/**
 * 인쇄 전용 레이아웃 — Playwright 가 PDF 로 굽는 페이지에 사용.
 *
 * 루트 레이아웃의 NavBar/Footer/BottomTabBar/AdSidebar/FeedbackRail/SiteNoticeBanner
 * 등 사이트 chrome 을 모두 가리고, 본문 영역(div.flex-1) 만 노출한다.
 *
 * @page CSS 와 페이지 분할 규칙은 각 인쇄 페이지에서 정의한다.
 */
export default function PrintLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <style
        dangerouslySetInnerHTML={{
          __html: `
            /* 사이트 chrome 전부 숨김. 본문 wrapper(.flex-1) 만 살린다. */
            body > *:not(.flex-1) { display: none !important; }
            /* 본문 wrapper 자체의 flex layout 도 무력화 — 인쇄용으로 흐름 단순화 */
            body { background: #ffffff !important; color: #111111 !important; padding: 0 !important; margin: 0 !important; }
            body > .flex-1 { background: #ffffff !important; min-height: 0 !important; }
            html { background: #ffffff !important; }
            html.dark { color-scheme: light !important; }
          `,
        }}
      />
      {children}
    </>
  );
}
