import fs from "fs";
import path from "path";
import type { CertKey } from "@/lib/cert-tokens";

export const OG_IMAGE_SIZE = { width: 1200, height: 630 } as const;
export const DEFAULT_OG_IMAGE_URL = "/opengraph-image";

export const CERT_OG_TITLES: Record<CertKey, string> = {
  SQLD: "SQLD CBT 무료 모의고사",
  ENGINEER_PRACTICAL: "정보처리기사 실기 CBT",
  ENGINEER_WRITTEN: "정보처리기사 필기 CBT",
  COMPUTER_LITERACY_1: "컴활 1급 CBT",
  COMPUTER_LITERACY_2: "컴활 2급 CBT",
  ADSP: "ADsP CBT",
};

const CERT_ACCENTS: Record<CertKey, string> = {
  SQLD: "#f59e0b",
  ENGINEER_PRACTICAL: "#10b981",
  ENGINEER_WRITTEN: "#f43f5e",
  COMPUTER_LITERACY_1: "#0ea5e9",
  COMPUTER_LITERACY_2: "#6366f1",
  ADSP: "#14b8a6",
};

let cachedBackgroundDataUrl: string | null | undefined;

function getOgBackgroundDataUrl(): string | null {
  if (cachedBackgroundDataUrl !== undefined) return cachedBackgroundDataUrl;

  try {
    const backgroundPath = path.join(process.cwd(), "public", "og", "og-bg-cbt.png");
    const image = fs.readFileSync(backgroundPath);
    cachedBackgroundDataUrl = `data:image/png;base64,${image.toString("base64")}`;
  } catch {
    cachedBackgroundDataUrl = null;
  }

  return cachedBackgroundDataUrl;
}

export function getCertAccent(cert: CertKey): string {
  return CERT_ACCENTS[cert];
}

export function CbtOgStaticImage() {
  const backgroundDataUrl = getOgBackgroundDataUrl();

  if (!backgroundDataUrl) {
    return (
      <CbtOgFrame
        eyebrow="무료 CBT 모의고사"
        title="SQLD · 정보처리기사 · 컴활"
        subtitle="실전 타이머 · 자동 채점 · 오답 복습"
      />
    );
  }

  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        background: "#09090b",
      }}
    >
      {/* eslint-disable-next-line @next/next/no-img-element -- next/og ImageResponse renders data URLs through a plain img. */}
      <img
        src={backgroundDataUrl}
        alt=""
        style={{
          width: "100%",
          height: "100%",
          objectFit: "cover",
        }}
      />
    </div>
  );
}

export function CbtOgFrame({
  eyebrow,
  title,
  subtitle,
  accent = "#f59e0b",
}: {
  eyebrow: string;
  title: string;
  subtitle: string;
  accent?: string;
}) {
  const backgroundDataUrl = getOgBackgroundDataUrl();

  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        position: "relative",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        overflow: "hidden",
        background: "#09090b",
        color: "#fafafa",
        fontFamily: "sans-serif",
      }}
    >
      {backgroundDataUrl ? (
        // eslint-disable-next-line @next/next/no-img-element -- next/og ImageResponse renders data URLs through a plain img.
        <img
          src={backgroundDataUrl}
          alt=""
          style={{
            position: "absolute",
            inset: 0,
            width: "100%",
            height: "100%",
            objectFit: "cover",
          }}
        />
      ) : null}
      <div
        style={{
          position: "absolute",
          inset: 0,
          background:
            "linear-gradient(90deg, rgba(9,9,11,0.96) 0%, rgba(9,9,11,0.82) 42%, rgba(9,9,11,0.38) 100%)",
        }}
      />
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: `linear-gradient(135deg, ${accent}26 0%, transparent 36%)`,
        }}
      />

      <div
        style={{
          position: "relative",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          width: "100%",
          height: "100%",
          padding: "64px 74px",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 12,
            alignSelf: "flex-start",
            padding: "10px 18px",
            borderRadius: 999,
            border: `1px solid ${accent}66`,
            background: "rgba(9, 9, 11, 0.72)",
            color: "#f4f4f5",
            fontSize: 22,
            fontWeight: 700,
            letterSpacing: 0,
          }}
        >
          <span
            style={{
              width: 10,
              height: 10,
              borderRadius: 999,
              background: accent,
            }}
          />
          {eyebrow}
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 22 }}>
          <h1
            style={{
              margin: 0,
              maxWidth: 760,
              fontSize: title.length > 16 ? 70 : 78,
              lineHeight: 1.08,
              fontWeight: 900,
              letterSpacing: 0,
              color: "#ffffff",
            }}
          >
            {title}
          </h1>
          <p
            style={{
              margin: 0,
              maxWidth: 760,
              fontSize: 30,
              lineHeight: 1.35,
              fontWeight: 600,
              letterSpacing: 0,
              color: "rgba(250,250,250,0.74)",
            }}
          >
            {subtitle}
          </p>
        </div>

        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            color: "rgba(250,250,250,0.72)",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: 8,
                background: accent,
                color: "#09090b",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: 26,
                fontWeight: 900,
              }}
            >
              문
            </div>
            <div style={{ display: "flex", gap: 8, fontSize: 31, fontWeight: 850 }}>
              <span style={{ color: "#ffffff" }}>문어</span>
              <span style={{ color: accent }}>CBT</span>
            </div>
          </div>
          <div style={{ fontFamily: "monospace", fontSize: 22, color: "#d4d4d8" }}>
            sqldpass.com
          </div>
        </div>
      </div>
    </div>
  );
}
