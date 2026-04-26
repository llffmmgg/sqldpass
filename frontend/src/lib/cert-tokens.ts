/**
 * Cert-level design tokens.
 * Single source of truth for the 6 certifications' labels and accent colors.
 * Previously duplicated across mock-exams/solve/wrong-answers/NavBar/blog.
 */

export type CertKey =
  | "SQLD"
  | "ENGINEER_PRACTICAL"
  | "ENGINEER_WRITTEN"
  | "COMPUTER_LITERACY_1"
  | "COMPUTER_LITERACY_2"
  | "ADSP";

export interface CertTailwind {
  /** text color utility (e.g. "text-amber-500") */
  text: string;
  /** soft/subtle text (e.g. "text-amber-400") */
  textSoft: string;
  /** solid background (e.g. "bg-amber-500") */
  bg: string;
  /** low-opacity background for chips (e.g. "bg-amber-500/10") */
  bgSoft: string;
  /** hover background (e.g. "hover:bg-amber-500/[0.08]") */
  bgHover: string;
  /** subtle border (e.g. "border-amber-500/30") */
  border: string;
  /** hover border (e.g. "hover:border-amber-500/50") */
  borderHover: string;
  /** focus/ring (e.g. "ring-amber-500/30") */
  ring: string;
  /** solid dot used as a compact marker (e.g. "bg-amber-400") */
  dot: string;
  /** hover glow shadow (e.g. "hover:shadow-[0_0_20px_rgba(245,158,11,0.18)]") */
  glow: string;
}

export interface CertToken {
  key: CertKey;
  /** short label shown in chips / tabs (e.g. "SQLD", "정처기 실기") */
  label: string;
  /** long descriptive label (e.g. "SQL 개발자 자격증") */
  labelLong: string;
  /** even shorter label for tight spots */
  shortLabel: string;
  /** backend root subject name, used by detectCertFromRootName */
  rootSubjectName: string;
  /** blog category slug (Korean string as used in blog frontmatter) */
  blogCategory: string;
  /** display order in nav / filters */
  order: number;
  tailwind: CertTailwind;
}

export const CERT_TOKENS: Record<CertKey, CertToken> = {
  SQLD: {
    key: "SQLD",
    label: "SQLD",
    labelLong: "SQL 개발자 자격증",
    shortLabel: "SQLD",
    rootSubjectName: "SQLD",
    blogCategory: "SQLD",
    order: 0,
    tailwind: {
      text: "text-amber-500",
      textSoft: "text-amber-400",
      bg: "bg-amber-500",
      bgSoft: "bg-amber-500/10",
      bgHover: "hover:bg-amber-500/[0.08]",
      border: "border-amber-500/30",
      borderHover: "hover:border-amber-500/50",
      ring: "ring-amber-500/30",
      dot: "bg-amber-400",
      glow: "hover:shadow-[0_0_20px_rgba(245,158,11,0.18)]",
    },
  },
  ENGINEER_PRACTICAL: {
    key: "ENGINEER_PRACTICAL",
    label: "정처기 실기",
    labelLong: "정보처리기사 실기",
    shortLabel: "정처기실",
    rootSubjectName: "정보처리기사 실기",
    blogCategory: "정보처리기사",
    order: 1,
    tailwind: {
      text: "text-emerald-500",
      textSoft: "text-emerald-400",
      bg: "bg-emerald-500",
      bgSoft: "bg-emerald-500/10",
      bgHover: "hover:bg-emerald-500/[0.08]",
      border: "border-emerald-500/30",
      borderHover: "hover:border-emerald-500/50",
      ring: "ring-emerald-500/30",
      dot: "bg-emerald-400",
      glow: "hover:shadow-[0_0_20px_rgba(16,185,129,0.18)]",
    },
  },
  ENGINEER_WRITTEN: {
    key: "ENGINEER_WRITTEN",
    label: "정처기 필기",
    labelLong: "정보처리기사 필기",
    shortLabel: "정처기필",
    rootSubjectName: "정보처리기사 필기",
    blogCategory: "정보처리기사 필기",
    order: 2,
    tailwind: {
      text: "text-rose-500",
      textSoft: "text-rose-400",
      bg: "bg-rose-500",
      bgSoft: "bg-rose-500/10",
      bgHover: "hover:bg-rose-500/[0.08]",
      border: "border-rose-500/30",
      borderHover: "hover:border-rose-500/50",
      ring: "ring-rose-500/30",
      dot: "bg-rose-400",
      glow: "hover:shadow-[0_0_20px_rgba(244,63,94,0.18)]",
    },
  },
  COMPUTER_LITERACY_1: {
    key: "COMPUTER_LITERACY_1",
    label: "컴활 1급",
    labelLong: "컴퓨터활용능력 1급 필기",
    shortLabel: "컴활1급",
    rootSubjectName: "컴퓨터활용능력 1급 필기",
    blogCategory: "컴퓨터활용능력",
    order: 3,
    tailwind: {
      text: "text-sky-500",
      textSoft: "text-sky-400",
      bg: "bg-sky-500",
      bgSoft: "bg-sky-500/10",
      bgHover: "hover:bg-sky-500/[0.08]",
      border: "border-sky-500/30",
      borderHover: "hover:border-sky-500/50",
      ring: "ring-sky-500/30",
      dot: "bg-sky-400",
      glow: "hover:shadow-[0_0_20px_rgba(14,165,233,0.18)]",
    },
  },
  COMPUTER_LITERACY_2: {
    key: "COMPUTER_LITERACY_2",
    label: "컴활 2급",
    labelLong: "컴퓨터활용능력 2급 필기",
    shortLabel: "컴활2급",
    rootSubjectName: "컴퓨터활용능력 2급 필기",
    blogCategory: "컴퓨터활용능력",
    order: 4,
    tailwind: {
      text: "text-indigo-500",
      textSoft: "text-indigo-400",
      bg: "bg-indigo-500",
      bgSoft: "bg-indigo-500/10",
      bgHover: "hover:bg-indigo-500/[0.08]",
      border: "border-indigo-500/30",
      borderHover: "hover:border-indigo-500/50",
      ring: "ring-indigo-500/30",
      dot: "bg-indigo-400",
      glow: "hover:shadow-[0_0_20px_rgba(99,102,241,0.18)]",
    },
  },
  ADSP: {
    key: "ADSP",
    label: "ADsP",
    labelLong: "데이터분석 준전문가(ADsP)",
    shortLabel: "ADsP",
    rootSubjectName: "데이터분석 준전문가(ADsP)",
    blogCategory: "ADsP",
    order: 5,
    tailwind: {
      text: "text-teal-500",
      textSoft: "text-teal-400",
      bg: "bg-teal-500",
      bgSoft: "bg-teal-500/10",
      bgHover: "hover:bg-teal-500/[0.08]",
      border: "border-teal-500/30",
      borderHover: "hover:border-teal-500/50",
      ring: "ring-teal-500/30",
      dot: "bg-teal-400",
      glow: "hover:shadow-[0_0_20px_rgba(20,184,166,0.18)]",
    },
  },
};

/** Ordered list for rendering tabs / chips. */
export const CERT_LIST: CertToken[] = Object.values(CERT_TOKENS).sort(
  (a, b) => a.order - b.order,
);

const ROOT_NAME_TO_CERT: Record<string, CertKey> = {
  "정보처리기사 실기": "ENGINEER_PRACTICAL",
  "정보처리기사 필기": "ENGINEER_WRITTEN",
  "컴퓨터활용능력 1급 필기": "COMPUTER_LITERACY_1",
  "컴퓨터활용능력 2급 필기": "COMPUTER_LITERACY_2",
  "데이터분석 준전문가(ADsP)": "ADSP",
  SQLD: "SQLD",
};

/**
 * Resolve a cert from the backend root-subject name.
 * Falls back to SQLD for legacy data.
 */
export function certFromRootName(rootName: string): CertKey {
  return ROOT_NAME_TO_CERT[rootName] ?? "SQLD";
}

/**
 * Backend ExamType string → CertKey. ExamType uses the same enum keys.
 */
export function certFromExamType(examType: string | null | undefined): CertKey | null {
  if (!examType) return null;
  return examType in CERT_TOKENS ? (examType as CertKey) : null;
}

/**
 * Resolve a CertKey from a blog category string.
 * Multiple certs can map to the same category (e.g. both 컴활 1급/2급 use "컴퓨터활용능력").
 * This returns the first matching cert in order.
 */
export function certFromBlogCategory(category: string): CertKey | null {
  for (const token of CERT_LIST) {
    if (token.blogCategory === category) return token.key;
  }
  return null;
}

const SLUG_TO_CERT: Record<string, CertKey> = {
  sqld: "SQLD",
  engineer: "ENGINEER_PRACTICAL",
  "engineer-written": "ENGINEER_WRITTEN",
  "computer-literacy-1": "COMPUTER_LITERACY_1",
  "computer-literacy-2": "COMPUTER_LITERACY_2",
  adsp: "ADSP",
};

/** /learn/[cert] 경로의 slug → CertKey 매핑 */
export function certFromSlug(slug: string): CertKey | null {
  return SLUG_TO_CERT[slug] ?? null;
}

const CERT_TO_SLUG: Record<CertKey, string> = {
  SQLD: "sqld",
  ENGINEER_PRACTICAL: "engineer",
  ENGINEER_WRITTEN: "engineer-written",
  COMPUTER_LITERACY_1: "computer-literacy-1",
  COMPUTER_LITERACY_2: "computer-literacy-2",
  ADSP: "adsp",
};

export function slugFromCert(cert: CertKey): string {
  return CERT_TO_SLUG[cert];
}
