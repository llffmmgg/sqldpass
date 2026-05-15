import { ImageResponse } from "next/og";
import { CbtOgStaticImage, OG_IMAGE_SIZE } from "@/lib/ogImageTemplate";

export const runtime = "nodejs";
export const alt = "문어CBT 무료 CBT 모의고사";
export const size = OG_IMAGE_SIZE;
export const contentType = "image/png";

export default async function OgImage() {
  return new ImageResponse(<CbtOgStaticImage />, { ...size });
}
