import type { Metadata } from "next";
import Link from "next/link";
import { Container } from "@/components/ui";

export const metadata: Metadata = {
  title: "개인정보처리방침",
  description: "문어CBT가 수집·이용하는 개인정보의 종류와 처리 방침입니다.",
  alternates: { canonical: "/privacy" },
};

export default function PrivacyPage() {
  return (
    <main className="py-12 text-foreground">
      <Container size="narrow">
      <h1 className="text-3xl font-bold">개인정보처리방침</h1>
      <p className="mt-2 text-sm text-muted">최종 개정일: 2026년 5월 6일</p>

      <Section title="1. 수집하는 정보">
        <p>문어CBT(이하 &quot;사이트&quot;)는 다음 정보를 수집합니다.</p>
        <ul className="mt-2 list-disc pl-5">
          <li>
            <strong>회원가입 정보</strong> — Google OAuth 로그인을 통해 제공받는 닉네임, 프로필
            식별자(provider ID). 비밀번호는 저장하지 않습니다.
          </li>
          <li>
            <strong>학습 기록</strong> — 풀이한 문제 ID, 선택한 답, 정·오답 결과, 풀이 시각.
            오답노트와 통계 제공을 위한 최소 정보입니다.
          </li>
          <li>
            <strong>피드백</strong> — 사용자가 직접 작성해 보낸 문제 신고·기능 제안 내용.
          </li>
          <li>
            <strong>접속 정보</strong> — IP, User-Agent, 접속 경로 등 일반적인 웹 로그.
          </li>
          <li>
            <strong>결제 정보</strong> — 유료 모의고사 결제 시 PortOne 으로부터 전달받은 결제
            식별자(paymentId), 결제 상태, 결제 금액, 결제 시각, 잠금 해제 대상 회차 ID. 카드 번호·
            CVC·유효기간 등 카드 정보는 사이트가 직접 수집·저장하지 않습니다.
          </li>
        </ul>
      </Section>

      <Section title="2. 이용 목적">
        <ul className="list-disc pl-5">
          <li>학습 진척 추적, 오답노트, 모의고사 채점 및 통계 제공</li>
          <li>서비스 개선과 문제 품질 관리</li>
          <li>이용자 문의·피드백 응대</li>
        </ul>
      </Section>

      <Section title="3. 제3자 서비스 / 쿠키">
        <p>사이트는 다음 외부 서비스를 이용합니다.</p>
        <ul className="mt-2 list-disc pl-5">
          <li>
            <strong>Google Analytics 4</strong> — 익명화된 트래픽 분석. 페이지 조회·체류 시간
            측정용 쿠키를 사용합니다. 옵트아웃은{" "}
            <a
              href="https://tools.google.com/dlpage/gaoptout"
              className="text-primary underline"
              target="_blank"
              rel="noopener noreferrer"
            >
              Google Analytics Opt-out
            </a>
            에서 가능합니다.
          </li>
          <li>
            <strong>Google AdSense</strong> (도입 예정) — 사이트 운영비 충당을 위한 광고를
            표시합니다. AdSense는 관심 기반 광고를 위해 쿠키를 사용할 수 있으며, 이용자는{" "}
            <a
              href="https://adssettings.google.com"
              className="text-primary underline"
              target="_blank"
              rel="noopener noreferrer"
            >
              Google 광고 설정
            </a>
            에서 개인 맞춤 광고를 비활성화할 수 있습니다.
          </li>
          <li>
            <strong>Google OAuth</strong> — 로그인 인증.
          </li>
          <li>
            <strong>AI API (Anthropic Claude / Google Gemini)</strong> — 문제 생성과 검증.
            이 호출에는 사용자 식별 정보가 포함되지 않습니다.
          </li>
          <li>
            <strong>PortOne (코리아포트원)</strong> — 유료 모의고사 결제 처리. 결제창 호출과
            결제 검증 단계에서 paymentId·금액·상태가 송수신되며, 카드 정보는 PortOne 및 카드사
            구간에서만 처리됩니다.
          </li>
        </ul>
        <p className="mt-3">
          브라우저 설정에서 쿠키를 차단할 수 있으나, 차단 시 일부 기능(로그인 등)이 제한될 수
          있습니다.
        </p>
      </Section>

      <Section title="4. 보관 기간">
        <p>
          회원이 탈퇴를 요청하면 학습 기록·피드백을 포함한 개인 식별 가능 데이터를 즉시
          파기합니다. 통계 목적의 익명화된 집계 데이터는 보관될 수 있습니다.
        </p>
        <p className="mt-2">
          단, 결제 정보(paymentId · 금액 · 결제 시각)는 전자상거래 등에서의 소비자보호에 관한
          법률에 따라 거래 완료 시점부터 5년간 보관 후 파기합니다.
        </p>
      </Section>

      <Section title="5. 이용자 권리">
        <p>
          이용자는 본인 정보의 열람·수정·삭제를 요청할 수 있습니다. 마이페이지의 피드백 폼 또는
          아래 연락처로 요청해 주세요.
        </p>
      </Section>

      <Section title="6. 개인정보처리책임자">
        <p>
          사이트는 이용자의 개인정보를 보호하고 관련 문의·요청을 처리하기 위해 다음과 같이
          개인정보처리책임자를 지정합니다.
        </p>
        <ul className="mt-2 list-disc pl-5">
          <li>책임자: 정희훈 (대표)</li>
          <li>
            연락처:{" "}
            <a className="underline" href="mailto:ssomker.dev@gmail.com">
              ssomker.dev@gmail.com
            </a>
          </li>
          <li>소속: 에스큐엘디패스 (사업자등록번호 443-41-01548)</li>
        </ul>
        <p className="mt-2">
          개인정보 열람·수정·삭제·처리정지 요청은 위 연락처로 보내주시면 지체 없이 처리합니다.
        </p>
      </Section>

      <Section title="7. 연락처">
        <p>
          문의는 사이트 내 <Link href="/profile" className="text-primary underline">피드백</Link>{" "}
          기능을 통해 보내주시면 가장 빠르게 확인됩니다.
        </p>
      </Section>

      <Section title="8. 개정 이력">
        <ul className="list-disc pl-5">
          <li>2026-05-07 — 개인정보처리책임자 항목 추가</li>
          <li>2026-05-06 — 결제 정보 수집·PortOne 연동·보관 기간 조항 추가</li>
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
