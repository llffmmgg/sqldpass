import { ImageResponse } from "next/og";
import { CbtOgStaticImage, OG_IMAGE_SIZE } from "@/lib/ogImageTemplate";
import { CERT_LIST, slugFromCert } from "@/lib/cert-tokens";

export const runtime = "nodejs";
export const alt = "문어CBT 자격증별 무료 CBT 모의고사";
export const size = OG_IMAGE_SIZE;
export const contentType = "image/png";

type Params = { cert: string };

export function generateStaticParams() {
  return CERT_LIST.map((cert) => ({ cert: slugFromCert(cert.key) }));
}

export default async function CertOgImage({
  params,
}: {
  params: Promise<Params> | Params;
}) {
  await Promise.resolve(params);
  return new ImageResponse(<CbtOgStaticImage />, { ...size });
}
