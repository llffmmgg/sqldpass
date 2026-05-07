import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";

import { PastExamCard } from "@/components/past-exams/PastExamCatalog";
import { Card, Container } from "@/components/ui";
import {
  CERT_LIST,
  CERT_TOKENS,
  slugFromCert,
  type CertKey,
} from "@/lib/cert-tokens";
import {
  buildPastExamCountsByCert,
  flattenPastExamLists,
  loadPastExamListsByCert,
} from "@/lib/pastExamCatalog";

const TITLE = "기출 복원 모의고사 — SQLD·정처기·컴활·ADsP";
const DESCRIPTION =
  "SQLD·정보처리기사·컴퓨터활용능력·ADsP 의 최신 정기 회차 기출을 복원해 실전 타이머·자동 채점·해설로 풀어볼 수 있습니다. 가입 없이 문제 미리보기, 로그인 시 채점·오답노트 자동 저장.";

export const metadata: Metadata = {
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

export default async function PastExamsPage({
  searchParams,
}: {
  searchParams: Promise<{ cert?: string }>;
}) {
  const { cert } = await searchParams;
  if (cert && cert in CERT_TOKENS) {
    redirect(`/past-exams/${slugFromCert(cert as CertKey)}`);
  }

  const listsByCert = await loadPastExamListsByCert();
  const countsByCert = buildPastExamCountsByCert(listsByCert);
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
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
          기출 복원
        </h1>
        <p className="mt-2 text-sm text-text-muted">
          SQLD, 정보처리기사, 컴퓨터활용능력, ADsP 기출 복원 문제를 자격증별로 모아두었습니다. 문제는 로그인 없이 볼 수 있고, 채점과 해설은 로그인 후 이어서 확인할 수 있습니다.
        </p>

        <div className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-2">
          {CERT_LIST.map((cert) => {
            const slug = slugFromCert(cert.key);
            const count = countsByCert[cert.key] ?? 0;
            const latest = listsByCert[cert.key]?.[0] ?? null;

            return (
              <Link key={cert.key} href={`/past-exams/${slug}`} className="group block">
                <Card variant="interactive" padding="lg" accent={cert.key}>
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">
                        {cert.label}
                      </p>
                      <h2 className="mt-1 text-lg font-semibold">{cert.labelLong}</h2>
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
