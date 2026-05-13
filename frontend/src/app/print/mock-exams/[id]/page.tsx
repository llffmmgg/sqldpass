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
  kind?: "AI" | "PAST_EXAM";
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

/** 표지에 들어갈 자격증별 메타 — 큰 제목(KOR), 영문 라벨(ENG), 시간/과목수.
 *  문항수는 실제 totalQuestions 우선, fallback 으로 본 표의 값 사용. */
const COVER_META: Record<string, { kor: string; eng: string; questions: number; minutes: number; subjects: number }> = {
  SQLD: { kor: "SQLD", eng: "SQLD MOCK EXAM", questions: 50, minutes: 90, subjects: 5 },
  ENGINEER_WRITTEN: { kor: "정처기 필기", eng: "ENGINEER WRITTEN", questions: 100, minutes: 150, subjects: 5 },
  ENGINEER_PRACTICAL: { kor: "정처기 실기", eng: "ENGINEER PRACTICAL", questions: 20, minutes: 150, subjects: 1 },
  COMPUTER_LITERACY_1: { kor: "컴활 1급", eng: "COMPUTER LITERACY 1", questions: 60, minutes: 60, subjects: 3 },
  COMPUTER_LITERACY_2: { kor: "컴활 2급", eng: "COMPUTER LITERACY 2", questions: 40, minutes: 40, subjects: 2 },
  ADSP: { kor: "ADsP", eng: "ADSP MOCK EXAM", questions: 50, minutes: 90, subjects: 3 },
};

/** kind=PAST_EXAM 일 때 사용되는 영문 라벨. 자격증 종류별로 'PAST EXAM' 변형을 가진다. */
const COVER_META_PAST_ENG: Record<string, string> = {
  SQLD: "SQLD PAST EXAM",
  ENGINEER_WRITTEN: "ENGINEER WRITTEN PAST EXAM",
  ENGINEER_PRACTICAL: "ENGINEER PRACTICAL PAST EXAM",
  COMPUTER_LITERACY_1: "COMPUTER LITERACY 1 PAST EXAM",
  COMPUTER_LITERACY_2: "COMPUTER LITERACY 2 PAST EXAM",
  ADSP: "ADSP PAST EXAM",
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
  // 에러 상태에서도 즉시 시그널을 박아서 Playwright 가 60초 대기 없이 빨리 실패하게 한다.
  useEffect(() => {
    if (error) {
      document.body.setAttribute("data-print-error", error);
      document.body.setAttribute("data-print-ready", "error");
      return;
    }
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
  }, [data, error]);

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
        {/* 표지 — 보라 Hero 디자인 */}
        {(() => {
          const baseMeta = COVER_META[data.examType] ?? {
            kor: EXAM_TYPE_LABEL[data.examType] ?? data.examType,
            eng: data.examType,
            questions: data.totalQuestions,
            minutes: 90,
            subjects: 1,
          };
          const isPast = data.kind === "PAST_EXAM";
          const engLabel = isPast
            ? COVER_META_PAST_ENG[data.examType] ?? `${baseMeta.eng} PAST EXAM`
            : baseMeta.eng;
          const meta = { ...baseMeta, eng: engLabel };
          const totalQ = data.totalQuestions || meta.questions;
          const volume =
            isPast && data.examYear && data.examRound
              ? `${data.examYear}.${data.examRound}회`
              : `VOL.${data.sequence}`;
          return (
            <section className="print-cover">
              <div className="cover-block" />

              <div className="cover-topRow">
                <div className="cover-brand">
                  <div className="cover-mark">문</div>
                  <div className="cover-name">문어CBT</div>
                </div>
              </div>

              <div className="cover-vlabel">── {meta.eng} / {volume}</div>

              <div className="cover-headline">
                <div className="cover-big">{meta.kor}</div>
                <div className="cover-sub">{isPast ? "기출" : "모의고사"}<b>.</b></div>
              </div>

              <div className="cover-stats">
                <div className="cover-chip">{totalQ}문항</div>
                <div className="cover-chip">{meta.minutes}분</div>
                <div className="cover-chip">{meta.subjects}과목</div>
                <div className="cover-chip">해설포함</div>
              </div>

              <div className="cover-mascot">
                {/* eslint-disable-next-line @next/next/no-img-element -- 인쇄 페이지는 same-origin 정적 이미지를 그대로 사용 */}
                <img src="/logo/logo.webp" alt="" />
              </div>
            </section>
          );
        })()}

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

/* === 표지 — 보라 Hero ===
   원본 standalone HTML 의 .cover (794×1123) 를 A4 본문 폭에 맞게 재현.
   .print-root 가 max-width 182mm 라 표지는 그 안에서 가득 채우는 형태. */
.print-cover {
  position: relative;
  width: 182mm;
  height: 257mm;            /* A4 297mm − 상하 여백 16mm × 2 */
  margin: 0;
  background: #fff;
  color: #0F1424;
  overflow: hidden;
  page-break-after: always;
  break-after: page;
  font-family: "Noto Sans KR", -apple-system, BlinkMacSystemFont, "Apple SD Gothic Neo", "Malgun Gothic", sans-serif;
}
.print-cover .cover-block {
  position: absolute; top: 0; left: 0; right: 0; height: 65%;
  background: #7C5CC4;
  -webkit-clip-path: polygon(0 0, 100% 0, 100% 88%, 0 100%);
          clip-path: polygon(0 0, 100% 0, 100% 88%, 0 100%);
}
.print-cover .cover-topRow {
  position: absolute; top: 14mm; left: 14mm; right: 14mm;
  display: flex; align-items: center; justify-content: space-between;
  color: #fff;
}
.print-cover .cover-brand {
  display: flex; align-items: center; gap: 8px;
}
.print-cover .cover-mark {
  width: 28px; height: 28px; border-radius: 7px; background: #fff;
  display: flex; align-items: center; justify-content: center;
  color: #7C5CC4; font-weight: 800; font-size: 14px;
}
.print-cover .cover-name { font-weight: 700; font-size: 15px; }
.print-cover .cover-vlabel {
  position: absolute; top: 30mm; left: 14mm;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 10px; color: rgba(255,255,255,0.78);
  letter-spacing: 4px; font-weight: 500;
}
.print-cover .cover-headline {
  position: absolute; top: 42mm; left: 14mm; right: 14mm; color: #fff;
}
.print-cover .cover-big {
  font-size: 110px; font-weight: 900; line-height: 0.85; letter-spacing: -5px;
}
.print-cover .cover-sub {
  font-size: 48px; font-weight: 300; line-height: 1; letter-spacing: -2px; margin-top: 6px;
}
.print-cover .cover-sub b { font-weight: 800; }
.print-cover .cover-stats {
  position: absolute; top: 130mm; left: 14mm;
  display: flex; gap: 8px; flex-wrap: wrap;
}
.print-cover .cover-chip {
  padding: 7px 14px; border-radius: 999px;
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.32);
  color: #fff; font-size: 12px; font-weight: 600;
}
.print-cover .cover-mascot {
  position: absolute; right: -20mm; top: 130mm;
  width: 110mm; height: 95mm;
}
.print-cover .cover-mascot img { width: 100%; height: 100%; object-fit: contain; }

@media print {
  .print-cover .cover-block,
  .print-cover .cover-chip {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
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
