"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";

import {
  createManualMockExam,
  type CreateMockExamType,
  type ManualMockExamPayload,
  type ManualMockExamQuestion,
  type MockExamCreationDifficulty,
} from "@/lib/adminApi";

const EXAM_TYPES: { id: CreateMockExamType; label: string }[] = [
  { id: "SQLD", label: "SQLD" },
  { id: "ENGINEER_PRACTICAL", label: "정처기 실기" },
  { id: "ENGINEER_WRITTEN", label: "정처기 필기" },
  { id: "COMPUTER_LITERACY_1", label: "컴활 1급" },
  { id: "COMPUTER_LITERACY_2", label: "컴활 2급" },
  { id: "ADSP", label: "ADsP" },
];

const DIFFICULTIES: { id: MockExamCreationDifficulty; label: string }[] = [
  { id: "EASY", label: "쉬움" },
  { id: "NORMAL", label: "보통" },
  { id: "HARD", label: "어려움" },
  { id: "VERY_HARD", label: "매우 어려움" },
];

const EXAMPLE_QUESTIONS: ManualMockExamQuestion[] = [
  {
    subjectId: 6,
    content:
      "다음 SQL 결과는? (Oracle)\n\n```sql\nSELECT NVL(NULL, 'X') FROM DUAL;\n```\n\n① NULL\n② 빈 문자열\n③ X\n④ 오류",
    questionType: "MCQ",
    correctOption: 3,
    explanation: "NVL(expr1, expr2): expr1이 NULL이면 expr2 반환.",
    summary: "NVL 기본 동작",
    topic: "NULL 처리 함수",
    difficulty: 1,
  },
];

const SCHEMA_HELP = `questions[*] 필드:
- subjectId (필수): leaf 과목 id
- content (필수): 본문 (마크다운/코드 블록 가능)
- questionType: MCQ (기본) | SHORT_ANSWER | DESCRIPTIVE
- correctOption: MCQ 일 때 1~4 필수
- answer: SHORT_ANSWER/DESCRIPTIVE 일 때 필수
- keywords: SHORT_ANSWER alias / DESCRIPTIVE 채점 키워드 (배열)
- explanation (필수): 해설
- summary, topic, difficulty: 옵션`;

/**
 * JSON.parse 가 실패하면 string literal 안의 raw newline 을 \\n 으로 escape 후 재시도.
 * 사용자가 멀티라인 본문을 그대로 textarea 에 붙여넣어도 동작하도록 한다.
 */
function tryParseQuestionsJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    const fixed = text.replace(/"((?:[^"\\]|\\[\s\S])*)"/g, (_match, inner: string) =>
      '"' +
      inner
        .replace(/\r\n/g, "\\n")
        .replace(/\n/g, "\\n")
        .replace(/\r/g, "\\n")
        .replace(/\t/g, "\\t") +
      '"',
    );
    return JSON.parse(fixed);
  }
}

export default function ManualMockExamPage() {
  const router = useRouter();

  const [examType, setExamType] = useState<CreateMockExamType>("SQLD");
  const [difficulty, setDifficulty] = useState<MockExamCreationDifficulty>("NORMAL");
  const [expertVerified, setExpertVerified] = useState(true);
  const [isPastExam, setIsPastExam] = useState(false);
  const [examYear, setExamYear] = useState<string>("");
  const [examRound, setExamRound] = useState<string>("");
  const [examDate, setExamDate] = useState<string>("");
  const [customName, setCustomName] = useState<string>("");

  const [text, setText] = useState<string>(
    JSON.stringify(EXAMPLE_QUESTIONS, null, 2),
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [parseHint, setParseHint] = useState<string | null>(null);

  const namePreview = useMemo(() => {
    if (customName.trim()) return customName.trim();
    const examLabel = EXAM_TYPES.find((e) => e.id === examType)?.label ?? examType;
    const diffLabel = DIFFICULTIES.find((d) => d.id === difficulty)?.label ?? "";
    return `${examLabel} 모의고사 N회 (${diffLabel})  ※ N은 등록 시 백엔드가 자동 부여`;
  }, [customName, examType, difficulty]);

  function handleValidate() {
    setError(null);
    setParseHint(null);
    try {
      const parsed = tryParseQuestionsJson(text);
      if (!Array.isArray(parsed)) {
        throw new Error("questions 는 배열([...]) 형태여야 합니다.");
      }
      setParseHint(`JSON 유효 — 문제 ${parsed.length}개`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "JSON 파싱 실패");
    }
  }

  async function handleSubmit() {
    setError(null);
    let parsed: unknown;
    try {
      parsed = tryParseQuestionsJson(text);
    } catch (e) {
      setError(e instanceof Error ? e.message : "JSON 파싱 실패");
      return;
    }
    if (!Array.isArray(parsed) || parsed.length === 0) {
      setError("questions 는 1개 이상의 배열이어야 합니다.");
      return;
    }

    const payload: ManualMockExamPayload = {
      examType,
      difficulty,
      expertVerified,
      questions: parsed as ManualMockExamQuestion[],
    };
    if (customName.trim()) payload.name = customName.trim();
    if (isPastExam) {
      payload.pastExam = true;
      if (examYear) payload.examYear = Number(examYear);
      if (examRound) payload.examRound = Number(examRound);
      if (examDate) payload.examDate = examDate;
    }

    if (!confirm(`${parsed.length}문항 모의고사를 등록할까요?\n이름: ${namePreview}`)) {
      return;
    }
    setSubmitting(true);
    try {
      const created = await createManualMockExam(payload);
      router.push(`/admin/mock-exams/${created.id}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "등록 실패");
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">수동 모의고사 등록</h1>
        <p className="text-sm text-muted">
          폼에서 자격증·난이도를 고르고 아래 textarea 에 questions 배열만 붙여넣으세요.
          이름은 백엔드가 자동으로 채워줍니다.
        </p>
      </div>

      {/* 메타 폼 */}
      <div className="mt-6 rounded-xl border border-border bg-surface/30 p-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="flex flex-col gap-1.5 text-xs">
            <span className="font-semibold text-foreground">자격증</span>
            <select
              value={examType}
              onChange={(e) => setExamType(e.target.value as CreateMockExamType)}
              className="rounded border border-border bg-background px-2 py-1.5 text-sm"
            >
              {EXAM_TYPES.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.label}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1.5 text-xs">
            <span className="font-semibold text-foreground">평균 난이도 (이름 라벨용)</span>
            <select
              value={difficulty}
              onChange={(e) =>
                setDifficulty(e.target.value as MockExamCreationDifficulty)
              }
              className="rounded border border-border bg-background px-2 py-1.5 text-sm"
            >
              {DIFFICULTIES.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-4 text-xs">
          <label className="inline-flex items-center gap-2">
            <input
              type="checkbox"
              checked={expertVerified}
              onChange={(e) => setExpertVerified(e.target.checked)}
              className="h-3.5 w-3.5"
            />
            <span>전문가 검수 완료(expertVerified) ON</span>
          </label>
          <label className="inline-flex items-center gap-2">
            <input
              type="checkbox"
              checked={isPastExam}
              onChange={(e) => setIsPastExam(e.target.checked)}
              className="h-3.5 w-3.5"
            />
            <span>기출 복원(PAST_EXAM) 으로 등록</span>
          </label>
        </div>

        {isPastExam && (
          <div className="mt-3 grid gap-2 sm:grid-cols-3">
            <input
              type="number"
              placeholder="연도 (예: 2025)"
              value={examYear}
              onChange={(e) => setExamYear(e.target.value)}
              className="rounded border border-border bg-background px-2 py-1.5 text-sm"
            />
            <input
              type="number"
              placeholder="회차 (예: 58)"
              value={examRound}
              onChange={(e) => setExamRound(e.target.value)}
              className="rounded border border-border bg-background px-2 py-1.5 text-sm"
            />
            <input
              type="date"
              value={examDate}
              onChange={(e) => setExamDate(e.target.value)}
              className="rounded border border-border bg-background px-2 py-1.5 text-sm"
            />
          </div>
        )}

        <div className="mt-4">
          <label className="flex flex-col gap-1.5 text-xs">
            <span className="font-semibold text-foreground">
              이름 직접 지정(선택 — 비우면 자동)
            </span>
            <input
              value={customName}
              onChange={(e) => setCustomName(e.target.value)}
              placeholder={namePreview}
              className="rounded border border-border bg-background px-2 py-1.5 text-sm"
            />
          </label>
          <p className="mt-1 text-[11px] text-muted">
            미리보기: <span className="text-foreground">{namePreview}</span>
          </p>
        </div>
      </div>

      {/* questions textarea */}
      <div className="mt-6 grid gap-4 lg:grid-cols-[1fr_320px]">
        <div className="flex flex-col gap-3">
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            spellCheck={false}
            className="min-h-[520px] w-full resize-y rounded-lg border border-border bg-surface/50 p-3 font-mono text-xs text-foreground focus:border-primary focus:outline-none"
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
              questions 는 배열만 넣으세요 — 메타(name·examType 등)는 위 폼에서 지정합니다.
            </li>
            <li>
              본문(content)에 줄바꿈이 raw 로 들어 있어도 자동으로 escape 후 파싱합니다.
            </li>
            <li>
              subjectId 는 leaf 과목 id. SQLD 기준 3=데이터 모델링의 이해, 4=데이터 모델과
              SQL, 5=SQL 기본, 6=SQL 활용, 7=관리 구문.
            </li>
            <li>등록 후 visibility 는 DRAFT. 모의고사 목록에서 PUBLISHED 로 변경.</li>
          </ul>
        </aside>
      </div>
    </div>
  );
}
