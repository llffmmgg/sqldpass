"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import {
  createManualMockExam,
  type ManualMockExamPayload,
} from "@/lib/adminApi";

const EXAMPLE_PAYLOAD: ManualMockExamPayload = {
  name: "SQLD 58회 (수동)",
  examType: "SQLD",
  pastExam: true,
  examYear: 2025,
  examRound: 58,
  examDate: "2025-09-21",
  expertVerified: true,
  questions: [
    {
      subjectId: 6,
      content:
        "다음 SQL 결과는? (Oracle)\n\n```sql\nSELECT NVL(NULL, 'X') FROM DUAL;\n```\n\n① NULL\n② 빈 문자열\n③ X\n④ 오류",
      questionType: "MCQ",
      correctOption: 3,
      explanation:
        "NVL(expr1, expr2): expr1이 NULL이면 expr2 반환. ①②④는 모두 잘못된 추론.",
      summary: "NVL 기본 동작",
      topic: "NULL 처리 함수",
      difficulty: 1,
    },
    {
      subjectId: 6,
      content:
        "다음 SQL 결과 행수는?\n\n[A] ID:1,2,3 [B] ID:1,2,4\n\n```sql\nSELECT * FROM A FULL OUTER JOIN B ON A.ID=B.ID;\n```\n\n① 2\n② 3\n③ 4\n④ 6",
      questionType: "MCQ",
      correctOption: 3,
      explanation: "매칭 (1,1)(2,2) + A 단독 (3,NULL) + B 단독 (NULL,4) = 4건",
      summary: "FULL OUTER JOIN 결과",
      topic: "JOIN",
      difficulty: 2,
    },
  ],
};

const SCHEMA_HELP = `필드 설명:
- name (필수): 모의고사 이름
- examType (필수): SQLD | ENGINEER_PRACTICAL | COMPUTER_LITERACY_1 | COMPUTER_LITERACY_2 | ENGINEER_WRITTEN | ADSP
- pastExam (옵션): true 면 PAST_EXAM 으로 승격. examYear/examRound/examDate 함께 입력
- expertVerified (옵션): true 면 등록과 동시에 전문가 검수 완료 플래그 ON
- questions (필수, 1개 이상)
  - subjectId (필수): leaf 과목 id
  - content (필수): 본문 (마크다운/코드 블록 가능)
  - questionType: MCQ (기본) | SHORT_ANSWER | DESCRIPTIVE
  - correctOption: MCQ 일 때 1~4 필수
  - answer: SHORT_ANSWER/DESCRIPTIVE 일 때 필수
  - keywords: SHORT_ANSWER alias / DESCRIPTIVE 채점 키워드 (배열)
  - explanation (필수): 해설
  - summary, topic, difficulty: 옵션`;

export default function ManualMockExamPage() {
  const router = useRouter();
  const [text, setText] = useState<string>(
    JSON.stringify(EXAMPLE_PAYLOAD, null, 2),
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [parseHint, setParseHint] = useState<string | null>(null);

  function handleValidate() {
    setError(null);
    setParseHint(null);
    try {
      const parsed = JSON.parse(text) as ManualMockExamPayload;
      if (!parsed.name || !parsed.examType || !Array.isArray(parsed.questions)) {
        throw new Error("name, examType, questions 가 모두 있어야 합니다.");
      }
      setParseHint(
        `JSON 유효 — examType=${parsed.examType}, 문제 ${parsed.questions.length}개, ` +
          `pastExam=${parsed.pastExam ?? false}, expertVerified=${parsed.expertVerified ?? false}`,
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : "JSON 파싱 실패");
    }
  }

  async function handleSubmit() {
    setError(null);
    let payload: ManualMockExamPayload;
    try {
      payload = JSON.parse(text) as ManualMockExamPayload;
    } catch (e) {
      setError(e instanceof Error ? e.message : "JSON 파싱 실패");
      return;
    }

    if (!confirm(`${payload.name} 으로 모의고사를 등록할까요?`)) return;
    setSubmitting(true);
    try {
      const created = await createManualMockExam(payload);
      router.push(`/admin/mock-exams/${created.id}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "등록 실패");
      setSubmitting(false);
    }
  }

  function handleReset() {
    if (!confirm("입력 내용을 예시로 초기화할까요?")) return;
    setText(JSON.stringify(EXAMPLE_PAYLOAD, null, 2));
    setError(null);
    setParseHint(null);
  }

  return (
    <div>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">수동 모의고사 등록</h1>
        <p className="text-sm text-muted">
          JSON 한 통으로 모의고사 메타 + 문제 N개를 동시에 적재합니다. AI 자동
          생성과는 별도 경로입니다.
        </p>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-[1fr_320px]">
        <div className="flex flex-col gap-3">
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            spellCheck={false}
            className="min-h-[600px] w-full resize-y rounded-lg border border-border bg-surface/50 p-3 font-mono text-xs text-foreground focus:border-primary focus:outline-none"
          />
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={handleValidate}
              disabled={submitting}
              className="rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-foreground hover:bg-surface/80 disabled:opacity-50"
            >
              JSON 유효성 확인
            </button>
            <button
              type="button"
              onClick={handleSubmit}
              disabled={submitting}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-zinc-900 hover:bg-primary-hover disabled:opacity-50"
            >
              {submitting ? "등록 중..." : "모의고사 등록"}
            </button>
            <button
              type="button"
              onClick={handleReset}
              disabled={submitting}
              className="ml-auto rounded-lg border border-border bg-surface px-3 py-2 text-xs text-muted hover:text-foreground disabled:opacity-50"
            >
              예시로 초기화
            </button>
          </div>

          {parseHint && (
            <div className="rounded-lg border border-emerald-500/30 bg-emerald-500/5 p-3 text-xs text-emerald-300">
              {parseHint}
            </div>
          )}
          {error && (
            <div className="rounded-lg border border-red-500/30 bg-red-500/5 p-3 text-xs text-red-400">
              {error}
            </div>
          )}
        </div>

        <aside className="rounded-lg border border-border bg-surface/30 p-4">
          <h2 className="text-sm font-semibold text-foreground">스키마</h2>
          <pre className="mt-2 whitespace-pre-wrap break-words text-[11px] leading-relaxed text-muted">
            {SCHEMA_HELP}
          </pre>
          <h3 className="mt-4 text-xs font-semibold text-foreground">참고</h3>
          <ul className="mt-1 list-disc pl-4 text-[11px] leading-relaxed text-muted">
            <li>
              subjectId 는 leaf 과목 id. 자격증 트리에서 해당 과목 id 를 미리
              확인 후 입력.
            </li>
            <li>등록 후 visibility 는 DRAFT. 모의고사 목록에서 공개 처리.</li>
            <li>
              실패해도 트랜잭션 롤백되어 부분 저장은 발생하지 않음. 에러 메시지를
              확인 후 JSON 수정 → 재시도.
            </li>
          </ul>
        </aside>
      </div>
    </div>
  );
}
