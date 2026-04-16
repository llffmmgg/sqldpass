import type { ReactNode, ThHTMLAttributes } from "react";

type DataTableProps = {
  children: ReactNode;
  /** 밑줄형 헤더 vs 배경형 헤더 */
  variant?: "surface" | "plain";
  /** 최대 높이(스크롤 시 sticky 헤더 작동). 지정 안 하면 자연 높이 */
  maxHeight?: string;
};

/**
 * 어드민 공통 테이블 래퍼.
 * - overflow-x-auto로 모바일 가로 스크롤
 * - thead에 sticky top-0 자동 적용
 * - rounded 컨테이너로 표 외곽 깔끔하게
 *
 * 사용법:
 * <DataTable>
 *   <DataTable.Head>
 *     <DataTable.HeadCell>이름</DataTable.HeadCell>
 *   </DataTable.Head>
 *   <tbody>
 *     <DataTable.Row>...</DataTable.Row>
 *   </tbody>
 * </DataTable>
 */
export default function DataTable({
  children,
  variant = "surface",
  maxHeight,
}: DataTableProps) {
  return (
    <div
      className="overflow-x-auto rounded-xl border border-border bg-surface/40"
      style={maxHeight ? { maxHeight, overflowY: "auto" } : undefined}
    >
      <table className={`w-full text-sm ${variant === "surface" ? "[&_thead_tr]:bg-surface/80" : ""}`}>
        {children}
      </table>
    </div>
  );
}

function Head({ children }: { children: ReactNode }) {
  return (
    <thead className="sticky top-0 z-10 [&_tr]:border-b [&_tr]:border-border">
      <tr>{children}</tr>
    </thead>
  );
}

type HeadCellProps = ThHTMLAttributes<HTMLTableCellElement> & {
  align?: "left" | "right" | "center";
  /** 정렬 가능 컬럼일 때 지정. active면 방향 표시. */
  sortable?: {
    active: boolean;
    direction: "asc" | "desc";
    onToggle: () => void;
  };
};

function HeadCell({
  className = "",
  align = "left",
  sortable,
  children,
  ...rest
}: HeadCellProps) {
  const alignClass =
    align === "right" ? "text-right" : align === "center" ? "text-center" : "text-left";
  const base = `px-4 py-3 text-[11px] font-semibold uppercase tracking-wider text-muted ${alignClass} ${className}`;
  if (!sortable) {
    return (
      <th {...rest} className={base}>
        {children}
      </th>
    );
  }
  const { active, direction, onToggle } = sortable;
  return (
    <th {...rest} className={base}>
      <button
        type="button"
        onClick={onToggle}
        aria-sort={active ? (direction === "asc" ? "ascending" : "descending") : "none"}
        className={`inline-flex items-center gap-1 transition-colors ${
          active ? "text-foreground" : "hover:text-foreground"
        } ${align === "right" ? "ml-auto" : ""}`}
      >
        <span>{children}</span>
        <SortIcon active={active} direction={direction} />
      </button>
    </th>
  );
}

function SortIcon({ active, direction }: { active: boolean; direction: "asc" | "desc" }) {
  if (!active) {
    return (
      <svg className="h-3 w-3 text-muted/40" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M8 9l4-4 4 4m-8 6l4 4 4-4" />
      </svg>
    );
  }
  return direction === "asc" ? (
    <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 15l7-7 7 7" />
    </svg>
  ) : (
    <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
    </svg>
  );
}

function Row({ children, className = "", ...rest }: React.HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      {...rest}
      className={`border-b border-border/60 last:border-none transition-colors hover:bg-surface/80 ${className}`}
    >
      {children}
    </tr>
  );
}

type CellProps = React.TdHTMLAttributes<HTMLTableCellElement> & {
  align?: "left" | "right" | "center";
  mono?: boolean;
};

function Cell({ className = "", align = "left", mono = false, children, ...rest }: CellProps) {
  const alignClass =
    align === "right" ? "text-right" : align === "center" ? "text-center" : "text-left";
  const numClass = mono ? "tabular-nums" : "";
  return (
    <td
      {...rest}
      className={`px-4 py-3 ${alignClass} ${numClass} ${className}`}
    >
      {children}
    </td>
  );
}

/** 로딩 중 테이블 행을 대체하는 skeleton. cols 수와 rows 수 지정. */
export function TableSkeleton({
  cols,
  rows = 6,
}: {
  cols: number;
  rows?: number;
}) {
  return (
    <tbody aria-hidden="true">
      {Array.from({ length: rows }).map((_, r) => (
        <tr key={r} className="border-b border-border/60 last:border-none">
          {Array.from({ length: cols }).map((_, c) => (
            <td key={c} className="px-4 py-3">
              <div className="h-3 animate-pulse rounded bg-border/60" style={{ width: `${40 + ((c * 13 + r * 7) % 50)}%` }} />
            </td>
          ))}
        </tr>
      ))}
    </tbody>
  );
}

DataTable.Head = Head;
DataTable.HeadCell = HeadCell;
DataTable.Row = Row;
DataTable.Cell = Cell;
