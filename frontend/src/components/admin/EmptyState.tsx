import type { ReactNode } from "react";

type EmptyStateProps = {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  /** "inline"은 카드 내부에 둘 때. "block"은 독립 섹션. */
  variant?: "inline" | "block";
};

/**
 * 데이터 없음 상태. 테이블/카드 리스트/섹션 모두에서 사용.
 */
export default function EmptyState({
  icon,
  title,
  description,
  action,
  variant = "block",
}: EmptyStateProps) {
  const wrapperClass =
    variant === "block"
      ? "flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-surface/30 px-6 py-16 text-center"
      : "flex flex-col items-center justify-center py-12 text-center";

  return (
    <div className={wrapperClass}>
      {icon && (
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-muted/10 text-muted">
          {icon}
        </div>
      )}
      <p className="text-base font-semibold text-foreground">{title}</p>
      {description && (
        <p className="mt-1.5 max-w-sm text-sm leading-relaxed text-muted">{description}</p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
