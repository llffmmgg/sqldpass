import type { Metadata } from "next";
import Link from "next/link";
import { Container } from "@/components/ui";

export const metadata: Metadata = {
  title: "환불 정책",
  description: "문어CBT 유료 모의고사 환불·취소 정책입니다.",
  alternates: { canonical: "/refund" },
};

export default function RefundPage() {
  return (
    <main className="py-12 text-foreground">
      <Container size="narrow">
        <h1 className="text-3xl font-bold">환불 정책</h1>
        <p className="mt-2 text-sm text-muted">최종 개정일: 2026년 5월 6일</p>

        <Section title="1. 환불 대상">
          <p>
            문어CBT가 제공하는 유료 프리미엄 모의고사 결제건에 한해 본 환불 정책이 적용됩니다.
            결제는 PortOne(주식회사 코리아포트원) PG 결제 모듈을 통해 처리됩니다.
          </p>
        </Section>

        <Section title="2. 환불 가능 조건">
          <ul className="list-disc pl-5">
            <li>
              결제 후 7일 이내, <strong>해당 모의고사를 단 한 문제도 풀지 않고 PDF 도 하나도 다운로드하지 않은 경우</strong>{" "}
              전액 환불 가능합니다.
            </li>
            <li>
              사이트 측 귀책(서비스 장애·결제 오작동·컨텐츠 미제공 등) 으로 정상 이용이 불가했던
              경우 전액 환불됩니다.
            </li>
          </ul>
        </Section>

        <Section title="3. 환불 불가 조건">
          <ul className="list-disc pl-5">
            <li>해당 모의고사의 1문제 이상을 응시·채점한 경우 (디지털 컨텐츠 특성)</li>
            <li>결제 후 7일이 경과한 경우</li>
            <li>
              이용자가 약관에 위반되는 행위(자동화 봇·악의적 다중 결제 등)로 결제한 경우
            </li>
          </ul>
        </Section>

        <Section title="4. 환불 신청 방법">
          <p>
            사이트의{" "}
            <Link href="/profile" className="text-primary underline">
              피드백
            </Link>
            {" "}또는 대표 이메일(<a className="underline" href="mailto:ssomker.dev@gmail.com">ssomker.dev@gmail.com</a>) 로
            아래 정보를 보내주세요.
          </p>
          <ul className="mt-2 list-disc pl-5">
            <li>주문자 닉네임 · 결제 일자 · 결제 금액</li>
            <li>환불 요청 사유</li>
          </ul>
          <p className="mt-2">
            접수 후 영업일 기준 3일 이내 검토 결과를 회신하며, 승인 시 PortOne 결제 취소 API 를
            통해 결제 수단으로 환불 처리합니다. 환불 완료까지 카드사 정책에 따라 최대 7영업일이
            소요될 수 있습니다.
          </p>
        </Section>

        <Section title="5. 책임 한정">
          <p>
            환불 처리는 본 정책 범위 내에서 이루어지며, 환불로 인한 카드 포인트·할인 혜택 회수는
            각 카드사 정책을 따릅니다.
          </p>
        </Section>

        <Section title="6. 개정 이력">
          <ul className="list-disc pl-5">
            <li>2026-05-06 — 최초 작성</li>
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
