/**
 * 자격증별 공식 합격 기준 / 과목별 과락 룰.
 *
 * 출처:
 * - SQLD / ADsP: dataq.or.kr 공식
 * - 정처기 필기/실기: q-net.or.kr 공식
 * - 컴활 1급/2급: license.korcham.net 공식
 *
 * 모든 자격증 합격선은 60점, 과락 룰은 모두 과목별 40% 미만.
 * 정처기 실기만 단일 통합 과목이라 과락 비활성.
 */

import type { ExamType } from "@/lib/mockExamApi";

export interface PassCriteria {
  passScore: number;
  subjectCutoffPercent: number;
  subjectCutoffApplies: boolean;
}

export const PASS_CRITERIA: Record<ExamType, PassCriteria> = {
  SQLD: { passScore: 60, subjectCutoffPercent: 40, subjectCutoffApplies: true },
  ENGINEER_WRITTEN: { passScore: 60, subjectCutoffPercent: 40, subjectCutoffApplies: true },
  ENGINEER_PRACTICAL: { passScore: 60, subjectCutoffPercent: 0, subjectCutoffApplies: false },
  COMPUTER_LITERACY_1: { passScore: 60, subjectCutoffPercent: 40, subjectCutoffApplies: true },
  COMPUTER_LITERACY_2: { passScore: 60, subjectCutoffPercent: 40, subjectCutoffApplies: true },
  ADSP: { passScore: 60, subjectCutoffPercent: 40, subjectCutoffApplies: true },
};

export interface SubjectScore {
  subjectName: string;
  total: number;
  correct: number;
  rate: number;
  weighted: number;
  failed: boolean;
}

export interface PassOutcome {
  subjectScores: SubjectScore[];
  passed: boolean;
  passReason: string;
}

interface QuestionLike {
  id: number;
  subjectName: string;
}

interface AnswerLike {
  questionId: number;
  correct: boolean;
}

/**
 * 자격증별 합격 기준 적용해서 과목별 점수 + 합격 여부를 계산.
 *
 * 그룹핑은 question.subjectName 기준 (백엔드가 leaf의 parent로 한 단계 올려놓아 보내줌 — PastExamPublicService.get).
 * 모의고사 응시도 동일한 subjectName 단위로 들어옴 (MockExamDetailResponse.Question.subjectName).
 */
export function evaluatePass(
  examType: ExamType,
  questions: QuestionLike[],
  answers: AnswerLike[],
  totalScore: number,
): PassOutcome {
  const criteria = PASS_CRITERIA[examType] ?? PASS_CRITERIA.ENGINEER_PRACTICAL;

  // 정처기 실기는 단일 통합 과목 — 카테고리(C/Java/Python 등) 분리 없이 1줄로 합침.
  const groupLabelOf = (name: string | undefined) =>
    examType === "ENGINEER_PRACTICAL" ? "정보처리기사 실기" : (name ?? "기타");

  const subjectByQuestion = new Map<number, string>();
  for (const q of questions) {
    subjectByQuestion.set(q.id, groupLabelOf(q.subjectName));
  }

  // 과목별 total/correct 집계 — 등장 순서 유지
  const order: string[] = [];
  const agg = new Map<string, { total: number; correct: number }>();
  for (const a of answers) {
    const name = subjectByQuestion.get(a.questionId) ?? groupLabelOf(undefined);
    if (!agg.has(name)) {
      order.push(name);
      agg.set(name, { total: 0, correct: 0 });
    }
    const slot = agg.get(name)!;
    slot.total += 1;
    if (a.correct) slot.correct += 1;
  }

  let anyFailed = false;
  const subjectScores: SubjectScore[] = order.map((name) => {
    const { total, correct } = agg.get(name)!;
    const rate = total > 0 ? (correct * 100) / total : 0;
    const failed = criteria.subjectCutoffApplies && rate < criteria.subjectCutoffPercent;
    if (failed) anyFailed = true;
    return {
      subjectName: name,
      total,
      correct,
      rate: Math.round(rate * 10) / 10,
      weighted: Math.round(rate),
      failed,
    };
  });

  const totalOk = totalScore >= criteria.passScore;
  const passed = totalOk && !anyFailed;

  let passReason: string;
  if (passed) {
    passReason = `합격 (총점 ${totalScore}점)`;
  } else if (!totalOk && anyFailed) {
    passReason = `불합격 — 총점 ${totalScore}점 미달 + 과락 과목 있음`;
  } else if (!totalOk) {
    passReason = `불합격 — 총점 ${totalScore}점 (${criteria.passScore}점 이상 필요)`;
  } else {
    const failedNames = subjectScores
      .filter((s) => s.failed)
      .map((s) => `${s.subjectName}(${Math.round(s.rate)}%)`)
      .join(", ");
    passReason = `불합격 — 과락 과목: ${failedNames}`;
  }

  return { subjectScores, passed, passReason };
}
