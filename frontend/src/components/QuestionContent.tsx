"use client";

import dynamic from "next/dynamic";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeRaw from "rehype-raw";

// react-syntax-highlighter는 8개 언어 등록까지 포함해 ~200KB. 코드블록이 실제로 렌더될 때만 로드한다.
const QuestionCodeBlock = dynamic(() => import("./QuestionCodeBlock"), {
  ssr: false,
  loading: () => (
    <div className="my-3 h-24 rounded-lg border border-zinc-800 bg-zinc-900/60 animate-pulse" />
  ),
});

/**
 * 펜스 없는 평문 코드 블록을 자동으로 ```<lang> ... ``` 으로 래핑.
 * 이미 펜스 안에 있는 코드는 건드리지 않는다.
 *
 * 배경:
 * - SQLD 레거시 문제: SQL이 펜스 없이 평문으로 저장됨.
 * - 정처기 필기 보기(① ② ③ ④): C/Java/Python 코드 조각이 펜스 없이 들어 있음.
 * - 두 경우 모두 react-markdown이 일반 단락으로 렌더 → 신택스 하이라이팅 X.
 * 이 전처리로 모든 페이지에서 코드가 블록으로 표시됨.
 */
function ensureCodeFences(content: string): string {
  if (!content) return content;
  const lines = content.split("\n");
  const out: string[] = [];
  let i = 0;

  // 한글이 섞인 문장은 코드로 보지 않는다.
  // "SELECT 문의 실행 결과는?" 같은 한글 질문이 코드로 잘못 펜싱되는 false positive 방지.
  const HAS_KOREAN = /[가-힣]/;

  // SQL: DCL(GRANT/REVOKE)·TCL(COMMIT/ROLLBACK/SAVEPOINT)도 시작으로 취급.
  const SQL_START = /^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH|MERGE|TRUNCATE|GRANT|REVOKE|COMMIT|ROLLBACK|SAVEPOINT)\b/i;
  const SQL_CONT =
    /^\s*(FROM|WHERE|AND|OR|GROUP\s+BY|ORDER\s+BY|HAVING|JOIN|INNER\s+JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|FULL\s+JOIN|CROSS\s+JOIN|ON|UNION|UNION\s+ALL|INTERSECT|MINUS|EXCEPT|LIMIT|OFFSET|FETCH|VALUES|SET|RETURNING|WHEN|THEN|ELSE|END|CASE|INTO|USING|PARTITION\s+BY|WINDOW|RANGE|ROWS|TO|PUBLIC)\b/i;

  // C/C++: #include, 함수 시그니처, printf/scanf, 제어문(괄호 포함)
  const C_START =
    /^\s*(#include\b|int\s+main\b|int\s+\w+\s*[=;(\[]|void\s+\w+\s*\(|char\s+\*?\w+|float\s+\w+|double\s+\w+|long\s+\w+|short\s+\w+|unsigned\s+\w+|struct\s+\w+|typedef\s+\b|return\s+\d|printf\s*\(|scanf\s*\(|while\s*\(|for\s*\(|if\s*\(|do\s*\{|switch\s*\()/;
  // Java: 접근 제어자/제어문/대표 API
  const JAVA_START =
    /^\s*(public\s+(static\s+)?(class|void|int|String|boolean)\b|private\s+\w+|protected\s+\w+|class\s+\w+\s*\{?|System\.out\.print|void\s+\w+\s*\(|try\s*\{|catch\s*\()/;
  // Python: def/class/import/print + ":" 끝나는 제어문
  const PY_START =
    /^\s*(def\s+\w+\s*\(|class\s+\w+\s*[:(]|import\s+\w+|from\s+\w+\s+import\b|print\s*\(|for\s+\w+\s+in\s+|while\s+.+:|if\s+.+:)/;

  // 들여쓰기 또는 닫는 괄호/세미콜론 위주의 연속 라인 (코드 본문 추정)
  const CONT_INDENTED = /^\s{2,}\S/;
  const CONT_SYMBOLS = /^\s*([{}();,]|\/\/|\/\*|\*\/|else\b|return\b)/;

  function detectLang(line: string): "sql" | "c" | "java" | "python" | null {
    if (HAS_KOREAN.test(line)) return null;
    if (SQL_START.test(line)) return "sql";
    if (C_START.test(line)) return "c";
    if (JAVA_START.test(line)) return "java";
    if (PY_START.test(line)) return "python";
    return null;
  }

  function isContinuation(line: string, lang: "sql" | "c" | "java" | "python"): boolean {
    if (line.trim() === "") return false;
    if (HAS_KOREAN.test(line)) return false;
    if (CONT_INDENTED.test(line) || CONT_SYMBOLS.test(line)) return true;
    if (lang === "sql") return SQL_CONT.test(line) || SQL_START.test(line);
    if (lang === "c") return C_START.test(line);
    if (lang === "java") return JAVA_START.test(line);
    if (lang === "python") return PY_START.test(line);
    return false;
  }

  while (i < lines.length) {
    const line = lines[i];

    // 1) 이미 펜스 안이면 닫는 펜스까지 통과
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

    // 2) 코드 시작 키워드 감지 → 같은 블록으로 묶어서 fence
    const lang = detectLang(line);
    if (lang) {
      const codeLines: string[] = [line];
      i++;
      while (i < lines.length) {
        const next = lines[i];
        if (next.trim() === "") break;
        if (!isContinuation(next, lang)) break;
        codeLines.push(next);
        i++;
      }
      // 1줄짜리 짧은 토큰(`for(;;)`)은 인라인 코드로 두고 펜싱하지 않음.
      // 다중 라인이거나 본문 길이가 충분할 때만 블록 처리.
      if (codeLines.length === 1 && codeLines[0].length < 40) {
        out.push(codeLines[0]);
        continue;
      }
      out.push("```" + lang);
      out.push(...codeLines);
      out.push("```");
      continue;
    }

    out.push(line);
    i++;
  }
  return out.join("\n");
}

interface QuestionContentProps {
  content: string;
  className?: string;
}

export default function QuestionContent({
  content,
  className,
}: QuestionContentProps) {
  return (
    <div className={`question-md ${className ?? ""}`}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeRaw]}
        components={{
          code({ className, children, ...props }) {
            const match = /language-(\w+)/.exec(className ?? "");
            const value = String(children ?? "");
            // 블록 코드: className에 language- 가 있거나 줄바꿈 포함
            const isBlock = !!match || value.includes("\n");
            if (isBlock) {
              return (
                <QuestionCodeBlock
                  language={match?.[1] ?? null}
                  value={value}
                />
              );
            }
            return (
              <code
                className="rounded bg-zinc-800 px-1.5 py-0.5 font-mono text-[0.85em] text-amber-300"
                {...props}
              >
                {children}
              </code>
            );
          },
          p({ children }) {
            return (
              <p className="my-2 text-sm leading-relaxed whitespace-pre-line">
                {children}
              </p>
            );
          },
          h1({ children }) {
            return <h3 className="mt-4 mb-2 text-lg font-bold">{children}</h3>;
          },
          h2({ children }) {
            return <h3 className="mt-4 mb-2 text-base font-bold">{children}</h3>;
          },
          h3({ children }) {
            return <h4 className="mt-3 mb-1.5 text-sm font-bold">{children}</h4>;
          },
          ul({ children }) {
            return <ul className="my-2 ml-5 list-disc space-y-1 text-sm">{children}</ul>;
          },
          ol({ children }) {
            return <ol className="my-2 ml-5 list-decimal space-y-1 text-sm">{children}</ol>;
          },
          li({ children }) {
            return <li className="leading-relaxed">{children}</li>;
          },
          blockquote({ children }) {
            return (
              <blockquote className="my-3 border-l-2 border-amber-500/50 bg-zinc-900/50 px-3 py-2 text-sm text-muted">
                {children}
              </blockquote>
            );
          },
          table({ children }) {
            return (
              <div className="my-3 overflow-x-auto">
                <table className="w-full text-left text-sm border-collapse">
                  {children}
                </table>
              </div>
            );
          },
          thead({ children }) {
            return <thead className="border-b border-zinc-700 text-muted">{children}</thead>;
          },
          th({ children }) {
            return <th className="px-3 py-2 font-medium">{children}</th>;
          },
          td({ children }) {
            return <td className="border-b border-zinc-800 px-3 py-2 font-mono text-zinc-300">{children}</td>;
          },
          a({ children, href }) {
            return (
              <a
                href={href}
                className="text-amber-400 underline-offset-2 hover:underline"
                target="_blank"
                rel="noopener noreferrer"
              >
                {children}
              </a>
            );
          },
          strong({ children }) {
            return <strong className="font-semibold text-foreground">{children}</strong>;
          },
          img({ src, alt }) {
            if (!src || typeof src !== "string") return null;
            return (
              <img
                src={src}
                alt={alt ?? ""}
                loading="lazy"
                className="my-3 max-w-full rounded-lg border border-border bg-white"
              />
            );
          },
        }}
      >
        {ensureCodeFences(content)}
      </ReactMarkdown>
    </div>
  );
}
