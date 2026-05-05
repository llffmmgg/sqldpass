import { NextResponse, type NextRequest } from "next/server";

// Markdown for Agents — Cloudflare 가이드 기반.
// 브라우저는 기본 HTML, AI 에이전트는 Accept: text/markdown 으로 동일 URL에서 markdown 응답을 받는다.
// 현재는 홈("/")에 한해 협상하며, 응답은 사이트 요약(/llms.txt)으로 rewrite한다.
export function proxy(request: NextRequest): NextResponse {
  const accept = request.headers.get("accept") ?? "";
  const wantsMarkdown = accept.includes("text/markdown");
  if (wantsMarkdown && request.nextUrl.pathname === "/") {
    const url = request.nextUrl.clone();
    url.pathname = "/llms.txt";
    const res = NextResponse.rewrite(url);
    // 캐시·CDN이 Accept를 변형값으로 인식하도록 명시.
    res.headers.set("Vary", "Accept");
    return res;
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/"],
};
