"use client";

/* eslint-disable react-hooks/set-state-in-effect, @next/next/no-img-element --
   인쇄 페이지는 한 번 마운트 후 fetch 결과를 그대로 표시하는 단순 흐름이라
   effect 안에서 setState 가 자연스럽다. img 태그는 백엔드가 R2 public URL 을
   주입한 markdown 콘텐츠를 그대로 렌더해야 하므로 next/image 적용 불가. */

import { use, useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeRaw from "rehype-raw";
import { PrismLight as SyntaxHighlighter } from "react-syntax-highlighter";
import { coy } from "react-syntax-highlighter/dist/esm/styles/prism";
import sql from "react-syntax-highlighter/dist/esm/languages/prism/sql";
import python from "react-syntax-highlighter/dist/esm/languages/prism/python";
import java from "react-syntax-highlighter/dist/esm/languages/prism/java";
import c from "react-syntax-highlighter/dist/esm/languages/prism/c";
import cpp from "react-syntax-highlighter/dist/esm/languages/prism/cpp";
import javascript from "react-syntax-highlighter/dist/esm/languages/prism/javascript";
import typescript from "react-syntax-highlighter/dist/esm/languages/prism/typescript";
import bash from "react-syntax-highlighter/dist/esm/languages/prism/bash";
import { parseQuestion, OPTION_MARKERS } from "@/lib/parseQuestion";

SyntaxHighlighter.registerLanguage("sql", sql);
SyntaxHighlighter.registerLanguage("python", python);
SyntaxHighlighter.registerLanguage("py", python);
SyntaxHighlighter.registerLanguage("java", java);
SyntaxHighlighter.registerLanguage("c", c);
SyntaxHighlighter.registerLanguage("cpp", cpp);
SyntaxHighlighter.registerLanguage("javascript", javascript);
SyntaxHighlighter.registerLanguage("js", javascript);
SyntaxHighlighter.registerLanguage("typescript", typescript);
SyntaxHighlighter.registerLanguage("ts", typescript);
SyntaxHighlighter.registerLanguage("bash", bash);

type QuestionType = "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";

interface PrintQuestion {
  id: number;
  displayOrder: number;
  content: string;
  questionType: QuestionType;
  correctOption: number | null;
  answer: string | null;
  keywords: string[];
  explanation: string | null;
  topic: string | null;
  difficulty: number | null;
  subjectName: string | null;
}

interface PrintMockExam {
  id: number;
  name: string;
  examType: string;
  sequence: number;
  totalQuestions: number;
  examYear: number | null;
  examRound: number | null;
  examDate: string | null;
  questions: PrintQuestion[];
}

const EXAM_TYPE_LABEL: Record<string, string> = {
  SQLD: "SQLD",
  ENGINEER_PRACTICAL: "정보처리기사 실기",
  ENGINEER_WRITTEN: "정보처리기사 필기",
  COMPUTER_LITERACY_1: "컴퓨터활용능력 1급",
  COMPUTER_LITERACY_2: "컴퓨터활용능력 2급",
  ADSP: "데이터분석 준전문가 (ADsP)",
};

/* ---------- 평문 SQL → ```sql 자동 펜싱 (QuestionContent 와 동일 로직) ---------- */
function ensureSqlFences(content: string): string {
  if (!content) return content;
  const lines = content.split("\n");
  const out: string[] = [];
  let i = 0;
  const SQL_START = /^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH|MERGE|TRUNCATE|GRANT|REVOKE|COMMIT|ROLLBACK|SAVEPOINT)\b/i;
  const SQL_CONT =
    /^\s*(FROM|WHERE|AND|OR|GROUP\s+BY|ORDER\s+BY|HAVING|JOIN|INNER\s+JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|FULL\s+JOIN|CROSS\s+JOIN|ON|UNION|UNION\s+ALL|INTERSECT|MINUS|EXCEPT|LIMIT|OFFSET|FETCH|VALUES|SET|RETURNING|WHEN|THEN|ELSE|END|CASE|INTO|USING|PARTITION\s+BY|WINDOW|RANGE|ROWS|TO|PUBLIC)\b/i;
  const HAS_KOREAN = /[가-힣]/;
  while (i < lines.length) {
    const line = lines[i];
    if (/^\s*```/.test(line)) {
      out.push(line);
      i++;
      while (i < lines.length && !/^\s*```/.test(lines[i])) {
        out.push(lines[i]);
        i++;
      }
      if (i < lines.length) {
        out.push(lines[i]);
        i++;
      }
      continue;
    }
    if (SQL_START.test(line) && !HAS_KOREAN.test(line)) {
      const sqlLines: string[] = [line];
      i++;
      while (i < lines.length) {
        const next = lines[i];
        if (next.trim() === "") break;
        const isContinuation =
          SQL_CONT.test(next) ||
          /^\s{2,}/.test(next) ||
          /^\s*[(),]/.test(next) ||
          /^\s*(SELECT|INSERT|UPDATE|DELETE|GRANT|REVOKE|COMMIT|ROLLBACK|SAVEPOINT)\b/i.test(next) ||
          /^\s*--/.test(next);
        if (!isContinuation) break;
        sqlLines.push(next);
        i++;
      }
      out.push("```sql");
      out.push(...sqlLines);
      out.push("```");
      continue;
    }
    out.push(line);
    i++;
  }
  return out.join("\n");
}

/* ---------- 인쇄용 markdown — 라이트 테마, 컴팩트 ---------- */
function PrintMarkdown({ content }: { content: string }) {
  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm]}
      rehypePlugins={[rehypeRaw]}
      components={{
        code({ className, children }) {
          const match = /language-(\w+)/.exec(className ?? "");
          const value = String(children ?? "");
          const isBlock = !!match || value.includes("\n");
          if (isBlock) {
            const lang = (match?.[1] ?? "text").toLowerCase();
            return (
              <div className="print-codeblock">
                <SyntaxHighlighter
                  language={lang}
                  style={coy}
                  PreTag="div"
                  customStyle={{
                    margin: 0,
                    padding: "8px 10px",
                    background: "#f7f7f5",
                    fontSize: "11px",
                    lineHeight: 1.45,
                  }}
                  codeTagProps={{
                    style: {
                      fontFamily:
                        "var(--font-jetbrains-mono), ui-monospace, SFMono-Regular, Menlo, Consolas, monospace",
                    },
                  }}
                >
                  {value.replace(/\n$/, "")}
                </SyntaxHighlighter>
              </div>
            );
          }
          return (
            <code className="print-inline-code">{children}</code>
          );
        },
        p({ children }) {
          return <p className="print-p">{children}</p>;
        },
        ul({ children }) {
          return <ul className="print-ul">{children}</ul>;
        },
        ol({ children }) {
          return <ol className="print-ol">{children}</ol>;
        },
        li({ children }) {
          return <li className="print-li">{children}</li>;
        },
        table({ children }) {
          return (
            <div className="print-table-wrap">
              <table>{children}</table>
            </div>
          );
        },
        img({ src, alt }) {
          if (!src || typeof src !== "string") return null;
          return (
            <img src={src} alt={alt ?? ""} className="print-img" />
          );
        },
        strong({ children }) {
          return <strong className="print-strong">{children}</strong>;
        },
      }}
    >
      {ensureSqlFences(content)}
    </ReactMarkdown>
  );
}

const QUESTION_TYPE_LABEL: Record<QuestionType, string> = {
  MCQ: "객관식",
  SHORT_ANSWER: "단답형",
  DESCRIPTIVE: "서술형",
};

function answerLabel(q: PrintQuestion): string {
  if (q.questionType === "MCQ") {
    if (q.correctOption == null) return "-";
    return ["①", "②", "③", "④"][q.correctOption - 1] ?? String(q.correctOption);
  }
  return q.answer ?? "-";
}

export default function PrintMockExamPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ token?: string }>;
}) {
  const { id } = use(params);
  const sp = use(searchParams);
  const token = sp.token ?? "";

  const [data, setData] = useState<PrintMockExam | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setError("token 쿼리 파라미터가 필요합니다.");
      return;
    }
    const url = `/api/internal/print/mock-exams/${id}?token=${encodeURIComponent(token)}`;
    fetch(url)
      .then(async (res) => {
        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          throw new Error(body.message ?? `HTTP ${res.status}`);
        }
        return res.json();
      })
      .then((json: PrintMockExam) => setData(json))
      .catch((e: Error) => setError(e.message));
  }, [id, token]);

  // 데이터·이미지·폰트 로드 완료 시그널 — Playwright 의 waitForFunction 이 이 attribute 를 본다.
  useEffect(() => {
    if (!data) return;
    const markReady = () => {
      document.body.setAttribute("data-print-ready", "1");
    };
    // document.fonts 가 아직 로드 중이면 대기
    if (document.fonts && typeof document.fonts.ready?.then === "function") {
      document.fonts.ready.then(markReady).catch(markReady);
    } else {
      markReady();
    }
  }, [data]);

  if (error) {
    return (
      <div className="print-error">
        <p>인쇄 페이지를 불러올 수 없습니다.</p>
        <p style={{ fontSize: "12px", color: "#888" }}>{error}</p>
      </div>
    );
  }
  if (!data) {
    return <div className="print-loading">로딩 중…</div>;
  }

  return (
    <>
      <style dangerouslySetInnerHTML={{ __html: PRINT_CSS }} />

      <div className="print-root">
        {/* 표지 */}
        <section className="print-cover">
          <div className="print-cover-brand">문어CBT · sqldpass.com</div>
          <h1 className="print-cover-title">
            {EXAM_TYPE_LABEL[data.examType] ?? data.examType}
          </h1>
          <p className="print-cover-name">{data.name}</p>
          <dl className="print-cover-meta">
            {data.examYear && (
              <>
                <dt>회차</dt>
                <dd>
                  {data.examYear}년
                  {data.examRound != null ? ` ${data.examRound}회` : ""}
                </dd>
              </>
            )}
            {data.examDate && (
              <>
                <dt>시험일</dt>
                <dd>{data.examDate}</dd>
              </>
            )}
            <dt>총 문항</dt>
            <dd>{data.totalQuestions}문항</dd>
          </dl>
          <p className="print-cover-notice">
            ※ 본 자료는 학습 목적의 모의고사로, 실제 시험과 차이가 있을 수 있습니다.
          </p>
        </section>

        {/* 본문 */}
        <section className="print-body">
          {data.questions.map((q, idx) => {
            const parsed =
              q.questionType === "MCQ" ? parseQuestion(q.content) : { body: q.content, options: [] as string[] };
            return (
              <article key={q.id} className="print-question">
                <header className="print-q-head">
                  <span className="print-q-no">{idx + 1}.</span>
                  {q.subjectName && (
                    <span className="print-q-subject">{q.subjectName}</span>
                  )}
                  <span className="print-q-type">
                    {QUESTION_TYPE_LABEL[q.questionType]}
                  </span>
                </header>
                <div className="print-q-body">
                  <PrintMarkdown content={parsed.body} />
                </div>
                {q.questionType === "MCQ" && parsed.options.length > 0 && (
                  <ol className="print-options">
                    {parsed.options.map((opt, i) => (
                      <li key={i}>
                        <span className="print-opt-marker">
                          {OPTION_MARKERS[i] ?? `${i + 1})`}
                        </span>
                        <span className="print-opt-text">{opt}</span>
                      </li>
                    ))}
                  </ol>
                )}
                {q.questionType !== "MCQ" && (
                  <div className="print-answer-blank">
                    <span>답:</span>
                    <span className="print-blank-line" />
                  </div>
                )}
              </article>
            );
          })}
        </section>

        {/* 정답표 */}
        <section className="print-answer-key">
          <h2>정답표</h2>
          <div className="print-answer-grid">
            {data.questions.map((q, idx) => (
              <div key={q.id} className="print-answer-cell">
                <span className="print-answer-no">{idx + 1}</span>
                <span className="print-answer-val">{answerLabel(q)}</span>
              </div>
            ))}
          </div>
        </section>

        {/* 해설 */}
        <section className="print-explanations">
          <h2>해설</h2>
          {data.questions.map((q, idx) => (
            <article key={q.id} className="print-explanation">
              <header className="print-exp-head">
                <span className="print-exp-no">{idx + 1}.</span>
                <span className="print-exp-answer">정답 {answerLabel(q)}</span>
                {q.topic && <span className="print-exp-topic">{q.topic}</span>}
              </header>
              <div className="print-exp-body">
                {q.explanation ? (
                  <PrintMarkdown content={q.explanation} />
                ) : (
                  <p className="print-exp-empty">(해설 미작성)</p>
                )}
                {q.questionType !== "MCQ" && q.keywords.length > 0 && (
                  <p className="print-exp-keywords">
                    <strong>채점 키워드:</strong> {q.keywords.join(", ")}
                  </p>
                )}
              </div>
            </article>
          ))}
        </section>
      </div>
    </>
  );
}

const PRINT_CSS = `
@page { size: A4; margin: 16mm 14mm; }

.print-root {
  font-family: "Noto Sans KR", -apple-system, BlinkMacSystemFont, "Apple SD Gothic Neo", "Malgun Gothic", sans-serif;
  font-size: 12px;
  line-height: 1.55;
  color: #111;
  background: #fff;
  max-width: 182mm;
  margin: 0 auto;
  padding: 0;
}

.print-loading, .print-error {
  font-family: -apple-system, BlinkMacSystemFont, sans-serif;
  padding: 40px;
  text-align: center;
  color: #555;
}

/* === 표지 === */
.print-cover {
  min-height: calc(297mm - 32mm);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  page-break-after: always;
  break-after: page;
}
.print-cover-brand {
  font-size: 11px;
  letter-spacing: 0.2em;
  color: #888;
  margin-bottom: 24px;
}
.print-cover-title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 12px 0;
  letter-spacing: -0.02em;
}
.print-cover-name {
  font-size: 18px;
  color: #333;
  margin: 0 0 40px 0;
  font-weight: 500;
}
.print-cover-meta {
  display: grid;
  grid-template-columns: auto auto;
  column-gap: 16px;
  row-gap: 6px;
  font-size: 13px;
  margin: 0 0 80px 0;
}
.print-cover-meta dt { color: #888; text-align: right; }
.print-cover-meta dd { margin: 0; text-align: left; font-weight: 500; }
.print-cover-notice {
  font-size: 10px;
  color: #999;
  margin-top: auto;
}

/* === 본문 === */
.print-body { page-break-before: always; }
.print-question {
  break-inside: avoid;
  page-break-inside: avoid;
  margin: 0 0 16px 0;
  padding: 0;
}
.print-q-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}
.print-q-no {
  font-weight: 700;
  font-size: 13px;
  color: #111;
}
.print-q-subject {
  font-size: 10px;
  color: #2563eb;
  background: #eff6ff;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 500;
}
.print-q-type {
  font-size: 10px;
  color: #6b7280;
  margin-left: auto;
}
.print-q-body { font-size: 12px; line-height: 1.6; }

.print-p { margin: 4px 0; white-space: pre-line; }
.print-ul { margin: 4px 0 4px 20px; padding: 0; }
.print-ol { margin: 4px 0 4px 20px; padding: 0; }
.print-li { margin: 2px 0; }
.print-strong { font-weight: 700; }
.print-inline-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  background: #f3f4f6;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 11px;
  color: #b91c1c;
}
.print-codeblock {
  margin: 6px 0;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #f7f7f5;
  break-inside: avoid;
  page-break-inside: avoid;
}
.print-codeblock pre, .print-codeblock div { background: transparent !important; }

.print-table-wrap { margin: 6px 0; overflow: visible; }
.print-table-wrap table {
  border-collapse: collapse;
  font-size: 11px;
}
.print-table-wrap th, .print-table-wrap td {
  border: 1px solid #d1d5db;
  padding: 4px 8px;
  text-align: left;
}
.print-table-wrap th { background: #f3f4f6; font-weight: 600; }
.print-img { max-width: 100%; margin: 6px 0; border: 1px solid #e5e7eb; }

.print-options {
  list-style: none;
  margin: 4px 0 0 16px;
  padding: 0;
}
.print-options li {
  display: flex;
  gap: 6px;
  margin: 2px 0;
  font-size: 12px;
}
.print-opt-marker { font-weight: 600; flex-shrink: 0; }
.print-opt-text { flex: 1; }

.print-answer-blank {
  margin: 6px 0 0 16px;
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
}
.print-blank-line {
  display: inline-block;
  flex: 1;
  border-bottom: 1px solid #999;
  height: 1.4em;
}

/* === 정답표 === */
.print-answer-key {
  page-break-before: always;
  break-before: page;
}
.print-answer-key h2 {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 12px 0;
  border-bottom: 2px solid #111;
  padding-bottom: 6px;
}
.print-answer-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 4px;
  margin-bottom: 24px;
}
.print-answer-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid #d1d5db;
  padding: 4px 2px;
  font-size: 11px;
}
.print-answer-no { color: #888; font-size: 9px; }
.print-answer-val { font-weight: 700; font-size: 13px; margin-top: 2px; }

/* === 해설 === */
.print-explanations {
  page-break-before: always;
  break-before: page;
}
.print-explanations h2 {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 12px 0;
  border-bottom: 2px solid #111;
  padding-bottom: 6px;
}
.print-explanation {
  margin: 0 0 14px 0;
  padding: 8px 0;
  border-bottom: 1px dashed #e5e7eb;
  break-inside: avoid;
  page-break-inside: avoid;
}
.print-exp-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 4px;
  font-size: 12px;
}
.print-exp-no { font-weight: 700; }
.print-exp-answer {
  background: #fef3c7;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 600;
  color: #92400e;
}
.print-exp-topic { color: #6b7280; font-size: 10px; margin-left: auto; }
.print-exp-body { font-size: 11.5px; line-height: 1.55; color: #333; }
.print-exp-empty { color: #999; font-style: italic; }
.print-exp-keywords {
  margin-top: 4px;
  font-size: 11px;
  color: #555;
  background: #f9fafb;
  padding: 4px 6px;
  border-left: 2px solid #d1d5db;
}
`;
