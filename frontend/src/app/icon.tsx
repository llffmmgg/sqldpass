import { ImageResponse } from "next/og";

export const size = { width: 32, height: 32 };
export const contentType = "image/png";

export default function Icon() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: "linear-gradient(135deg, #f59e0b 0%, #8b5cf6 100%)",
          color: "#0b0b10",
          fontSize: 22,
          fontWeight: 800,
          letterSpacing: -1,
          borderRadius: 6,
        }}
      >
        S
      </div>
    ),
    { ...size },
  );
}
