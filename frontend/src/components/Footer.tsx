import Link from "next/link";

const PRODUCT_LINKS = [
  { href: "/solve", label: "문제 풀기" },
  { href: "/mock-exams", label: "모의고사" },
  { href: "/wrong-answers", label: "오답 노트" },
  { href: "/dashboard", label: "대시보드" },
  { href: "/checkout", label: "이용권 구매" },
  { href: "/blog", label: "시험 준비 팁" },
  { href: "/changelog", label: "업데이트 내역" },
];

const COMPANY_LINKS = [
  { href: "/about", label: "소개" },
  { href: "/profile", label: "피드백" },
];

const POLICY_LINKS = [
  { href: "/privacy", label: "개인정보처리방침" },
  { href: "/terms", label: "이용약관" },
  { href: "/refund", label: "환불 정책" },
];

function Column({ title, links }: { title: string; links: { href: string; label: string }[] }) {
  return (
    <div>
      <h3 className="text-[11px] font-semibold uppercase tracking-wider text-text-subtle">
        {title}
      </h3>
      <ul className="mt-3 space-y-2">
        {links.map((l) => (
          <li key={l.href}>
            <Link
              href={l.href}
              className="text-sm text-text-muted transition-colors hover:text-text"
            >
              {l.label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function Footer() {
  return (
    <footer
      role="contentinfo"
      aria-label="사이트 푸터"
      className="mt-24 border-t border-border bg-bg-elevated"
    >
      <div className="mx-auto w-full max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-8 sm:grid-cols-4">
          <div className="col-span-2 sm:col-span-1">
            <p className="text-base font-bold tracking-tight text-text">
              문어<span className="font-mono text-primary">CBT</span>
            </p>
            <p className="mt-1 text-xs font-medium text-text-muted">
              CBT 모의고사 플랫폼
            </p>
            <p className="mt-2 text-xs leading-relaxed text-text-subtle">
              SQLD · 정보처리기사 · 컴퓨터활용능력 · ADsP
            </p>
          </div>
          <Column title="제품" links={PRODUCT_LINKS} />
          <Column title="회사" links={COMPANY_LINKS} />
          <Column title="정책" links={POLICY_LINKS} />
        </div>

        <div data-nosnippet className="mt-10 border-t border-border pt-6">
          <p className="text-xs text-text-muted">
            배너 광고 문의 및 비즈니스 제안 ·{" "}
            <a
              href="mailto:ssomker.dev@gmail.com"
              className="text-text transition-colors hover:text-primary"
            >
              ssomker.dev@gmail.com
            </a>
          </p>
          {/* 사업자 정보 — 전자상거래법 13조 + PG 심사를 위해 항상 노출 (이전 details 접힘 제거) */}
          <dl className="mt-4 grid grid-cols-1 gap-x-6 gap-y-1 text-[11px] leading-relaxed text-text-subtle sm:grid-cols-2">
            <div className="flex gap-2">
              <dt className="shrink-0 text-text-muted">상호</dt>
              <dd>에스큐엘디패스</dd>
            </div>
            <div className="flex gap-2">
              <dt className="shrink-0 text-text-muted">대표자</dt>
              <dd>정희훈</dd>
            </div>
            <div className="flex gap-2">
              <dt className="shrink-0 text-text-muted">사업자등록번호</dt>
              <dd>443-41-01548</dd>
            </div>
            <div className="flex gap-2">
              <dt className="shrink-0 text-text-muted">통신판매업 신고</dt>
              <dd>제2026-서울서대문-0466호</dd>
            </div>
            <div className="flex gap-2 sm:col-span-2">
              <dt className="shrink-0 text-text-muted">사업장 주소</dt>
              <dd>서울특별시 서대문구 독립문로10길 2, 101동 1801호 (천연동)</dd>
            </div>
            <div className="flex gap-2">
              <dt className="shrink-0 text-text-muted">대표번호</dt>
              <dd>
                <a
                  href="tel:+821046393411"
                  className="transition-colors hover:text-text"
                >
                  010-4639-3411
                </a>
              </dd>
            </div>
            <div className="flex gap-2">
              <dt className="shrink-0 text-text-muted">개인정보책임자</dt>
              <dd>정희훈</dd>
            </div>
          </dl>
          <p className="mt-4 text-xs text-text-subtle">
            © {new Date().getFullYear()} 에스큐엘디패스 · sqldpass.com
          </p>
        </div>
      </div>
    </footer>
  );
}
