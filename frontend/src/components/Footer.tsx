import Link from "next/link";

const PRODUCT_LINKS = [
  { href: "/solve", label: "문제 풀기" },
  { href: "/mock-exams", label: "모의고사" },
  { href: "/wrong-answers", label: "오답 노트" },
  { href: "/dashboard", label: "대시보드" },
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
    <footer className="mt-24 border-t border-border bg-bg-elevated">
      <div className="mx-auto w-full max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-8 sm:grid-cols-4">
          <div className="col-span-2 sm:col-span-1">
            <p className="font-mono text-base font-bold tracking-tight text-text">
              🐙 문어<span className="text-primary">CBT</span>
            </p>
            <p className="mt-1 text-xs font-medium text-text-muted">
              CBT 자격증 모의고사 플랫폼
            </p>
            <p className="mt-2 text-xs leading-relaxed text-text-subtle">
              SQLD · 정보처리기사 · 컴퓨터활용능력 · ADsP 6종 자격증
            </p>
          </div>
          <Column title="제품" links={PRODUCT_LINKS} />
          <Column title="회사" links={COMPANY_LINKS} />
          <Column title="정책" links={POLICY_LINKS} />
        </div>

        <div className="mt-10 border-t border-border pt-6">
          <p className="text-xs text-text-muted">
            배너 광고 문의 및 비즈니스 제안 ·{" "}
            <a
              href="mailto:ssomker.dev@gmail.com"
              className="text-text transition-colors hover:text-primary"
            >
              ssomker.dev@gmail.com
            </a>
          </p>
          <p className="mt-2 text-xs text-text-subtle">
            © {new Date().getFullYear()} 문어CBT · sqldpass.com
          </p>
        </div>
      </div>
    </footer>
  );
}
