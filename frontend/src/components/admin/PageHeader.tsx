import Link from "next/link";
import type { ReactNode } from "react";

type Crumb = { label: string; href?: string };

type PageHeaderProps = {
  title: string;
  description?: string;
  crumbs?: Crumb[];
  /** 우측 액션 영역 (버튼, 필터 등) */
  actions?: ReactNode;
  /** 뒤로가기 경로 (설정 시 왼쪽에 ← 버튼 표시) */
  backHref?: string;
};

/**
 * 어드민 페이지 공통 헤더.
 * - 세로 리듬: title 24px, description 13px
 * - 크럼은 옵션. 존재 시 header 위에 12px 표시
 * - actions 슬롯은 우측 정렬, 여러 개일 경우 flex gap
 */
export default function PageHeader({
  title,
  description,
  crumbs,
  actions,
  backHref,
}: PageHeaderProps) {
  return (
    <header className="mb-6 sm:mb-8">
      {crumbs && crumbs.length > 0 && (
        <nav aria-label="Breadcrumb" className="mb-3 text-xs text-muted">
          <ol className="flex flex-wrap items-center gap-1.5">
            {crumbs.map((c, i) => (
              <li key={i} className="flex items-center gap-1.5">
                {i > 0 && <span className="text-muted/40">/</span>}
                {c.href ? (
                  <Link href={c.href} className="hover:text-foreground transition-colors">
                    {c.label}
                  </Link>
                ) : (
                  <span className="text-foreground/80">{c.label}</span>
                )}
              </li>
            ))}
          </ol>
        </nav>
      )}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 items-start gap-3">
          {backHref && (
            <Link
              href={backHref}
              aria-label="뒤로"
              className="mt-1 inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-md border border-border text-muted transition-colors hover:border-foreground/30 hover:text-foreground"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </Link>
          )}
          <div className="min-w-0">
            <h1 className="text-2xl font-bold tracking-tight sm:text-[26px]">{title}</h1>
            {description && (
              <p className="mt-1 text-sm leading-relaxed text-muted">{description}</p>
            )}
          </div>
        </div>
        {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
      </div>
    </header>
  );
}
