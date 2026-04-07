"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { PrismLight as SyntaxHighlighter } from "react-syntax-highlighter";
import { oneDark } from "react-syntax-highlighter/dist/esm/styles/prism";
import sql from "react-syntax-highlighter/dist/esm/languages/prism/sql";
import python from "react-syntax-highlighter/dist/esm/languages/prism/python";
import java from "react-syntax-highlighter/dist/esm/languages/prism/java";
import c from "react-syntax-highlighter/dist/esm/languages/prism/c";
import cpp from "react-syntax-highlighter/dist/esm/languages/prism/cpp";
import javascript from "react-syntax-highlighter/dist/esm/languages/prism/javascript";
import typescript from "react-syntax-highlighter/dist/esm/languages/prism/typescript";
import bash from "react-syntax-highlighter/dist/esm/languages/prism/bash";

SyntaxHighlighter.registerLanguage("sql", sql);
SyntaxHighlighter.registerLanguage("python", python);
SyntaxHighlighter.registerLanguage("py", python);
SyntaxHighlighter.registerLanguage("java", java);
SyntaxHighlighter.registerLanguage("c", c);
SyntaxHighlighter.registerLanguage("cpp", cpp);
SyntaxHighlighter.registerLanguage("c++", cpp);
SyntaxHighlighter.registerLanguage("javascript", javascript);
SyntaxHighlighter.registerLanguage("js", javascript);
SyntaxHighlighter.registerLanguage("typescript", typescript);
SyntaxHighlighter.registerLanguage("ts", typescript);
SyntaxHighlighter.registerLanguage("bash", bash);
SyntaxHighlighter.registerLanguage("sh", bash);
SyntaxHighlighter.registerLanguage("shell", bash);

const LANG_LABELS: Record<string, string> = {
  sql: "SQL",
  python: "Python",
  py: "Python",
  java: "Java",
  c: "C",
  cpp: "C++",
  "c++": "C++",
  javascript: "JavaScript",
  js: "JavaScript",
  typescript: "TypeScript",
  ts: "TypeScript",
  bash: "Shell",
  sh: "Shell",
  shell: "Shell",
};

interface CodeBlockProps {
  language: string | null;
  value: string;
}

function CodeBlock({ language, value }: CodeBlockProps) {
  const lang = (language ?? "").toLowerCase();
  const label = LANG_LABELS[lang] ?? (lang ? lang.toUpperCase() : "Code");
  const knownLang = lang in LANG_LABELS ? lang : "text";

  return (
    <div className="my-3 rounded-lg bg-zinc-900 overflow-hidden border border-zinc-800">
      <div className="flex items-center gap-2 px-4 py-2.5 border-b border-zinc-800">
        <span className="h-2.5 w-2.5 rounded-full bg-red-500/60" />
        <span className="h-2.5 w-2.5 rounded-full bg-yellow-500/60" />
        <span className="h-2.5 w-2.5 rounded-full bg-green-500/60" />
        <span className="ml-2 text-[11px] text-zinc-500 font-mono">
          {label}
        </span>
      </div>
      <SyntaxHighlighter
        language={knownLang}
        style={oneDark}
        PreTag="div"
        customStyle={{
          margin: 0,
          padding: "1rem",
          background: "transparent",
          fontSize: "0.875rem",
          lineHeight: 1.6,
        }}
        codeTagProps={{
          style: { fontFamily: "var(--font-jetbrains-mono), monospace" },
        }}
      >
        {value.replace(/\n$/, "")}
      </SyntaxHighlighter>
    </div>
  );
}

/**
 * 평문 SQL 블록을 감지해서 ```sql ... ``` 으로 자동 래핑.
 * 이미 펜스 안에 있는 코드는 건드리지 않는다.
 *
 * 배경: SQLD 레거시 문제는 SQL이 펜스 없이 평문으로 저장돼 있어
 * react-markdown이 일반 단락으로 렌더 → 신택스 하이라이팅 X.
 * 이 전처리로 모든 페이지에서 SQL이 코드 블록으로 표시됨.
 */
function ensureSqlFences(content: string): string {
  if (!content) return content;
  const lines = content.split("\n");
  const out: string[] = [];
  let i = 0;

  const SQL_START = /^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH|MERGE|TRUNCATE)\b/i;
  const SQL_CONT =
    /^\s*(FROM|WHERE|AND|OR|GROUP\s+BY|ORDER\s+BY|HAVING|JOIN|INNER\s+JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|FULL\s+JOIN|CROSS\s+JOIN|ON|UNION|UNION\s+ALL|INTERSECT|MINUS|EXCEPT|LIMIT|OFFSET|FETCH|VALUES|SET|RETURNING|WHEN|THEN|ELSE|END|CASE|INTO|USING|PARTITION\s+BY|WINDOW|RANGE|ROWS)\b/i;

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

    // 2) SQL 시작 키워드 감지 → 같은 블록으로 묶어서 fence
    if (SQL_START.test(line)) {
      const sqlLines: string[] = [line];
      i++;
      while (i < lines.length) {
        const next = lines[i];
        if (next.trim() === "") break;
        const isContinuation =
          SQL_CONT.test(next) ||
          /^\s{2,}/.test(next) ||
          /^\s*[(),]/.test(next) ||
          /^\s*(SELECT|INSERT|UPDATE|DELETE)\b/i.test(next);
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
        components={{
          code({ className, children, ...props }) {
            const match = /language-(\w+)/.exec(className ?? "");
            const value = String(children ?? "");
            // 블록 코드: className에 language- 가 있거나 줄바꿈 포함
            const isBlock = !!match || value.includes("\n");
            if (isBlock) {
              return (
                <CodeBlock
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
        }}
      >
        {ensureSqlFences(content)}
      </ReactMarkdown>
    </div>
  );
}
