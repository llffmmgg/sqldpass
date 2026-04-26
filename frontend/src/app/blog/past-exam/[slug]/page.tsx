import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import QuestionContent from "@/components/QuestionContent";
import AdResponsive from "@/components/AdResponsive";
import { Badge, ButtonLink, Card, Container } from "@/components/ui";
import {
  CERT_TOKENS,
  certFromExamType,
  slugFromCert,
  type CertKey,
} from "@/lib/cert-tokens";
import { parseQuestion } from "@/lib/parseQuestion";
import {
  getPublicPastExamWithAnswers,
  getPublicPastExamsByCert,
  type PublicPastExamDetailWithAnswers,
  type PublicPastExamQuestionWithAnswer,
  type PublicPastExamSummary,
} from "@/lib/publicApi";
import {
  certFromBlogPastExamSlug,
  findPastExamBySlug,
  pastExamBlogDescription,
  pastExamBlogTitle,
} from "@/lib/pastExamBlog";

export const dynamic = "force-dynamic";

type Params = { slug: string };

const CERT_SLUGS = ["sqld", "engineer", "engineer-written", "computer-literacy-1", "computer-literacy-2", "adsp"];

async function findExamBySlug(slug: string): Promise<PublicPastExamSummary | null> {
  const cert = certFromBlogPastExamSlug(slug);
  const certSlugs = cert ? [slugFromCert(cert)] : CERT_SLUGS;
  for (const cs of certSlugs) {
    const list = await getPublicPastExamsByCert(cs).catch(() => [] as PublicPastExamSummary[]);
    const found = findPastExamBySlug(list, slug);
    if (found) return found;
  }
  return null;
}

export async function generateMetadata({
  params,
}: {
  params: Promise<Params>;
}): Promise<Metadata> {
  const { slug } = await params;
  const exam = await findExamBySlug(slug);
  if (!exam) return { title: "글을 찾을 수 없습니다" };

  const title = pastExamBlogTitle(exam);
  const description = pastExamBlogDescription(exam);
  const canonical = `https://www.sqldpass.com/blog/past-exam/${slug}`;

  return {
    title,
    description,
    alternates: { canonical },
    openGraph: {
      type: "article",
      title: `${title} | 문어CBT`,
      description,
      url: canonical,
    },
    twitter: {
      card: "summary_large_image",
      title,
      description,
    },
  };
}

export default async function PastExamBlogPage({
  params,
}: {
  params: Promise<Params>;
}) {
  const { slug } = await params;
  const summary = await findExamBySlug(slug);
  if (!summary) notFound();

  const detail = await getPublicPastExamWithAnswers(summary.id).catch(() => null);
  if (!detail) notFound();

  const cert: CertKey = certFromExamType(detail.examType) ?? "SQLD";
  const token = CERT_TOKENS[cert];
  const title = pastExamBlogTitle(summary);
  const description = pastExamBlogDescription(summary);
  const runnerHref = `/past-exams/${detail.id}`;

  const articleLd = {
    "@context": "https://schema.org",
    "@type": "Article",
    headline: title,
    description,
    datePublished: summary.createdAt,
    author: { "@type": "Organization", name: "문어CBT" },
    publisher: { "@type": "Organization", name: "문어CBT" },
    mainEntityOfPage: `https://www.sqldpass.com/blog/past-exam/${slug}`,
  };

  return (
    <Container size="narrow" className="py-12">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(articleLd) }}
      />

      <nav className="text-sm text-text-muted">
        <Link href="/blog" className="hover:text-text">
          시험 준비 팁
        </Link>
        <span className="mx-2">/</span>
        <Link href={`/past-exams/${slugFromCert(cert)}`} className="hover:text-text">
          {token.label} 기출 복원
        </Link>
      </nav>

      <header className="mx-auto mt-6 max-w-[44rem]">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <Badge cert={cert} variant="soft" size="sm">
            {token.label}
          </Badge>
          {detail.examDate && (
            <span className="text-text-muted">
              시험일{" "}
              {new Date(detail.examDate).toLocaleDateString("ko-KR", {
                year: "numeric",
                month: "long",
                day: "numeric",
              })}
            </span>
          )}
          <span className="text-text-subtle">· {detail.totalQuestions}문항</span>
          {detail.expertVerified && (
            <Badge variant="soft" tone="success" size="xs">
              전문가 검수
            </Badge>
          )}
        </div>
        <h1 className="mt-5 text-[1.875rem] font-bold leading-[1.25] tracking-tight sm:text-[2.25rem] md:text-[2.5rem]">
          {title}
        </h1>
        <p className="mt-5 text-base leading-relaxed text-text-muted sm:text-lg">
          {description}
        </p>
        <p className="mt-3 text-sm text-text-subtle">안녕하세요. 문어입니다 🐙</p>
        <hr className="mt-8 border-border" />
      </header>

      <AdResponsive adSlot="9410711496" height={90} />

      <article className="mt-10 space-y-4">
        <p className="text-sm text-text-muted">
          {token.labelLong} {detail.examYear ? `${detail.examYear}년 ` : ""}
          {detail.examRound ? `${detail.examRound}회차 ` : ""}
          기출 복원입니다. 각 문제 아래의 <strong>「정답·해설 보기」</strong>를 펼치면 정답과
          해설이 나타나요. 실제 시험 환경(타이머·자동 채점·오답 누적)에서 풀어보고 싶다면 글
          맨 아래 「직접 풀러가기」를 눌러주세요.
        </p>

        <ol className="mt-6 space-y-6">
          {detail.questions.map((q, idx) => (
            <PastExamQuestionItem key={q.id} question={q} index={idx} />
          ))}
        </ol>
      </article>

      <AdResponsive adSlot="1456719374" height={280} />

      <Card padding="lg" className="mt-12 text-center">
        <h2 className="text-lg font-semibold tracking-tight">
          이번엔 직접 풀어보세요
        </h2>
        <p className="mt-2 text-sm text-text-muted">
          타이머와 자동 채점이 켜진 실제 시험 환경으로 응시할 수 있습니다.
        </p>
        <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
          <ButtonLink href={runnerHref} variant="primary" size="md" glow>
            직접 풀러가기
          </ButtonLink>
          <ButtonLink href={`/past-exams/${slugFromCert(cert)}`} variant="outline" size="md">
            다른 회차 보기
          </ButtonLink>
        </div>
      </Card>
    </Container>
  );
}

function PastExamQuestionItem({
  question,
  index,
}: {
  question: PublicPastExamQuestionWithAnswer;
  index: number;
}) {
  const parsed = parseQuestion(question.content);

  return (
    <li className="rounded-2xl border border-border bg-surface/40 p-5">
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold tabular-nums text-text-muted">
          문제 {index + 1}
        </p>
        <span className="text-[10px] font-medium uppercase tracking-wider text-text-subtle">
          {questionTypeLabel(question.questionType)}
        </span>
      </div>

      <div className="mt-3">
        <QuestionContent content={parsed.body} />
      </div>

      {question.questionType === "MCQ" && parsed.options.length > 0 && (
        <ul className="mt-4 space-y-1.5">
          {parsed.options.map((opt, i) => (
            <li
              key={i}
              className="rounded-md border border-border bg-bg px-3 py-2 text-sm"
            >
              <span className="mr-2 font-semibold tabular-nums">{i + 1}.</span>
              {opt}
            </li>
          ))}
        </ul>
      )}

      <details className="mt-4 group">
        <summary className="cursor-pointer select-none rounded-md border border-primary/30 bg-primary/[0.04] px-3 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary/[0.08]">
          정답·해설 보기
          <span className="ml-2 text-xs text-text-subtle group-open:hidden">▾</span>
          <span className="ml-2 hidden text-xs text-text-subtle group-open:inline">▴</span>
        </summary>

        <div className="mt-3 space-y-3 rounded-md border border-border bg-bg p-4 text-sm">
          {question.questionType === "MCQ" && question.correctOption != null && (
            <div>
              <p className="text-xs font-semibold text-text-muted">정답</p>
              <p className="mt-1 text-base font-semibold text-success">
                {question.correctOption}번
                {parsed.options[question.correctOption - 1] &&
                  `. ${parsed.options[question.correctOption - 1]}`}
              </p>
            </div>
          )}

          {question.questionType !== "MCQ" && question.answer && (
            <div>
              <p className="text-xs font-semibold text-text-muted">모범답안</p>
              <p className="mt-1 text-base font-semibold text-success">
                {question.answer}
              </p>
              {question.keywords.length > 0 && (
                <p className="mt-2 text-xs text-text-muted">
                  키워드: {question.keywords.join(", ")}
                </p>
              )}
            </div>
          )}

          {question.explanation && (
            <div>
              <p className="text-xs font-semibold text-text-muted">해설</p>
              <div className="mt-1 leading-relaxed">
                <QuestionContent content={question.explanation} />
              </div>
            </div>
          )}
        </div>
      </details>
    </li>
  );
}

function questionTypeLabel(type: PublicPastExamDetailWithAnswers["questions"][number]["questionType"]): string {
  if (type === "MCQ") return "4지선다";
  if (type === "SHORT_ANSWER") return "단답형";
  return "서술형";
}
