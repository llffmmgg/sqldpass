import Link from "next/link";

export default function Footer() {
  return (
    <footer className="mt-16 border-t border-border bg-surface/30">
      <div className="mx-auto max-w-6xl px-4 py-8 text-sm text-muted">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="font-semibold text-foreground">sqldpass</p>
            <p className="mt-1 text-xs">
              SQL 개발자(SQLD) · 정보처리기사 실기 · 컴퓨터활용능력 1급 필기 무료 CBT 모의고사
            </p>
          </div>
          <nav className="flex flex-wrap items-center gap-x-4 gap-y-2 text-xs">
            <Link href="/about" className="transition-colors hover:text-foreground">
              소개
            </Link>
            <Link href="/privacy" className="transition-colors hover:text-foreground">
              개인정보처리방침
            </Link>
            <Link href="/terms" className="transition-colors hover:text-foreground">
              이용약관
            </Link>
            <Link href="/profile" className="transition-colors hover:text-foreground">
              피드백
            </Link>
          </nav>
        </div>
        <p className="mt-6 text-xs text-muted/70">© {new Date().getFullYear()} sqldpass</p>
      </div>
    </footer>
  );
}
