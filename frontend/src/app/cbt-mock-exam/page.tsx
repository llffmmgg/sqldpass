import type { Metadata } from "next";
import Link from "next/link";
import { Container } from "@/components/ui";
import { CERT_LIST } from "@/lib/cert-tokens";

const SITE_URL = "https://www.sqldpass.com";

export const metadata: Metadata = {
  title: "무료 CBT 모의고사 | 6종 자격증 | 문어CBT",
  description:
    "SQLD·정처기 필기/실기·컴활 1/2급·ADsP CBT 모의고사를 무료로 풀어보세요. 실제 시험과 동일한 CBT 환경, 매번 새로 구성되는 기출 변형 문제, 오답 자동 복습까지.",
  alternates: { canonical: `${SITE_URL}/cbt-mock-exam` },
  openGraph: {
    title: "무료 CBT 모의고사 — 자격증 6종 한 곳에서 | 문어CBT",
    description:
      "SQLD·정처기·컴활·ADsP CBT 모의고사 무료. 실전 타이머·오답 복습·회차별 실력 추적.",
    url: `${SITE_URL}/cbt-mock-exam`,
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "무료 CBT 모의고사 — 자격증 6종 | 문어CBT",
    description:
      "SQLD·정처기·컴활·ADsP CBT 모의고사 무료. 실전 타이머 제공.",
  },
};

const FAQS = [
  {
    q: "CBT 모의고사 무료인가요?",
    a: "네, 문어CBT의 모든 CBT 모의고사는 완전 무료입니다. 회원가입은 Google 로그인 한 번이면 끝나고, 결제나 구독은 없어요.",
  },
  {
    q: "CBT 모의고사와 PBT(종이 시험) 모의고사의 차이는?",
    a: "CBT는 실제 자격증 시험과 같은 컴퓨터 화면 환경에서 치러요. 타이머·문제 표시·빠른 이동 기능을 실전 그대로 연습할 수 있어요. PBT는 종이 OMR이라 CBT 화면 적응력이 길러지지 않습니다.",
  },
  {
    q: "공식 기출과 얼마나 비슷한가요?",
    a: "문어CBT의 문제는 한국산업인력공단·한국데이터산업진흥원 공식 기출을 기반으로 AI가 변형한 문제입니다. 원문 복제가 아니라 같은 개념·난이도의 새 문제를 매번 새로 구성해요.",
  },
  {
    q: "몇 회 풀어야 합격권에 들 수 있나요?",
    a: "자격증마다 다르지만 대체로 모의고사 2~3회 + 주제별 풀이 200문제 이상이면 합격권에 안정적으로 진입해요. 오답을 자동 누적해 재풀이하는 구조라 효율이 높습니다.",
  },
  {
    q: "모바일에서도 CBT 모의고사 가능한가요?",
    a: "네, 반응형으로 제작되어 있어 모바일·태블릿에서도 풀 수 있어요. 다만 실제 CBT 시험장은 데스크톱 환경이라 가능하면 PC로 연습하는 걸 권장합니다.",
  },
];

export default function CbtMockExamPage() {
  const webPageLd = {
    "@context": "https://schema.org",
    "@type": "WebPage",
    name: "무료 CBT 모의고사 — 자격증 6종",
    url: `${SITE_URL}/cbt-mock-exam`,
    description:
      "SQLD·정처기·컴활·ADsP CBT 모의고사를 무료로 풀어볼 수 있는 플랫폼 페이지.",
    publisher: { "@type": "Organization", name: "문어CBT" },
  };

  const faqLd = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: FAQS.map((f) => ({
      "@type": "Question",
      name: f.q,
      acceptedAnswer: { "@type": "Answer", text: f.a },
    })),
  };

  return (
    <main className="min-h-screen bg-bg text-text">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(webPageLd) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(faqLd) }}
      />

      {/* Hero */}
      <section className="relative overflow-hidden border-b border-border">
        <Container size="default" className="py-20 text-center sm:py-28">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/25 bg-primary/10 px-3.5 py-1.5 text-xs font-medium text-primary">
            🐙 문어CBT · 6종 자격증 무료
          </span>
          <h1 className="mt-6 text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl">
            무료 CBT 모의고사
            <br />
            <span className="bg-gradient-to-r from-primary to-[#5ee0a5] bg-clip-text text-transparent">
              자격증 6종 한 곳에서
            </span>
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-base leading-relaxed text-text-muted sm:text-lg">
            SQLD · 정보처리기사 필기/실기 · 컴퓨터활용능력 1/2급 · ADsP CBT 모의고사를
            실제 시험과 동일한 환경에서 무료로 풀어볼 수 있어요.
          </p>
          <div className="mt-10 flex flex-wrap items-center justify-center gap-2">
            {CERT_LIST.map((cert) => (
              <span
                key={cert.key}
                className={`inline-flex items-center gap-1.5 rounded-full border ${cert.tailwind.border} ${cert.tailwind.bgSoft} px-3 py-1 text-xs font-medium ${cert.tailwind.textSoft}`}
              >
                <span className={`h-1.5 w-1.5 rounded-full ${cert.tailwind.dot}`} />
                {cert.label}
              </span>
            ))}
          </div>
          <div className="mt-10">
            <Link
              href="/mock-exams"
              className="btn-glow inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-primary-fg transition-all hover:bg-primary-hover hover:scale-[1.02]"
            >
              지금 CBT 모의고사 풀기 →
            </Link>
          </div>
        </Container>
      </section>

      {/* 자격증별 CBT 카드 */}
      <section className="border-b border-border">
        <Container size="default" className="py-16">
          <h2 className="text-center text-2xl font-bold tracking-tight sm:text-3xl">
            자격증별 CBT 모의고사
          </h2>
          <p className="mt-3 text-center text-sm text-text-muted">
            카드 클릭 시 해당 자격증 CBT 모의고사로 이동합니다.
          </p>
          <div className="mt-10 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            {CERT_LIST.map((cert) => (
              <Link
                key={cert.key}
                href={`/mock-exams?cert=${cert.key}`}
                className={`group rounded-xl border border-border bg-surface p-6 transition-all hover:scale-[1.02] active:scale-[0.98] ${cert.tailwind.borderHover} ${cert.tailwind.glow}`}
              >
                <div className="flex items-center gap-2">
                  <span className={`h-2 w-2 rounded-full ${cert.tailwind.dot}`} />
                  <span
                    className={`inline-flex items-center rounded border px-2 py-0.5 text-[11px] font-medium ${cert.tailwind.border} ${cert.tailwind.bgSoft} ${cert.tailwind.textSoft}`}
                  >
                    {cert.label}
                  </span>
                </div>
                <h3 className="mt-4 text-lg font-bold">{cert.labelLong}</h3>
                <p className="mt-2 text-sm text-text-muted">
                  {cert.label} CBT 모의고사 · 실전 타이머 · 오답 자동 복습
                </p>
                <p className={`mt-4 text-xs font-medium ${cert.tailwind.textSoft}`}>
                  {cert.label} 모의고사 시작 →
                </p>
              </Link>
            ))}
          </div>
        </Container>
      </section>

      {/* 왜 CBT 모의고사 */}
      <section className="border-b border-border">
        <Container size="default" className="py-16">
          <h2 className="text-center text-2xl font-bold tracking-tight sm:text-3xl">
            왜 CBT 모의고사가 필요한가
          </h2>
          <div className="mt-10 grid grid-cols-1 gap-6 md:grid-cols-3">
            <div className="rounded-xl border border-border bg-surface p-6">
              <p className="text-xs font-semibold uppercase tracking-wider text-primary">
                01
              </p>
              <h3 className="mt-3 text-base font-bold">실전 타이머 압박 적응</h3>
              <p className="mt-2 text-sm text-text-muted">
                실제 CBT 시험에서 화면 타이머는 강한 긴장 요소예요. 모의고사 단계부터
                같은 압박을 경험해야 시험장에서 흔들리지 않습니다.
              </p>
            </div>
            <div className="rounded-xl border border-border bg-surface p-6">
              <p className="text-xs font-semibold uppercase tracking-wider text-primary">
                02
              </p>
              <h3 className="mt-3 text-base font-bold">오답 자동 복습</h3>
              <p className="mt-2 text-sm text-text-muted">
                종이 오답노트와 달리 CBT 환경에서 자동 누적되어 같은 화면·같은 마우스
                조작으로 다시 풀 수 있어요. 감각 유지에 효과적입니다.
              </p>
            </div>
            <div className="rounded-xl border border-border bg-surface p-6">
              <p className="text-xs font-semibold uppercase tracking-wider text-primary">
                03
              </p>
              <h3 className="mt-3 text-base font-bold">회차별 실력 추적</h3>
              <p className="mt-2 text-sm text-text-muted">
                풀 때마다 과목별 정답률이 쌓여 약점이 숫자로 드러나요. 추측이 아니라
                데이터로 학습 방향을 잡을 수 있습니다.
              </p>
            </div>
          </div>
        </Container>
      </section>

      {/* FAQ */}
      <section className="border-b border-border">
        <Container size="default" className="py-16">
          <h2 className="text-center text-2xl font-bold tracking-tight sm:text-3xl">
            자주 묻는 질문
          </h2>
          <div className="mt-10 space-y-4">
            {FAQS.map((f, i) => (
              <details
                key={i}
                className="group rounded-xl border border-border bg-surface p-5 transition-colors hover:border-primary/30"
              >
                <summary className="flex cursor-pointer items-center justify-between text-base font-semibold">
                  {f.q}
                  <span className="ml-4 text-text-muted transition-transform group-open:rotate-180">
                    ▾
                  </span>
                </summary>
                <p className="mt-3 text-sm leading-relaxed text-text-muted">{f.a}</p>
              </details>
            ))}
          </div>
        </Container>
      </section>

      {/* CTA */}
      <section>
        <Container size="default" className="py-20 text-center">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
            지금 바로 CBT 모의고사 시작
          </h2>
          <p className="mt-3 text-sm text-text-muted">
            회원가입 Google 로그인 한 번 · 모든 기능 무료
          </p>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <Link
              href="/mock-exams"
              className="btn-glow inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-primary-fg transition-all hover:bg-primary-hover hover:scale-[1.02]"
            >
              CBT 모의고사 풀기 →
            </Link>
            <Link
              href="/blog/cbt-mock-exam-guide"
              className="inline-flex items-center gap-2 rounded-lg border border-border bg-surface px-5 py-2.5 text-sm font-medium text-text transition-all hover:border-primary/40"
            >
              CBT 모의고사란? 자세히 보기
            </Link>
          </div>
        </Container>
      </section>
    </main>
  );
}
