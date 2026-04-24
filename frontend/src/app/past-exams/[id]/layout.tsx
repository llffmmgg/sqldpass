import type { Metadata } from "next";
import { getPublicPastExam, type PublicPastExamDetail } from "@/lib/publicApi";

const SITE_URL = "https://www.sqldpass.com";

const CERT_DISPLAY: Record<string, string> = {
  SQLD: "SQLD",
  ENGINEER_PRACTICAL: "정보처리기사 실기",
  ENGINEER_WRITTEN: "정보처리기사 필기",
  COMPUTER_LITERACY_1: "컴퓨터활용능력 1급",
  COMPUTER_LITERACY_2: "컴퓨터활용능력 2급",
  ADSP: "ADsP",
};

function buildRoundTitle(exam: PublicPastExamDetail): string {
  const cert = CERT_DISPLAY[exam.examType] ?? exam.name;
  const parts: string[] = [cert];
  if (exam.examYear) parts.push(`${exam.examYear}년`);
  if (exam.examRound) parts.push(`제${exam.examRound}회`);
  return parts.join(" ");
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const numId = Number(id);
  if (!Number.isFinite(numId)) {
    return { title: "기출 복원 — 문어CBT" };
  }
  try {
    const exam = await getPublicPastExam(numId);
    const roundTitle = buildRoundTitle(exam);
    const title = `${roundTitle} 기출 복원 · ${exam.totalQuestions}문항 무료 CBT — 문어CBT`;
    const description = `${roundTitle} 기출 복원 ${exam.totalQuestions}문항을 실제 시험 시간과 동일한 환경으로 무료 응시. 문제별 정답과 해설은 제출 후 바로 공개되며, 로그인하면 점수와 오답이 자동 저장됩니다.`;
    const canonical = `${SITE_URL}/past-exams/${numId}`;
    return {
      title,
      description,
      alternates: { canonical },
      openGraph: {
        title,
        description,
        url: canonical,
        type: "article",
      },
      twitter: {
        card: "summary_large_image",
        title,
        description,
      },
    };
  } catch {
    return {
      title: "기출 복원 · 무료 CBT — 문어CBT",
      description:
        "실제 시험 시간과 동일한 환경으로 기출 복원 회차를 풀어볼 수 있는 무료 CBT.",
    };
  }
}

function buildJsonLd(id: number, exam: PublicPastExamDetail) {
  const roundTitle = buildRoundTitle(exam);
  const canonical = `${SITE_URL}/past-exams/${id}`;
  const breadcrumb = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "홈", item: `${SITE_URL}/` },
      {
        "@type": "ListItem",
        position: 2,
        name: "기출 복원",
        item: `${SITE_URL}/past-exams`,
      },
      {
        "@type": "ListItem",
        position: 3,
        name: roundTitle,
        item: canonical,
      },
    ],
  };
  const quiz = {
    "@context": "https://schema.org",
    "@type": "Quiz",
    name: `${roundTitle} 기출 복원`,
    description: `${roundTitle} 기출을 복원한 ${exam.totalQuestions}문항 CBT. 무료로 응시 가능하며 제출 후 해설이 공개됩니다.`,
    url: canonical,
    inLanguage: "ko",
    isAccessibleForFree: true,
    learningResourceType: "Quiz",
    numberOfQuestions: exam.totalQuestions,
    educationalUse: "practice",
    about: CERT_DISPLAY[exam.examType] ?? exam.name,
    provider: {
      "@type": "Organization",
      name: "문어CBT",
      url: SITE_URL,
    },
  };
  return [breadcrumb, quiz];
}

export default async function PastExamDetailLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const numId = Number(id);
  let ldBlocks: ReturnType<typeof buildJsonLd> | null = null;
  if (Number.isFinite(numId)) {
    try {
      const exam = await getPublicPastExam(numId);
      ldBlocks = buildJsonLd(numId, exam);
    } catch {
      ldBlocks = null;
    }
  }
  return (
    <>
      {ldBlocks?.map((ld, i) => (
        <script
          key={i}
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(ld) }}
        />
      ))}
      {children}
    </>
  );
}
