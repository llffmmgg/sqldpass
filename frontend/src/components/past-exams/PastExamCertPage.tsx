import Link from "next/link";
import { notFound } from "next/navigation";

import { Card, Container } from "@/components/ui";
import {
  CERT_TOKENS,
  certFromSlug,
} from "@/lib/cert-tokens";
import {
  buildPastExamCountsByCert,
  loadPastExamListsByCert,
} from "@/lib/pastExamCatalog";
import { PastExamGrid, PastExamTabs } from "@/components/past-exams/PastExamCatalog";

export default async function PastExamCertPage({
  certSlug,
}: {
  certSlug: string;
}) {
  const cert = certFromSlug(certSlug);
  if (!cert) {
    notFound();
  }

  const listsByCert = await loadPastExamListsByCert();
  const countsByCert = buildPastExamCountsByCert(listsByCert);
  const exams = listsByCert[cert];
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

        <PastExamTabs activeCert={cert} countsByCert={countsByCert} />

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
