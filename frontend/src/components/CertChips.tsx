/**
 * 현재 지원 중인 자격증 — 히어로/푸터 등에서 재사용.
 * Vercel·Linear 풍 트러스트바 스타일: 카드/배경 없이 모노 회색 텍스트만
 * 가로로 균등 배치하고 아래 캡션 한 줄.
 * 새 자격증 추가 시 CERTS 배열만 수정하면 랜딩에 자동 반영.
 */

const CERTS = [
  { id: "sqld", label: "SQLD" },
  { id: "engineer", label: "정보처리기사 실기" },
  { id: "engineer-written", label: "정보처리기사 필기" },
  { id: "computer-literacy-1", label: "컴퓨터활용능력 1급" },
  { id: "computer-literacy-2", label: "컴퓨터활용능력 2급" },
  { id: "adsp", label: "ADsP" },
] as const;

export default function CertChips() {
  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex flex-wrap items-center justify-center gap-x-8 gap-y-3 sm:gap-x-10 md:gap-x-14">
        {CERTS.map((c) => (
          <span
            key={c.id}
            className="text-sm font-semibold tracking-tight text-text-subtle transition-colors hover:text-text-muted sm:text-base"
          >
            {c.label}
          </span>
        ))}
      </div>
      <p className="mt-5 text-center text-xs text-text-subtle">
        현재 지원 중인 6개 자격증 CBT 모의고사
      </p>
    </div>
  );
}
