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

const PAST_EXAM_NEW_WINDOW_DAYS = 3;

/**
 * 자격증별로 최근 3일 안에 추가된 기출 회차 개수.
 * PastExamCard 의 NEW 표시(`isWithinDays(createdAt, 3)`) 와 동일 정책.
 */
export function buildPastExamNewCountsByCert(
  listsByCert: PastExamListsByCert,
): PastExamCountsByCert {
  const windowMs = PAST_EXAM_NEW_WINDOW_DAYS * 24 * 60 * 60 * 1000;
  const now = Date.now();
  return Object.fromEntries(
    CERT_LIST.map((cert) => {
      const list = listsByCert[cert.key] ?? [];
      const newCount = list.filter((e) => {
        if (!e.createdAt) return false;
        const t = new Date(e.createdAt).getTime();
        if (Number.isNaN(t)) return false;
        return now - t <= windowMs;
      }).length;
      return [cert.key, newCount];
    }),
  ) as PastExamCountsByCert;
}

export function flattenPastExamLists(
  listsByCert: PastExamListsByCert,
): PublicPastExamSummary[] {
  return CERT_LIST.flatMap((cert) => listsByCert[cert.key] ?? []);
}
