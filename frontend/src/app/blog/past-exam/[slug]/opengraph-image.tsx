import { ImageResponse } from "next/og";

import {
  CERT_TOKENS,
  certFromExamType,
  slugFromCert,
  type CertKey,
} from "@/lib/cert-tokens";
import { getPublicPastExamsByCert } from "@/lib/publicApi";
import {
  certFromBlogPastExamSlug,
  findPastExamBySlug,
} from "@/lib/pastExamBlog";

// Next.js OG image conventions
export const runtime = "nodejs";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

const CERT_SLUGS = ["sqld", "engineer", "engineer-written", "computer-literacy-1", "computer-literacy-2", "adsp"];

const CERT_HEX: Record<CertKey, string> = {
  SQLD: "#f59e0b",
  ENGINEER_PRACTICAL: "#2dbb7a",
  ENGINEER_WRITTEN: "#f43f5e",
  COMPUTER_LITERACY_1: "#0ea5e9",
  COMPUTER_LITERACY_2: "#6366f1",
  ADSP: "#14b8a6",
};

export default async function PastExamOgImage({ params }: { params: { slug: string } }) {
  const { slug } = params;

  // 회차 정보 조회 (sitemap·메타에서 쓰는 헬퍼 재사용)
  const certKey = certFromBlogPastExamSlug(slug);
  const certSlugs = certKey ? [slugFromCert(certKey)] : CERT_SLUGS;
  let exam = null as Awaited<ReturnType<typeof getPublicPastExamsByCert>>[number] | null;
  for (const cs of certSlugs) {
    const list = await getPublicPastExamsByCert(cs).catch(() => []);
    const found = findPastExamBySlug(list, slug);
    if (found) {
      exam = found;
      break;
    }
  }

  const cert: CertKey = certFromExamType(exam?.examType) ?? certKey ?? "SQLD";
  const token = CERT_TOKENS[cert];
  const accent = CERT_HEX[cert];

  const yearText = exam?.examYear ? `${exam.examYear}년` : "";
  const roundText = exam?.examRound ? `${exam.examRound}회` : "";
  const subtitle = [yearText, roundText].filter(Boolean).join(" ") || (exam?.name ?? "");

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          background: "#0f0f10",
          color: "#ededed",
          display: "flex",
          flexDirection: "column",
          padding: "72px 88px",
          fontFamily: "Pretendard, system-ui, sans-serif",
          position: "relative",
        }}
      >
        {/* 좌측 cert 컬러 strip */}
        <div
          style={{
            position: "absolute",
            left: 0,
            top: 0,
            bottom: 0,
            width: 12,
            background: accent,
          }}
        />

        {/* 상단 — cert 라벨 chip + 사이트 이름 */}
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 14,
              padding: "10px 22px",
              borderRadius: 999,
              border: `2px solid ${accent}`,
              background: `${accent}1a`,
              color: accent,
              fontSize: 28,
              fontWeight: 700,
            }}
          >
            <span
              style={{
                width: 12,
                height: 12,
                borderRadius: 999,
                background: accent,
                display: "block",
              }}
            />
            {token.label}
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 10, color: "#a1a1aa", fontSize: 26 }}>
            <span style={{ fontWeight: 900, color: "#ededed" }}>문어</span>
            <span style={{ color: "#3ecf8e", fontWeight: 700 }}>CBT</span>
          </div>
        </div>

        {/* 중앙 — 회차 정보 */}
        <div
          style={{
            flex: 1,
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            marginTop: 24,
          }}
        >
          {subtitle && (
            <div
              style={{
                fontSize: 36,
                color: "#a1a1aa",
                marginBottom: 18,
                fontWeight: 600,
              }}
            >
              {subtitle}
            </div>
          )}
          <div
            style={{
              fontSize: 88,
              fontWeight: 900,
              lineHeight: 1.1,
              letterSpacing: "-0.02em",
              color: "#ffffff",
            }}
          >
            기출 복원
          </div>
          <div
            style={{
              fontSize: 32,
              color: "#a1a1aa",
              marginTop: 18,
              maxWidth: 980,
              lineHeight: 1.4,
            }}
          >
            {token.labelLong} 기출 문제·정답·해설을 한 번에
          </div>
        </div>

        {/* 하단 — 메타 정보 */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 24,
            color: "#a1a1aa",
            fontSize: 24,
            borderTop: "1px solid #2a2a2e",
            paddingTop: 22,
          }}
        >
          {exam?.totalQuestions ? <span>{exam.totalQuestions}문항</span> : null}
          {exam?.examDate ? (
            <>
              <span style={{ color: "#525252" }}>·</span>
              <span>
                시험일{" "}
                {new Date(exam.examDate).toLocaleDateString("ko-KR", {
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                })}
              </span>
            </>
          ) : null}
          <span style={{ marginLeft: "auto", color: "#3ecf8e", fontWeight: 700 }}>
            sqldpass.com
          </span>
        </div>
      </div>
    ),
    { ...size },
  );
}
