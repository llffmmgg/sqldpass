import { ImageResponse } from "next/og";

export const runtime = "edge";
export const alt = "SQLD Pass — IT 자격증 실전 모의고사";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default async function OgImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          padding: "72px 80px",
          background:
            "radial-gradient(ellipse 80% 60% at 20% 10%, rgba(245,158,11,0.22), transparent 60%), radial-gradient(ellipse 80% 60% at 85% 90%, rgba(139,92,246,0.22), transparent 60%), #09090b",
          color: "#fafafa",
          fontFamily: "sans-serif",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 10,
              padding: "10px 20px",
              borderRadius: 999,
              border: "1px solid rgba(139,92,246,0.4)",
              background: "rgba(139,92,246,0.1)",
              color: "#c4b5fd",
              fontSize: 22,
              fontWeight: 500,
            }}
          >
            <div
              style={{
                width: 10,
                height: 10,
                borderRadius: 999,
                background: "#a78bfa",
              }}
            />
            매번 새로 추가되는 문제 · 무료 CBT
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
          <div
            style={{
              fontSize: 88,
              fontWeight: 800,
              lineHeight: 1.1,
              letterSpacing: -2,
            }}
          >
            <div style={{ color: "#fafafa" }}>IT 자격증,</div>
            <div
              style={{
                background:
                  "linear-gradient(90deg, #fbbf24 0%, #fcd34d 50%, #f59e0b 100%)",
                backgroundClip: "text",
                color: "transparent",
              }}
            >
              실전 모의고사로 합격
            </div>
          </div>
          <div
            style={{
              fontSize: 30,
              color: "rgba(250,250,250,0.7)",
              fontFamily: "monospace",
              display: "flex",
              gap: 14,
            }}
          >
            <span style={{ color: "#fbbf24" }}>SQL</span>
            <span>·</span>
            <span style={{ color: "#c4b5fd" }}>C</span>
            <span>·</span>
            <span style={{ color: "#7dd3fc" }}>Java</span>
            <span>·</span>
            <span style={{ color: "#6ee7b7" }}>Python</span>
          </div>
        </div>

        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            fontSize: 28,
            color: "rgba(250,250,250,0.6)",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: 10,
                background: "#fbbf24",
                color: "#09090b",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: 26,
                fontWeight: 800,
              }}
            >
              S
            </div>
            <div style={{ color: "#fafafa", fontSize: 32, fontWeight: 700 }}>
              SQLD <span style={{ color: "#fbbf24" }}>Pass</span>
            </div>
          </div>
          <div style={{ fontFamily: "monospace", fontSize: 22 }}>
            sqldpass.com
          </div>
        </div>
      </div>
    ),
    { ...size },
  );
}
