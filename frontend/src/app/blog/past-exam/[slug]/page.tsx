import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import QuestionContent from "@/components/QuestionContent";
import AdResponsive from "@/components/AdResponsive";
import BlogViewCounter from "@/components/BlogViewCounter";
import { Badge, ButtonLink, Card, Container } from "@/components/ui";
import {
  CERT_TOKENS,
  certFromExamType,
  slugFromCert,
  type CertKey,
} from "@/lib/cert-tokens";
import { parseQuestion } from "@/lib/parseQuestion";
import {
  getPublicBlogViews,
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
  pastExamBlogSlug,
  pastExamBlogTitle,
} from "@/lib/pastExamBlog";
import { getAllPosts, type BlogPostMeta } from "@/lib/blog";

// ISR — 회차 데이터는 거의 변하지 않으므로 1시간 캐시 (구글 크롤러 LCP 개선)
export const revalidate = 3600;

const SITE_URL = "https://www.sqldpass.com";

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

/**
 * 자격증별 cert 풀네임 + 약어. SEO 본문 첫 문단에 둘 다 노출하기 위함.
 * 예: "SQLD(SQL 개발자)", "정보처리기사 필기"
 */
function certCombinedLabel(cert: CertKey): string {
  switch (cert) {
    case "SQLD":
      return "SQLD(SQL 개발자)";
    case "ENGINEER_PRACTICAL":
      return "정보처리기사 실기";
    case "ENGINEER_WRITTEN":
      return "정보처리기사 필기";
    case "COMPUTER_LITERACY_1":
      return "컴퓨터활용능력 1급(컴활 1급)";
    case "COMPUTER_LITERACY_2":
      return "컴퓨터활용능력 2급(컴활 2급)";
    case "ADSP":
      return "ADsP(데이터분석 준전문가)";
  }
}

/** 자격증별 짧은 학습 팁 (SEO 본문용) */
function certStudyTip(cert: CertKey): string {
  switch (cert) {
    case "SQLD":
      return "SQLD 는 1과목 데이터 모델링 과락(8점 미만)만 피하면 2과목 SQL 활용에서 합격선(60점) 도달이 어렵지 않습니다. 회차별 기출 복원을 시간 재서 풀어보고, 윈도우 함수·계층형 쿼리·NULL 함정만 따로 정리해두세요.";
    case "ENGINEER_PRACTICAL":
      return "정보처리기사 실기는 코드 빈칸·약술형 비중이 큽니다. 기출 복원으로 자주 나오는 키워드(SQL, 보안, 디자인 패턴, 신기술 동향)를 익히고, 출제 빈도 높은 챕터부터 회독하는 게 효율적입니다.";
    case "ENGINEER_WRITTEN":
      return "정보처리기사 필기는 5과목 100문항 4지선다입니다. 과목별 40% 과락 + 평균 60점 기준이라 한 과목도 버리지 않는 게 핵심. 기출 복원으로 자주 출제되는 영역을 빠르게 파악하세요.";
    case "COMPUTER_LITERACY_1":
      return "컴퓨터활용능력 1급 필기는 컴퓨터 일반·스프레드시트·데이터베이스 60문항입니다. 매크로(VBA), 함수, 데이터베이스 정규화는 고정 단골이라 기출 복원 회독으로 패턴을 익히는 게 가장 빠릅니다.";
    case "COMPUTER_LITERACY_2":
      return "컴퓨터활용능력 2급 필기는 컴퓨터 일반·스프레드시트 40문항입니다. 1급보다 비중과 난이도가 낮아 기출 복원 2~3회차만 풀어도 합격선 통과 가능합니다.";
    case "ADSP":
      return "ADsP 는 데이터 이해·분석 기획·분석 50문항 4지선다입니다. 2024년 개편 후 통계 비중이 늘었으니 기출 복원에서 통계 계산 문제를 우선 보세요.";
  }
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
  const canonical = `${SITE_URL}/blog/past-exam/${slug}`;
  const cert = certFromExamType(exam.examType);

  // SEO 키워드 — 풀네임/약어/자주 검색되는 변형 모두 포함
  const keywords = [
    cert ? CERT_TOKENS[cert].label : exam.certSlug,
    cert ? CERT_TOKENS[cert].labelLong : "",
    exam.examYear ? `${exam.examYear}년` : "",
    exam.examRound ? `${exam.examRound}회` : "",
    "기출 복원",
    "기출복원",
    "기출문제",
    "정답",
    "해설",
  ].filter(Boolean);

  return {
    title,
    description,
    keywords,
    alternates: { canonical },
    openGraph: {
      type: "article",
      title: `${title} | 문어CBT`,
      description,
      url: canonical,
      siteName: "문어CBT",
      locale: "ko_KR",
      publishedTime: exam.createdAt,
      // og:image 는 Next.js 가 같은 디렉토리의 opengraph-image.tsx 를 자동 사용
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
  const viewSlug = `past-exam-${slug}`;
  const canonical = `${SITE_URL}/blog/past-exam/${slug}`;
  const certCombined = certCombinedLabel(cert);
  const studyTip = certStudyTip(cert);

  // 같은 자격증의 모든 회차 — prev/next + 관련 카드용
  const sameCertExams = await getPublicPastExamsByCert(slugFromCert(cert)).catch(
    () => [] as PublicPastExamSummary[],
  );
  const sortedExams = [...sameCertExams].sort((a, b) => {
    if (a.examYear !== b.examYear) return (b.examYear ?? 0) - (a.examYear ?? 0);
    return (b.examRound ?? 0) - (a.examRound ?? 0);
  });
  const currentIdx = sortedExams.findIndex((e) => e.id === summary.id);
  const newerExam = currentIdx > 0 ? sortedExams[currentIdx - 1] : null;
  const olderExam =
    currentIdx >= 0 && currentIdx < sortedExams.length - 1 ? sortedExams[currentIdx + 1] : null;
  const otherExams = sortedExams.filter((e) => e.id !== summary.id).slice(0, 4);

  // 같은 자격증 카테고리의 mdx 블로그 글 (관련 글 카드)
  const relatedPosts = pickRelatedBlogPosts(cert);

  // 과목별 문항 수 (출제 영역 표)
  const subjectBreakdown = aggregateBySubject(detail.questions);

  let viewCount = 0;
  try {
    const views = await getPublicBlogViews();
    viewCount = views[viewSlug] ?? 0;
  } catch {
    /* 백엔드 미연결 시 무시 */
  }

  // ============== JSON-LD ==============
  const articleLd = {
    "@context": "https://schema.org",
    "@type": "Article",
    headline: title,
    description,
    inLanguage: "ko",
    articleSection: "기출 복원",
    keywords: [token.label, token.labelLong, "기출 복원", "기출복원", "기출문제"]
      .filter(Boolean)
      .join(", "),
    datePublished: summary.createdAt,
    dateModified: summary.createdAt,
    image: `${canonical}/opengraph-image`,
    author: {
      "@type": "Organization",
      name: "문어CBT",
      url: SITE_URL,
    },
    publisher: {
      "@type": "Organization",
      name: "문어CBT",
      url: SITE_URL,
      logo: {
        "@type": "ImageObject",
        url: `${SITE_URL}/logo/logo.webp`,
      },
    },
    mainEntityOfPage: canonical,
  };

  const breadcrumbLd = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "홈", item: SITE_URL },
      {
        "@type": "ListItem",
        position: 2,
        name: "시험 준비 팁",
        item: `${SITE_URL}/blog`,
      },
      {
        "@type": "ListItem",
        position: 3,
        name: `${token.label} 기출 복원`,
        item: `${SITE_URL}/past-exams/${slugFromCert(cert)}`,
      },
      {
        "@type": "ListItem",
        position: 4,
        name: title,
        item: canonical,
      },
    ],
  };

  return (
    <Container size="narrow" className="py-12">
      <BlogViewCounter slug={viewSlug} />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(articleLd) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbLd) }}
      />

      <nav className="text-sm text-text-muted" aria-label="브레드크럼">
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
          <span className="text-text-subtle">· 조회 {viewCount.toLocaleString()}</span>
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
          {certCombined} {detail.examYear ? `${detail.examYear}년 ` : ""}
          {detail.examRound ? `${detail.examRound}회 ` : ""}
          기출 복원 {detail.totalQuestions}문항을 정답·해설과 함께 정리했습니다. 검색해서 들어오신 분들도
          바로 풀어보고 채점까지 가능합니다.
        </p>
        <p className="mt-3 text-sm text-text-subtle">안녕하세요. 문어입니다 🐙</p>
        <hr className="mt-8 border-border" />
      </header>

      <AdResponsive adSlot="9410711496" height={90} />

      {/* SEO 도입부 — H2 헤딩 3개로 검색엔진에 풍부한 컨텍스트 제공 */}
      <section className="mt-10 space-y-8">
        <div>
          <h2 className="text-xl font-semibold tracking-tight">📋 회차 정보</h2>
          <p className="mt-2 text-sm text-text-muted">
            이번 글은 {certCombined} {detail.examYear ? `${detail.examYear}년` : ""}
            {detail.examRound ? ` ${detail.examRound}회` : ""} 기출 복원입니다. 아래는 시험 응시 정보 요약이에요.
          </p>
          <div className="mt-4 overflow-hidden rounded-xl border border-border bg-surface">
            <table className="w-full text-sm">
              <tbody className="divide-y divide-border">
                <tr>
                  <th className="w-32 bg-bg-elevated px-4 py-2.5 text-left font-medium text-text-muted">
                    자격증
                  </th>
                  <td className="px-4 py-2.5 font-semibold">{certCombined}</td>
                </tr>
                {detail.examYear && (
                  <tr>
                    <th className="bg-bg-elevated px-4 py-2.5 text-left font-medium text-text-muted">
                      연도
                    </th>
                    <td className="px-4 py-2.5 font-semibold tabular-nums">
                      {detail.examYear}년
                    </td>
                  </tr>
                )}
                {detail.examRound && (
                  <tr>
                    <th className="bg-bg-elevated px-4 py-2.5 text-left font-medium text-text-muted">
                      회차
                    </th>
                    <td className="px-4 py-2.5 font-semibold tabular-nums">
                      {detail.examRound}회
                    </td>
                  </tr>
                )}
                {detail.examDate && (
                  <tr>
                    <th className="bg-bg-elevated px-4 py-2.5 text-left font-medium text-text-muted">
                      시험일
                    </th>
                    <td className="px-4 py-2.5 font-semibold tabular-nums">
                      {new Date(detail.examDate).toLocaleDateString("ko-KR", {
                        year: "numeric",
                        month: "long",
                        day: "numeric",
                      })}
                    </td>
                  </tr>
                )}
                <tr>
                  <th className="bg-bg-elevated px-4 py-2.5 text-left font-medium text-text-muted">
                    문항 수
                  </th>
                  <td className="px-4 py-2.5 font-semibold tabular-nums">
                    {detail.totalQuestions}문항
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {subjectBreakdown.length > 1 && (
          <div>
            <h2 className="text-xl font-semibold tracking-tight">📊 출제 영역 한눈에</h2>
            <p className="mt-2 text-sm text-text-muted">
              이번 회차의 과목별 문항 분포입니다. 비중이 큰 영역부터 우선 풀이하면 효율적이에요.
            </p>
            <div className="mt-4 overflow-hidden rounded-xl border border-border bg-surface">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border bg-bg-elevated">
                    <th className="px-4 py-2.5 text-left font-medium text-text-muted">과목</th>
                    <th className="px-4 py-2.5 text-right font-medium text-text-muted">문항 수</th>
                    <th className="px-4 py-2.5 text-right font-medium text-text-muted">비중</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {subjectBreakdown.map((row) => (
                    <tr key={row.name}>
                      <td className="px-4 py-2.5 font-medium">{row.name}</td>
                      <td className="px-4 py-2.5 text-right tabular-nums">{row.count}문항</td>
                      <td className="px-4 py-2.5 text-right tabular-nums text-text-muted">
                        {Math.round((row.count / detail.totalQuestions) * 100)}%
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        <div>
          <h2 className="text-xl font-semibold tracking-tight">💡 학습 팁</h2>
          <p className="mt-2 text-sm leading-relaxed text-text-muted">{studyTip}</p>
        </div>
      </section>

      {/* 본문 — 회차의 모든 문제 + 정답·해설 */}
      <article className="mt-12 space-y-4">
        <h2 className="text-xl font-semibold tracking-tight">📝 기출문제 전체 보기</h2>
        <p className="text-sm text-text-muted">
          {token.labelLong} {detail.examYear ? `${detail.examYear}년 ` : ""}
          {detail.examRound ? `${detail.examRound}회차 ` : ""}
          기출 복원입니다. 각 문제 아래의 <strong>「정답·해설 보기」</strong>를 펼치면 정답과
          해설이 나타나요. 실전 시험 환경(타이머·자동 채점·오답 누적)에서 풀어보고 싶다면 글
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
        <h2 className="text-lg font-semibold tracking-tight">이번엔 직접 풀어보세요</h2>
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

      {/* prev/next 회차 */}
      {(newerExam || olderExam) && (
        <nav
          className="mt-10 grid gap-3 sm:grid-cols-2"
          aria-label="이전·다음 회차"
        >
          {olderExam ? (
            <Link
              href={`/blog/past-exam/${pastExamBlogSlug(olderExam)}`}
              className="rounded-xl border border-border bg-surface p-4 transition-colors hover:border-border-strong"
            >
              <p className="text-xs text-text-subtle">← 이전 회차</p>
              <p className="mt-1 text-sm font-semibold">{pastExamBlogTitle(olderExam)}</p>
            </Link>
          ) : (
            <span />
          )}
          {newerExam ? (
            <Link
              href={`/blog/past-exam/${pastExamBlogSlug(newerExam)}`}
              className="rounded-xl border border-border bg-surface p-4 text-right transition-colors hover:border-border-strong sm:col-start-2"
            >
              <p className="text-xs text-text-subtle">다음 회차 →</p>
              <p className="mt-1 text-sm font-semibold">{pastExamBlogTitle(newerExam)}</p>
            </Link>
          ) : null}
        </nav>
      )}

      {/* 같은 자격증 다른 회차 */}
      {otherExams.length > 0 && (
        <section className="mt-12">
          <h2 className="text-lg font-semibold tracking-tight">
            {token.label} 다른 회차 기출 복원
          </h2>
          <ul className="mt-4 grid gap-2 sm:grid-cols-2">
            {otherExams.map((ex) => (
              <li key={ex.id}>
                <Link
                  href={`/blog/past-exam/${pastExamBlogSlug(ex)}`}
                  className="block rounded-lg border border-border bg-surface p-3 text-sm transition-colors hover:border-border-strong"
                >
                  <p className="font-semibold">{pastExamBlogTitle(ex)}</p>
                  <p className="mt-0.5 text-xs text-text-muted">{ex.totalQuestions}문항</p>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* 관련 SEO 글 (같은 자격증 mdx 블로그) */}
      {relatedPosts.length > 0 && (
        <section className="mt-12">
          <h2 className="text-lg font-semibold tracking-tight">함께 읽으면 좋은 글</h2>
          <ul className="mt-4 space-y-2">
            {relatedPosts.map((post) => (
              <li key={post.slug}>
                <Link
                  href={`/blog/${post.slug}`}
                  className="block rounded-lg border border-border bg-surface p-3 transition-colors hover:border-border-strong"
                >
                  <p className="text-sm font-semibold">{post.title}</p>
                  <p className="mt-0.5 text-xs text-text-muted line-clamp-1">
                    {post.description}
                  </p>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}
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

function questionTypeLabel(
  type: PublicPastExamDetailWithAnswers["questions"][number]["questionType"],
): string {
  if (type === "MCQ") return "4지선다";
  if (type === "SHORT_ANSWER") return "단답형";
  return "서술형";
}

function aggregateBySubject(
  questions: PublicPastExamDetailWithAnswers["questions"],
): { name: string; count: number }[] {
  const map = new Map<string, number>();
  for (const q of questions) {
    map.set(q.subjectName, (map.get(q.subjectName) ?? 0) + 1);
  }
  return Array.from(map.entries()).map(([name, count]) => ({ name, count }));
}

/** mdx 블로그에서 같은 자격증 카테고리 글 3개 픽업 (관련 글 카드용) */
function pickRelatedBlogPosts(cert: CertKey): BlogPostMeta[] {
  const blogCategory = CERT_TOKENS[cert].blogCategory;
  return getAllPosts()
    .filter((p) => p.category === blogCategory)
    .slice(0, 3);
}
