import type { BlogPostMeta } from "@/lib/blog";

/**
 * 블로그 카테고리 공통 메타.
 * 허브(/blog)와 카테고리 페이지(/blog/category/[...])에서 같은 소스를 공유.
 */
export type CategoryMeta = {
  /** frontmatter의 category 값 (DB 키) */
  name: string;
  /** URL slug — 한글 그대로 사용 */
  slug: string;
  /** 화면 표시용 이름 */
  label: string;
  /** 카테고리 소개 한 줄 */
  description: string;
  /** 카드 배경 그라데이션 클래스 */
  gradient: string;
  /** 카드 테두리 클래스 */
  border: string;
  /** 아이콘 타일 배경 */
  iconBg: string;
  /** 이모지 (Hero·카드 아이콘) */
  emoji: string;
  /** 텍스트/포커스 강조 색상 */
  text: string;
  /** 뱃지 배경+텍스트 (chip/featured 배지) */
  badge: string;
  /** 색상 강조 bar (Featured 상단 바) */
  bar: string;
};

const FALLBACK: CategoryMeta = {
  name: "",
  slug: "",
  label: "",
  description: "",
  gradient: "from-muted/10 to-muted/5",
  border: "border-border hover:border-foreground/30",
  iconBg: "bg-muted/10",
  emoji: "📄",
  text: "text-foreground",
  badge: "bg-muted/10 text-muted border-muted/30",
  bar: "bg-muted",
};

export const CATEGORIES: CategoryMeta[] = [
  {
    name: "SQLD",
    slug: "SQLD",
    label: "SQLD",
    description: "SQL 개발자 자격증 공부법, 핵심 개념, 합격률, 시험일정",
    gradient: "from-amber-500/20 to-orange-500/10",
    border: "border-amber-500/30 hover:border-amber-400/60",
    iconBg: "bg-amber-500/15",
    emoji: "🗃️",
    text: "text-primary",
    badge: "bg-primary/10 text-primary border-primary/30",
    bar: "bg-primary",
  },
  {
    name: "정보처리기사",
    slug: "정보처리기사",
    label: "정보처리기사 실기",
    description: "정처기 실기 출제 경향, 코드 문제 풀이, 암기 과목 정리",
    gradient: "from-violet-500/20 to-purple-500/10",
    border: "border-violet-500/30 hover:border-violet-400/60",
    iconBg: "bg-violet-500/15",
    emoji: "💻",
    text: "text-accent",
    badge: "bg-accent/10 text-accent border-accent/30",
    bar: "bg-accent",
  },
  {
    name: "정보처리기사 필기",
    slug: "정보처리기사 필기",
    label: "정보처리기사 필기",
    description: "정처기 필기 5과목 공부법, 핵심 개념 요약, 합격률 분석",
    gradient: "from-purple-500/20 to-fuchsia-500/10",
    border: "border-purple-500/30 hover:border-purple-400/60",
    iconBg: "bg-purple-500/15",
    emoji: "📝",
    text: "text-purple-500",
    badge: "bg-purple-500/10 text-purple-500 border-purple-500/30",
    bar: "bg-purple-500",
  },
  {
    name: "컴퓨터활용능력",
    slug: "컴퓨터활용능력",
    label: "컴퓨터활용능력 1급",
    description: "컴활 1급 필기 벼락치기, 실기 대비, 합격률 분석",
    gradient: "from-sky-500/20 to-blue-500/10",
    border: "border-sky-500/30 hover:border-sky-400/60",
    iconBg: "bg-sky-500/15",
    emoji: "📊",
    text: "text-blue-600",
    badge: "bg-blue-600/10 text-blue-600 border-blue-600/30",
    bar: "bg-blue-600",
  },
  {
    name: "컴퓨터활용능력 2급",
    slug: "컴퓨터활용능력 2급",
    label: "컴퓨터활용능력 2급",
    description: "컴활 2급 필기 공부법, 핵심 개념, 합격률, 시험일정",
    gradient: "from-indigo-500/20 to-blue-500/10",
    border: "border-indigo-500/30 hover:border-indigo-400/60",
    iconBg: "bg-indigo-500/15",
    emoji: "📋",
    text: "text-indigo-500",
    badge: "bg-indigo-500/10 text-indigo-500 border-indigo-500/30",
    bar: "bg-indigo-500",
  },
  {
    name: "ADsP",
    slug: "ADsP",
    label: "데이터분석 준전문가(ADsP)",
    description: "ADsP 공부법, 핵심 개념 요약, 합격률, 2024 개편 대응",
    gradient: "from-teal-500/20 to-cyan-500/10",
    border: "border-teal-500/30 hover:border-teal-400/60",
    iconBg: "bg-teal-500/15",
    emoji: "📈",
    text: "text-teal-500",
    badge: "bg-teal-500/10 text-teal-500 border-teal-500/30",
    bar: "bg-teal-500",
  },
  {
    name: "일반",
    slug: "일반",
    label: "시험 팁",
    description: "자격증 비교, 시험 당일 꿀팁, CBT 모의고사 활용법",
    gradient: "from-emerald-500/20 to-teal-500/10",
    border: "border-emerald-500/30 hover:border-emerald-400/60",
    iconBg: "bg-emerald-500/15",
    emoji: "🎯",
    text: "text-emerald-500",
    badge: "bg-emerald-500/10 text-emerald-500 border-emerald-500/30",
    bar: "bg-emerald-500",
  },
];

export function getCategoryMeta(name: string): CategoryMeta {
  const found = CATEGORIES.find((c) => c.name === name);
  if (found) return found;
  return { ...FALLBACK, name, slug: name, label: name };
}

/** 카테고리 내 섹션 그룹 정의 */
export type GroupDef = {
  key: string;
  label: string;
  icon: string;
  description?: string;
};

/**
 * 모든 카테고리에 공통으로 적용되는 3그룹 체계.
 * frontmatter `group` 우선 → 없으면 slug 패턴으로 자동 추론.
 */
const COMMON_GROUPS: GroupDef[] = [
  {
    key: "기출 복원",
    label: "기출 복원",
    icon: "📜",
    description: "회차별 기출 복원 — 정답·해설 포함",
  },
  {
    key: "과목별 개념",
    label: "과목별 개념",
    icon: "📚",
    description: "시험 과목·핵심 개념을 깊이 있게 정리",
  },
  {
    key: "학습 전략",
    label: "학습 전략",
    icon: "🎯",
    description: "공부법, 단기 합격, 취업 활용법",
  },
  {
    key: "시험 정보",
    label: "시험 정보",
    icon: "🗓️",
    description: "시험일정, 합격률, 접수·환불 규정",
  },
];

/**
 * 카테고리별 섹션 그룹 순서.
 * 모든 카테고리가 동일한 3그룹 체계 공유.
 * 카테고리 정의 없으면 단일 그룹(폴백).
 */
export const CATEGORY_GROUPS: Record<string, GroupDef[]> = {
  SQLD: COMMON_GROUPS,
  ADsP: COMMON_GROUPS,
  정보처리기사: COMMON_GROUPS,
  "정보처리기사 필기": COMMON_GROUPS,
  컴퓨터활용능력: COMMON_GROUPS,
  "컴퓨터활용능력 2급": COMMON_GROUPS,
  일반: COMMON_GROUPS,
};

/**
 * frontmatter `group`이 없는 글의 그룹을 slug 패턴으로 추론.
 * 위에서부터 매칭되는 패턴 우선 (순서 중요).
 */
export function inferGroupFromSlug(slug: string): string {
  // 기출 복원: past-exam/ prefix
  if (slug.startsWith("past-exam/")) return "기출 복원";
  // 시험 정보: 일정·합격률·유효기간·응시료·CBT vs PBT·당일 팁·개편
  if (
    /(2026-schedule|-schedule$|pass-rate|expiry-renewal|fees-refund|cbt-vs-pbt|exam-day-tips|2024-reform|written-vs-practical)/.test(
      slug,
    )
  ) {
    return "시험 정보";
  }
  // 학습 전략: 공부법·벼락치기·비전공·과락·연결·입문·취업 활용·비교
  if (
    /(cram|study-guide|non-major|avoid-fail|balance|self-study|cbt-practice|cbt-strategy|to-practical|to-adp|to-1-upgrade|beginner|how-to-use-mock-exam|resume|combo-recommendation|sqld-vs-engineer|adsp-vs-sqld|memorization|code-solving|practical-tips|practical-guide)/.test(
      slug,
    )
  ) {
    return "학습 전략";
  }
  // 나머지는 과목별 개념 (sql·java·python·함수·정규화·보안·용어 정리 등)
  return "과목별 개념";
}

/** 섹션별 분류 결과 */
export type GroupedPosts = {
  key: string;
  label: string;
  icon: string;
  description?: string;
  posts: BlogPostMeta[];
};

/**
 * 카테고리의 글을 정의된 순서대로 섹션별로 묶는다.
 * - 카테고리에 그룹 정의가 없거나 매칭되는 글이 하나도 없으면 단일 "all" 그룹 하나만 반환.
 * - group 필드가 없는 글은 기타 섹션으로 들어가지 않고 누락되지 않도록 처리.
 */
export function groupPostsByMeta(
  category: string,
  posts: BlogPostMeta[],
): GroupedPosts[] {
  const defs = CATEGORY_GROUPS[category];
  if (!defs || defs.length === 0) {
    return [{ key: "all", label: "", icon: "", posts }];
  }

  const buckets = new Map<string, BlogPostMeta[]>();
  for (const def of defs) buckets.set(def.key, []);

  const leftovers: BlogPostMeta[] = [];
  for (const post of posts) {
    // frontmatter group 우선 → 없으면 slug 패턴으로 추론
    const effectiveGroup = post.group || inferGroupFromSlug(post.slug);
    if (buckets.has(effectiveGroup)) {
      buckets.get(effectiveGroup)!.push(post);
    } else {
      leftovers.push(post);
    }
  }

  const sections: GroupedPosts[] = defs
    .map((def) => ({
      key: def.key,
      label: def.label,
      icon: def.icon,
      description: def.description,
      posts: buckets.get(def.key) ?? [],
    }))
    .filter((s) => s.posts.length > 0);

  if (leftovers.length > 0) {
    sections.push({
      key: "기타",
      label: "기타",
      icon: "📌",
      posts: leftovers,
    });
  }

  // 어떤 그룹도 매칭 안 됐으면 단일 그룹으로 통합
  if (sections.length === 0) {
    return [{ key: "all", label: "", icon: "", posts }];
  }

  return sections;
}

/** "X분 읽기" 문자열에서 숫자를 파싱해 합산한 분 단위 총합 반환. */
export function sumReadingMinutes(posts: BlogPostMeta[]): number {
  let total = 0;
  for (const p of posts) {
    const m = p.readingTime.match(/(\d+(?:\.\d+)?)/);
    if (m) total += parseFloat(m[1]);
  }
  return Math.round(total);
}
