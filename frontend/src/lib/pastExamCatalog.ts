import { CERT_LIST, type CertKey, slugFromCert } from "@/lib/cert-tokens";
import {
  getPublicPastExamsByCert,
  type PublicPastExamSummary,
} from "@/lib/publicApi";

export type PastExamListsByCert = Record<CertKey, PublicPastExamSummary[]>;
export type PastExamCountsByCert = Record<CertKey, number>;

export async function loadPastExamListsByCert(): Promise<PastExamListsByCert> {
  const entries = await Promise.all(
    CERT_LIST.map(async (cert) => [
      cert.key,
      await getPublicPastExamsByCert(slugFromCert(cert.key)).catch(() => []),
    ] as const),
  );

  return Object.fromEntries(entries) as PastExamListsByCert;
}

export function buildPastExamCountsByCert(
  listsByCert: PastExamListsByCert,
): PastExamCountsByCert {
  return Object.fromEntries(
    CERT_LIST.map((cert) => [cert.key, listsByCert[cert.key]?.length ?? 0]),
  ) as PastExamCountsByCert;
}

export function flattenPastExamLists(
  listsByCert: PastExamListsByCert,
): PublicPastExamSummary[] {
  return CERT_LIST.flatMap((cert) => listsByCert[cert.key] ?? []);
}
