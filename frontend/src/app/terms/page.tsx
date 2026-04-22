import type { Metadata } from "next";
import Link from "next/link";
import { Container } from "@/components/ui";

export const metadata: Metadata = {
  title: "이용약관",
  description: "sqldpass 서비스 이용약관입니다.",
  alternates: { canonical: "/terms" },
};

export default function TermsPage() {
  return (
    <main className="py-12 text-foreground">
      <Container size="narrow">
      <h1 className="text-3xl font-bold">이용약관</h1>
      <p className="mt-2 text-sm text-muted">최종 개정일: 2026년 4월 9일</p>

      <Section title="1. 목적">
        <p>
          본 약관은 sqldpass(이하 &quot;사이트&quot;)가 제공하는 SQL 개발자(SQLD), 정보처리기사
          실기, 컴퓨터활용능력 1급 필기 학습 지원 서비스의 이용 조건을 규정합니다.
        </p>
      </Section>

      <Section title="2. 컨텐츠 정확성과 면책">
        <p>
          사이트의 문제와 해설은 AI(Claude, Gemini 등)로 자동 생성되며, 운영자가 사후 검증을 진행
          중입니다. 그럼에도 일부 문제에는 오류가 포함될 수 있으며, 사이트는 다음을 보증하지
          않습니다.
        </p>
        <ul className="mt-2 list-disc pl-5">
          <li>모든 문제와 해설의 절대적 정확성</li>
          <li>실제 시험 합격 또는 점수</li>
          <li>실제 시험 출제 범위와의 완전 일치</li>
        </ul>
        <p className="mt-2">
          이용자는 학습 보조 자료로 활용하시고, 정확한 시험 범위·정답은 공식 출제기관의 자료를
          병행해 확인해 주세요. 오류 발견 시{" "}
          <Link href="/profile" className="text-primary underline">피드백</Link>으로 알려주시면
          신속히 수정합니다.
        </p>
      </Section>

      <Section title="3. 무료 서비스 / 광고">
        <p>
          사이트는 기본 기능을 무료로 제공합니다. 운영비(서버·AI API) 충당을 위해 광고가 표시될
          수 있으며, 이는 학습 흐름을 방해하지 않는 위치에만 배치됩니다.
        </p>
      </Section>

      <Section title="4. 이용자 의무">
        <ul className="list-disc pl-5">
          <li>정상적 학습 목적으로만 서비스를 이용</li>
          <li>자동화 봇·스크립트로 무리한 요청 금지</li>
          <li>사이트 컨텐츠를 무단으로 복제·배포하지 않음</li>
          <li>다른 이용자나 운영자에 대한 비방·괴롭힘 금지</li>
        </ul>
      </Section>

      <Section title="5. 계정">
        <p>
          로그인은 Google OAuth를 통해 이루어집니다. 이용자는 언제든 탈퇴할 수 있으며, 탈퇴 시
          학습 기록은 개인정보처리방침에 따라 삭제됩니다.
        </p>
      </Section>

      <Section title="6. 저작권">
        <p>
          사이트가 생성·배포하는 모든 문제, 해설, 코드 예제, UI는 sqldpass에 귀속됩니다. 학습
          목적의 개인적 사용은 자유이나, 무단 복제·재배포·상업적 이용은 금지됩니다.
        </p>
      </Section>

      <Section title="7. 서비스 변경 및 중단">
        <p>
          사이트는 서비스 개선을 위해 기능을 추가·변경할 수 있으며, 운영상 필요한 경우 사전 공지
          후 일부 또는 전체 서비스를 중단할 수 있습니다.
        </p>
      </Section>

      <Section title="8. 책임의 한정">
        <p>
          사이트는 무료 학습 보조 도구로 제공되며, 이용으로 인해 발생한 직간접적 손해(시험 결과,
          잘못된 정보로 인한 학습 손실 등)에 대해 법이 허용하는 최대 범위에서 책임을 지지
          않습니다.
        </p>
      </Section>

      <Section title="9. 분쟁 해결">
        <p>
          본 약관은 대한민국 법령에 따라 해석되며, 분쟁 발생 시 운영자 주소지의 관할 법원을
          전속 관할로 합니다.
        </p>
      </Section>

      <Section title="10. 개정 이력">
        <ul className="list-disc pl-5">
          <li>2026-04-09 — 최초 작성</li>
        </ul>
      </Section>
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
