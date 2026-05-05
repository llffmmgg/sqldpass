// RFC 9727 API Catalog (linkset+json).
// 공개 콘텐츠 API(/api/public/**)만 카탈로그에 노출 — 관리자/회원 API는 인증 필요라 디스커버리 대상 아님.
//
// service-desc: OpenAPI 스펙 (springdoc 기본 경로 /v3/api-docs)
// service-doc:  Swagger UI
// status:       Spring Actuator health endpoint

const SITE_URL = "https://www.sqldpass.com";

export function GET(): Response {
  const linkset = {
    linkset: [
      {
        anchor: `${SITE_URL}/api/public`,
        "service-desc": [
          {
            href: `${SITE_URL}/v3/api-docs`,
            type: "application/json",
            title: "sqldpass 공개 콘텐츠 API — OpenAPI 3 spec",
          },
        ],
        "service-doc": [
          {
            href: `${SITE_URL}/swagger-ui.html`,
            type: "text/html",
            title: "sqldpass 공개 콘텐츠 API — Swagger UI",
          },
        ],
        status: [
          {
            href: `${SITE_URL}/actuator/health`,
            type: "application/vnd.spring-boot.actuator.v3+json",
            title: "sqldpass backend health check",
          },
          {
            href: "https://github.com/llffmmgg/sqldpass/issues",
            type: "text/html",
            title: "이슈 트래커",
          },
        ],
        author: [
          {
            href: `${SITE_URL}/about`,
            type: "text/html",
            title: "문어CBT 소개",
          },
        ],
        "terms-of-service": [
          {
            href: `${SITE_URL}/terms`,
            type: "text/html",
          },
        ],
        license: [
          {
            href: `${SITE_URL}/terms`,
            type: "text/html",
            title: "이용 약관",
          },
        ],
      },
    ],
  };

  return new Response(JSON.stringify(linkset, null, 2), {
    status: 200,
    headers: {
      "Content-Type": "application/linkset+json",
      "Cache-Control": "public, max-age=3600, s-maxage=3600",
    },
  });
}
