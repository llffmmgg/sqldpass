/**
 * 현재 지원 중인 자격증 칩 — 히어로/푸터 등에서 재사용.
 * 새 자격증 추가 시 여기 한 곳만 수정하면 랜딩에 자동 반영.
 */

const CERTS = [
  { id: "sqld", label: "SQLD" },
  { id: "engineer", label: "정보처리기사 실기" },
  { id: "computer-literacy", label: "컴퓨터활용능력 1급" },
] as const;

export default function CertChips() {
  return (
    <div className="flex flex-wrap items-center justify-center gap-2">
      {CERTS.map((c) => (
        <span
          key={c.id}
          className="inline-flex items-center rounded-full border border-border/80 bg-surface/60 px-3 py-1 text-xs font-medium text-foreground/80 backdrop-blur-sm"
        >
          {c.label}
        </span>
      ))}
      <span className="inline-flex items-center rounded-full border border-dashed border-border/50 px-3 py-1 text-xs text-muted/60">
        + 추가 예정
      </span>
    </div>
  );
}
