import type { Metadata } from "next";
import Link from "next/link";
import { Container } from "@/components/ui";

export const metadata: Metadata = {
  title: "sqldpass 소개",
  description:
    "sqldpass는 SQLD, 정보처리기사 필기·실기, 컴퓨터활용능력 1·2급, ADsP를 무료로 풀어볼 수 있는 CBT 모의고사 사이트입니다.",
  alternates: { canonical: "https://www.sqldpass.com/about" },
};

export default function AboutPage() {
  const faqLd = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: [
      {
        "@type": "Question",
        name: "무엇을 제공하나요?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "실제 시험과 비슷한 환경의 모의고사 자동 생성, 주제별 무한 풀이 모드, 오답노트 자동 누적, 풀이 통계 및 대시보드. 모든 기능 무료, 회원가입은 Google 로그인 한 번이면 끝.",
        },
      },
      {
        "@type": "Question",
        name: "문제는 어떻게 만들어지나요?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "운영팀이 정리한 토픽별 시드 문제를 기반으로, 변형 문제를 생성합니다. 생성된 문제는 다시 LLM 검증과 운영자 검토를 거칩니다.",
        },
      },
      {
        "@type": "Question",
        name: "왜 도메인은 sqldpass.com인가요?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "저희 사이트는 문어CBT입니다. 처음에 SQLD만 다룰 때 만든 도메인이 sqldpass.com이라, 이름은 바뀌어도 주소는 그대로 갑니다. 이제는 CBT로 붙는 자격증이라면 한 곳에서 다 풀 수 있어요.",
        },
      },
      {
        "@type": "Question",
        name: "누가 만드나요?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "sqldpass는 자격증 학습자를 위해 1인 운영팀이 만들고 있습니다. 운영비(서버 + AI API)는 일부 광고로 충당합니다.",
        },
      },
      {
        "@type": "Question",
        name: "문의 / 피드백은 어떻게 하나요?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "오류 신고, 기능 제안, 기타 문의는 사이트 내 피드백 기능으로 보내주세요.",
        },
      },
    ],
  };

  return (
    <main className="py-12 text-foreground">
      <Container size="narrow">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(faqLd) }}
      />
      <h1 className="text-3xl font-bold">문어CBT란?</h1>
      <p className="mt-3 text-base text-muted">
        문어CBT는 <strong className="text-foreground">CBT 모의고사 플랫폼</strong>입니다.
        SQLD · 정처기 필기/실기 · 컴활 1/2급 · ADsP를 무료로 풀어볼 수 있어요.
        모든 기능 무료, 회원가입은 Google 로그인 한 번이면 끝.
      </p>

      <Section title="무엇을 제공하나요?">
        <ul className="list-disc pl-5">
          <li>실제 시험과 비슷한 환경의 모의고사 자동 생성</li>
          <li>주제별 무한 풀이 모드 (푼 문제는 자동으로 뒤로 밀려남)</li>
          <li>오답노트 자동 누적 + 다시 풀기</li>
          <li>풀이 통계, 과목별 정답률, 학습 진척 대시보드</li>
          <li>모든 기능 무료, 회원가입은 Google 로그인 한 번이면 끝</li>
        </ul>
      </Section>

      <Section title="문제는 어떻게 만들어지나요?">
        <p>
          운영팀이 정리한 토픽별 시드 문제를 기반으로, 변형 문제를 생성합니다.
          생성된 문제는 다시 LLM 검증과 운영자 검토를 거쳐 오류가 있으면 수정·삭제됩니다.
          그래도 일부 오류는 남을 수 있으니, 발견하시면{" "}
          <Link href="/profile" className="text-primary underline underline-offset-2 transition-colors hover:text-primary-hover">피드백</Link>으로 알려주세요.
        </p>
      </Section>

      <Section title="왜 도메인은 sqldpass.com인가요?">
        <p>
          저희 사이트는 문어CBT예요. 🐙 근데 처음에 SQLD만 다룰 때 만든 도메인이
          sqldpass.com이라, 이름은 바뀌어도 주소는 그대로 갑니다. 이제는 CBT로
          붙는 자격증이라면 한 곳에서 다 풀 수 있어요.
        </p>
      </Section>

      <Section title="누가 만드나요?">
        <p>
          sqldpass는 자격증 학습자를 위해 1인 운영팀이 만들고 있습니다. 운영비(서버 + AI API)는
          일부 광고로 충당합니다. 학습 흐름을 방해하지 않는 위치에만 광고를 표시합니다.
        </p>
      </Section>

      <Section title="문의 / 피드백">
        <p>
          오류 신고, 기능 제안, 기타 문의는 사이트 내{" "}
          <Link href="/profile" className="text-primary underline underline-offset-2 transition-colors hover:text-primary-hover">피드백</Link> 기능으로 보내주세요.
        </p>
      </Section>

      <div className="mt-10 flex flex-wrap gap-3">
        <Link
          href="/learn"
          className="rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-fg transition-all hover:bg-primary-hover hover:scale-[1.02] active:scale-[0.98]"
        >
          학습 시작하기
        </Link>
        <Link
          href="/mock-exams"
          className="rounded-lg border border-border px-5 py-2.5 text-sm font-semibold hover:bg-surface"
        >
          모의고사 보러가기
        </Link>
      </div>
      </Container>
    </main>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-8 text-sm leading-relaxed text-muted">
      <h2 className="text-lg font-semibold text-foreground">{title}</h2>
      <div className="mt-2 space-y-2">{children}</div>
    </section>
  );
}
