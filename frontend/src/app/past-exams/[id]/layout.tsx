import type { Metadata } from "next";

import { getPublicPastExam, type PublicPastExamDetail } from "@/lib/publicApi";
import { CERT_DISPLAY, buildRoundTitle } from "@/lib/pastExamRoundTitle";

const SITE_URL = "https://www.sqldpass.com";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const examId = Number(id);

  if (!Number.isFinite(examId)) {
    return { title: "기출 복원 | 문어CBT" };
  }

  try {
    const exam = await getPublicPastExam(examId);
    const roundTitle = buildRoundTitle(exam);
    const title = `${roundTitle} 기출 복원 | ${exam.totalQuestions}문항 무료 CBT | 문어CBT`;
    const description = `${roundTitle} 기출 복원 ${exam.totalQuestions}문항을 실전처럼 풀어보고, 로그인 후 채점과 해설까지 이어서 확인할 수 있는 무료 CBT 페이지입니다.`;
    const canonical = `${SITE_URL}/past-exams/${examId}`;

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
      title: "기출 복원 | 무료 CBT | 문어CBT",
      description:
        "실전처럼 기출 복원 문제를 풀어보고 로그인 후 채점과 해설까지 확인할 수 있는 무료 CBT 페이지입니다.",
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
    description: `${roundTitle} 기출 복원 ${exam.totalQuestions}문항 CBT. 문제는 로그인 없이 볼 수 있고, 채점과 해설 확인은 로그인 후 가능합니다.`,
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
  const examId = Number(id);

  let ldBlocks: ReturnType<typeof buildJsonLd> | null = null;
  if (Number.isFinite(examId)) {
    try {
      const exam = await getPublicPastExam(examId);
      ldBlocks = buildJsonLd(examId, exam);
    } catch {
      ldBlocks = null;
    }
  }

  return (
    <>
      {ldBlocks?.map((ld, index) => (
        <script
          key={index}
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(ld) }}
        />
      ))}
      {children}
    </>
  );
}
