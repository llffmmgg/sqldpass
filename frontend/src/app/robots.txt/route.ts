// robots.txt route handler.
// Next.js metadata API(robots.ts)는 Content-Signal 같은 커스텀 디렉티브를 지원하지 않아
// route handler로 직접 응답한다.
//
// 포함:
// - 표준 User-agent / Allow / Disallow 규칙 (기존 robots.ts와 동일)
// - Content-Signal 디렉티브 (contentsignals.org 초안) — AI 학습/검색/입력 용도 선언
// - Sitemap, Host

const SITE_URL = "https://www.sqldpass.com";

const DISALLOW_PATHS = [
  "/admin",
  "/admin/",
  "/auth/",
  "/api/",
  "/dashboard",
  "/dashboard/",
  "/wrong-answers",
  "/wrong-answers/",
  "/profile",
  "/history",
  "/history/",
  "/mypage/",
];

const AI_AGENTS = ["OAI-SearchBot", "ChatGPT-User"];

function buildRules(userAgent: string): string {
  const lines: string[] = [];
  lines.push(`User-agent: ${userAgent}`);
  lines.push("Allow: /");
  for (const path of DISALLOW_PATHS) {
    lines.push(`Disallow: ${path}`);
  }
  return lines.join("\n");
}

export function GET(): Response {
  const sections: string[] = [];

  // Content-Signal: 사이트 전체 정책.
  // - search=yes: 일반 검색 인덱싱 허용
  // - ai-input=yes: AI 답변 생성 시 입력으로 사용 허용 (RAG 등)
  // - ai-train=no: 모델 학습용 수집은 거부
  sections.push("# Content Signals — https://contentsignals.org/");
  sections.push("Content-Signal: search=yes, ai-input=yes, ai-train=no");
  sections.push("");

  for (const agent of AI_AGENTS) {
    sections.push(buildRules(agent));
    sections.push("");
  }
  sections.push(buildRules("*"));
  sections.push("");
  sections.push(`Sitemap: ${SITE_URL}/sitemap.xml`);
  sections.push(`Host: ${SITE_URL}`);

  return new Response(sections.join("\n") + "\n", {
    status: 200,
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "public, max-age=3600, s-maxage=3600",
    },
  });
}
