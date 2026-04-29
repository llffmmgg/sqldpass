"use client";

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

interface QuestionCodeBlockProps {
  language: string | null;
  value: string;
}

export default function QuestionCodeBlock({ language, value }: QuestionCodeBlockProps) {
  const lang = (language ?? "").toLowerCase();
  const label = LANG_LABELS[lang] ?? (lang ? lang.toUpperCase() : "Code");
  const knownLang = lang in LANG_LABELS ? lang : "text";

  return (
    <div className="my-3 rounded-lg bg-zinc-900 overflow-hidden border border-zinc-800">
      <div className="flex items-center gap-2 px-4 py-2.5 border-b border-zinc-800">
        <span className="h-2.5 w-2.5 rounded-full bg-red-500/60" />
        <span className="h-2.5 w-2.5 rounded-full bg-yellow-500/60" />
        <span className="h-2.5 w-2.5 rounded-full bg-green-500/60" />
        <span className="ml-2 text-[11px] text-zinc-500 font-mono">{label}</span>
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
