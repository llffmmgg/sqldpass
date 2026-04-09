"use client";

import { useState } from "react";

/**
 * 자격증별 샘플 문제 미리보기 — 탭 전환.
 * 새 자격증 추가 시 SAMPLES 배열에 1건 추가하면 끝.
 */

type Sample = {
  id: string;
  certLabel: string;
  topicLabel: string;
  topicSubject: string;
  question: string;
  /** 선택지 — 4개 고정 */
  choices: string[];
  /** 정답 (1-based) */
  correct: number;
  explanation: string;
};

const SAMPLES: Sample[] = [
  {
    id: "sqld",
    certLabel: "SQLD",
    topicLabel: "2과목",
    topicSubject: "SQL 활용",
    question: "다음 SQL의 실행 결과로 올바른 것은?\n\nSELECT COUNT(*) AS CNT,\n       SUM(CASE WHEN salary > 3000\n                THEN 1 ELSE 0 END) AS HIGH_SAL\nFROM   employee\nWHERE  dept_id = 10;",
    choices: ["CNT: 3, HIGH_SAL: 2", "CNT: 4, HIGH_SAL: 2", "CNT: 3, HIGH_SAL: 3", "CNT: 4, HIGH_SAL: 3"],
    correct: 1,
    explanation: "WHERE dept_id = 10 조건으로 3건이 조회되며, salary > 3000인 행은 102(3500)와 103(4000)으로 2건입니다.",
  },
  {
    id: "engineer",
    certLabel: "정보처리기사 실기",
    topicLabel: "프로그래밍",
    topicSubject: "Java 출력 결과",
    question: "다음 Java 코드의 실행 결과는?\n\npublic class Main {\n    public static void main(String[] args) {\n        int sum = 0;\n        for (int i = 1; i <= 10; i++) {\n            if (i % 2 == 0) sum += i;\n        }\n        System.out.println(sum);\n    }\n}",
    choices: ["25", "30", "55", "100"],
    correct: 2,
    explanation: "1~10 중 짝수(2, 4, 6, 8, 10)의 합은 30입니다.",
  },
  {
    id: "computer-literacy",
    certLabel: "컴퓨터활용능력 1급",
    topicLabel: "1과목",
    topicSubject: "컴퓨터 일반",
    question: "다음 중 ASCII 코드에 대한 설명으로 옳은 것은?",
    choices: [
      "1바이트(8비트)로 한글을 표현할 수 있다.",
      "7비트 구성으로 총 128가지 문자를 표현한다.",
      "유니코드와 호환되지 않는 별도 표준이다.",
      "확장 ASCII는 16비트 구성이다.",
    ],
    correct: 2,
    explanation: "기본 ASCII는 7비트로 0~127까지 총 128가지 문자(영문, 숫자, 기호, 제어문자)를 표현합니다.",
  },
];

export default function PreviewTabs() {
  const [activeId, setActiveId] = useState<string>(SAMPLES[0].id);
  const sample = SAMPLES.find((s) => s.id === activeId) ?? SAMPLES[0];

  return (
    <div className="mx-auto mt-12 max-w-3xl">
      {/* 탭 */}
      <div role="tablist" className="flex flex-wrap gap-1 border-b border-border">
        {SAMPLES.map((s) => {
          const active = s.id === activeId;
          return (
            <button
              key={s.id}
              role="tab"
              aria-selected={active}
              onClick={() => setActiveId(s.id)}
              className={`relative -mb-px border-b-2 px-4 py-2.5 text-sm font-medium transition ${
                active
                  ? "border-amber-400 text-amber-300"
                  : "border-transparent text-muted hover:text-foreground"
              }`}
            >
              {s.certLabel}
            </button>
          );
        })}
      </div>

      {/* 카드 */}
      <div className="mt-6 rounded-xl border border-border bg-surface p-6 sm:p-8">
        <div className="flex items-center gap-2 text-sm text-muted">
          <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-400">
            {sample.topicLabel}
          </span>
          <span>{sample.topicSubject}</span>
        </div>

        <pre className="mt-4 whitespace-pre-wrap break-words font-mono text-[13px] leading-relaxed text-foreground/90">
          {sample.question}
        </pre>

        <ul className="mt-6 space-y-2 text-sm">
          {sample.choices.map((choice, i) => (
            <li
              key={i}
              className="flex items-start gap-3 rounded-lg border border-border px-4 py-3 transition-colors hover:border-amber-500/40 hover:bg-amber-500/5"
            >
              <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-border text-xs text-muted">
                {i + 1}
              </span>
              <span className="leading-relaxed">{choice}</span>
            </li>
          ))}
        </ul>

        <details className="mt-6 rounded-lg border border-border px-4 py-3 text-sm">
          <summary className="flex cursor-pointer items-center gap-2 font-medium text-amber-400">
            <svg className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
            </svg>
            정답 보기
          </summary>
          <p className="mt-3 leading-relaxed text-muted">
            <strong className="text-foreground">정답: {sample.correct}번</strong> — {sample.explanation}
          </p>
        </details>
      </div>
    </div>
  );
}
