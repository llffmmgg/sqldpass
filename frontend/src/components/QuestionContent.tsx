"use client";

import { type Segment } from "@/lib/parseQuestion";

const SQL_HIGHLIGHT =
  /\b(SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|JOIN|INNER|LEFT|RIGHT|OUTER|CROSS|NATURAL|GROUP\s+BY|ORDER\s+BY|HAVING|COUNT|SUM|AVG|MAX|MIN|UNION|CASE|WHEN|THEN|ELSE|END|AS|ON|AND|OR|IN|EXISTS|BETWEEN|LIKE|IS|NOT|NULL|DISTINCT|INTO|VALUES|SET|GRANT|REVOKE|COMMIT|ROLLBACK|TRUNCATE|SAVEPOINT|TABLE|INDEX|VIEW|PRIMARY|FOREIGN|KEY|REFERENCES|CHECK|DEFAULT|UNIQUE|CASCADE|LIMIT|OFFSET|ASC|DESC|ALL|OVER|PARTITION|ROW_NUMBER|RANK|DENSE_RANK|LAG|LEAD|ROLLUP|CUBE|GROUPING|PIVOT|UNPIVOT|CONNECT\s+BY|START\s+WITH|LEVEL|PRIOR|DECODE|NVL|COALESCE|SUBSTR|LENGTH|TRIM|UPPER|LOWER|TO_CHAR|TO_DATE|TO_NUMBER|SYSDATE|ROWNUM|DUAL|VARCHAR|INT|CHAR|DATE|NUMBER|FLOAT|BOOLEAN|TINYINT|COLUMN|RENAME|FUNCTION|TRIGGER|SEQUENCE|EXCEPT|INTERSECT|MINUS|FULL|USING|WINDOW|RANGE|PRECEDING|FOLLOWING|UNBOUNDED|CURRENT\s+ROW|FETCH|NEXT|ROWS|ONLY|WITH|MERGE|TOP)\b/gi;

function highlightSQL(code: string): React.ReactNode[] {
  const parts: React.ReactNode[] = [];
  let lastIndex = 0;
  const regex = new RegExp(SQL_HIGHLIGHT.source, "gi");
  let match;

  while ((match = regex.exec(code)) !== null) {
    if (match.index > lastIndex) {
      parts.push(code.slice(lastIndex, match.index));
    }
    parts.push(
      <span key={match.index} className="text-amber-400">
        {match[0]}
      </span>
    );
    lastIndex = regex.lastIndex;
  }
  if (lastIndex < code.length) {
    parts.push(code.slice(lastIndex));
  }
  return parts;
}

function SqlBlock({ content }: { content: string }) {
  return (
    <div className="my-3 rounded-lg bg-zinc-900 overflow-hidden border border-zinc-800">
      <div className="flex items-center gap-2 px-4 py-2.5 border-b border-zinc-800">
        <span className="h-2.5 w-2.5 rounded-full bg-red-500/60" />
        <span className="h-2.5 w-2.5 rounded-full bg-yellow-500/60" />
        <span className="h-2.5 w-2.5 rounded-full bg-green-500/60" />
        <span className="ml-2 text-[11px] text-zinc-500 font-mono">
          SQL Query
        </span>
      </div>
      <pre className="overflow-x-auto p-4 text-sm leading-relaxed text-zinc-300 font-mono">
        <code>{highlightSQL(content)}</code>
      </pre>
    </div>
  );
}

function InfoBlock({ content }: { content: string }) {
  return (
    <div className="my-3 rounded-r-lg border-l-2 border-amber-500/50 bg-zinc-900/50 px-3 py-2">
      <p className="text-sm text-muted font-mono">{content}</p>
    </div>
  );
}

function TextBlock({ content }: { content: string }) {
  return (
    <p className="text-sm font-medium leading-relaxed whitespace-pre-line">
      {content}
    </p>
  );
}

interface QuestionContentProps {
  segments: Segment[];
  className?: string;
}

export default function QuestionContent({
  segments,
  className,
}: QuestionContentProps) {
  return (
    <div className={className}>
      {segments.map((segment, i) => {
        switch (segment.type) {
          case "sql":
            return <SqlBlock key={i} content={segment.content} />;
          case "info":
            return <InfoBlock key={i} content={segment.content} />;
          case "text":
            return <TextBlock key={i} content={segment.content} />;
        }
      })}
    </div>
  );
}
