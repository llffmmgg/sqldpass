import type { PublicPastExamDetail } from "@/lib/publicApi";

/**
 * past-exams/[id] 의 layout(메타데이터·JSON-LD) 과 page(sr-only 본문) 양쪽에서
 * 같은 회차 표기를 쓰기 위한 공용 헬퍼.
 *
 * page.tsx 에서 layout.tsx 를 직접 import 하면 Next.js 라우팅 트리 안에서
 * 순환 의존성이 만들어져 라우트가 깨질 수 있어, 별도 lib 파일로 분리한다.
 */

export const CERT_DISPLAY: Record<string, string> = {
  SQLD: "SQLD",
  ENGINEER_PRACTICAL: "정보처리기사 실기",
  ENGINEER_WRITTEN: "정보처리기사 필기",
  COMPUTER_LITERACY_1: "컴퓨터활용능력 1급",
  COMPUTER_LITERACY_2: "컴퓨터활용능력 2급",
  ADSP: "ADsP",
};

export function buildRoundTitle(exam: PublicPastExamDetail): string {
  const cert = CERT_DISPLAY[exam.examType] ?? exam.name;
  const parts: string[] = [cert];
  if (exam.examYear) parts.push(`${exam.examYear}년`);
  if (exam.examRound) parts.push(`${exam.examRound}회`);
  return parts.join(" ");
}
