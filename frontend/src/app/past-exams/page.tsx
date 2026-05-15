import type { Metadata } from "next";
import Link from "next/link";

import {
  PastExamCard,
  PastExamGrid,
  PastExamTabs,
} from "@/components/past-exams/PastExamCatalog";
import { Card, Container } from "@/components/ui";
import {
  CERT_LIST,
  CERT_TOKENS,
  certFromSlug,
  slugFromCert,
  type CertKey,
} from "@/lib/cert-tokens";
import {
  buildPastExamCountsByCert,
  buildPastExamNewCountsByCert,
  flattenPastExamLists,
  loadPastExamListsByCert,
} from "@/lib/pastExamCatalog";
import { buildPastExamCertMetadata } from "@/lib/pastExamSeo";

const TITLE = "기출 복원 모의고사 — SQLD·정처기·컴활·ADsP";
const DESCRIPTION =
  "SQLD·정보처리기사·컴퓨터활용능력·ADsP 의 최신 정기 회차 기출을 복원해 실전 타이머·자동 채점·해설로 풀어볼 수 있습니다. 가입 없이 문제 미리보기, 로그인 시 채점·오답노트 자동 저장.";

const DEFAULT_METADATA: Metadata = {
  title: TITLE,
  description: DESCRIPTION,
  alternates: { canonical: "/past-exams" },
  openGraph: {
    type: "website",
    url: "/past-exams",
    title: TITLE,
    description: DESCRIPTION,
    siteName: "문어CBT",
    locale: "ko_KR",
  },
  twitter: {
    card: "summary_large_image",
    title: TITLE,
    description: DESCRIPTION,
  },
};

function parseCertParam(raw: string | undefined): CertKey | null {
  if (!raw) return null;
  // 대문자 키(예: "SQLD") 가 그대로 들어오는 경우와 slug(예: "sqld") 둘 다 지원
  if (raw in CERT_TOKENS) return raw as CertKey;
  return certFromSlug(raw);
}

export async function generateMetadata({
  searchParams,
}: {
  searchParams: Promise<{ cert?: string }>;
}): Promise<Metadata> {
  const { cert } = await searchParams;
  const certKey = parseCertParam(cert);
  if (certKey) {
    return buildPastExamCertMetadata(slugFromCert(certKey));
  }
  return DEFAULT_METADATA;
}

export default async function PastExamsPage({
  searchParams,
}: {
  searchParams: Promise<{ cert?: string }>;
}) {
  const { cert: certParam } = await searchParams;
  const cert = parseCertParam(certParam);

  const listsByCert = await loadPastExamListsByCert();
  const countsByCert = buildPastExamCountsByCert(listsByCert);

  // ── 자격증 선택됨 — 탭 + 그리드 (기존 PastExamCertPage 흐름)
  if (cert) {
    const newCountsByCert = buildPastExamNewCountsByCert(listsByCert);
    const exams = listsByCert[cert] ?? [];
    const token = CERT_TOKENS[cert];

    return (
      <main className="min-h-screen bg-bg text-text">
        <Container size="narrow" className="py-16">
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
            {token.label} 기출 복원
          </h1>
          <p className="mt-2 text-sm text-text-muted">
            {token.labelLong} 기출 복원 문제를 로그인 없이 확인하고, 로그인 후 채점과 해설까지 이어서 볼 수 있습니다.
          </p>

          <PastExamTabs
            activeCert={cert}
            countsByCert={countsByCert}
            newCountsByCert={newCountsByCert}
          />

          <div className="mt-8">
            {exams.length === 0 ? (
              <Card padding="lg" className="text-center">
                <p className="text-base font-semibold">아직 준비 중입니다</p>
                <p className="mt-2 text-sm text-text-muted">
                  {token.label} 기출 복원 회차가 아직 등록되지 않았습니다.
                </p>
              </Card>
            ) : (
              <PastExamGrid exams={exams} />
            )}
          </div>

          <div className="mt-14 flex items-center justify-center gap-6 text-sm text-text-muted">
            <Link href="/solve" className="transition-colors hover:text-text">
              무한 문제 풀기
            </Link>
            <span className="text-border">·</span>
            <Link href="/mock-exams" className="transition-colors hover:text-text">
              모의고사
            </Link>
          </div>
        </Container>
      </main>
    );
  }

  // ── 자격증 미선택 — 메인 카탈로그 (자격증 4개 카드 + 최근 추가 회차)
  const latestExams = flattenPastExamLists(listsByCert)
    .slice()
    .sort((a, b) => {
      const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return timeB - timeA;
    })
    .slice(0, 8);

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-16">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">기출 복원</h1>
        <p className="mt-2 text-sm text-text-muted">
          SQLD, 정보처리기사, 컴퓨터활용능력, ADsP 기출 복원 문제를 자격증별로 모아두었습니다. 문제는 로그인 없이 볼 수 있고, 채점과 해설은 로그인 후 이어서 확인할 수 있습니다.
        </p>

        <div className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-2">
          {CERT_LIST.map((certItem) => {
            const count = countsByCert[certItem.key] ?? 0;
            const latest = listsByCert[certItem.key]?.[0] ?? null;

            return (
              <Link
                key={certItem.key}
                href={`/past-exams?cert=${certItem.key}`}
                scroll={false}
                className="group block"
              >
                <Card variant="interactive" padding="lg" accent={certItem.key}>
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">
                        {certItem.label}
                      </p>
                      <h2 className="mt-1 text-lg font-semibold">{certItem.labelLong}</h2>
                    </div>
                    <span className="text-sm font-semibold tabular-nums text-text-muted">
                      {count}회차
                    </span>
                  </div>
                  <p className="mt-3 text-sm text-text-muted">
                    {latest
                      ? `${latest.examYear ?? "최근"}년 ${latest.examRound ? `${latest.examRound}회` : latest.name} 포함`
                      : "등록된 회차가 없습니다."}
                  </p>
                  <div className="mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-primary transition-transform group-hover:translate-x-1">
                    자격증별 기출 보러가기 →
                  </div>
                </Card>
              </Link>
            );
          })}
        </div>

        <section className="mt-12">
          <div className="mb-4 flex items-end justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold tracking-tight">최근 추가된 회차</h2>
              <p className="mt-1 text-sm text-text-muted">
                새로 복원된 기출 회차를 바로 확인할 수 있습니다.
              </p>
            </div>
          </div>

          {latestExams.length === 0 ? (
            <Card padding="lg" className="text-center">
              <p className="text-base font-semibold">아직 등록된 기출 회차가 없습니다</p>
            </Card>
          ) : (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {latestExams.map((exam) => (
                <PastExamCard key={exam.id} exam={exam} />
              ))}
            </div>
          )}
        </section>

        <div className="mt-14 flex items-center justify-center gap-6 text-sm text-text-muted">
          <Link href="/solve" className="transition-colors hover:text-text">
            무한 문제 풀기
          </Link>
          <span className="text-border">·</span>
          <Link href="/mock-exams" className="transition-colors hover:text-text">
            모의고사
          </Link>
        </div>
      </Container>
    </main>
  );
}
