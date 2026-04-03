export type SegmentType = "text" | "sql" | "info";

export interface Segment {
  type: SegmentType;
  content: string;
}

export interface ParsedQuestion {
  segments: Segment[];
  options: string[];
}

export const OPTION_MARKERS = ["①", "②", "③", "④"];

const SQL_STATEMENT_STARTERS =
  /^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|GRANT|REVOKE|TRUNCATE|WITH|MERGE)\b/i;

const SQL_KEYWORDS =
  /\b(SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|JOIN|INNER|LEFT|RIGHT|OUTER|CROSS|NATURAL|GROUP\s+BY|ORDER\s+BY|HAVING|COUNT|SUM|AVG|MAX|MIN|UNION|CASE|WHEN|THEN|ELSE|END|AS|ON|AND|OR|IN|EXISTS|BETWEEN|LIKE|IS|NOT|NULL|DISTINCT|INTO|VALUES|SET|GRANT|REVOKE|COMMIT|ROLLBACK|TRUNCATE|SAVEPOINT|TABLE|INDEX|VIEW|CONSTRAINT|PRIMARY|FOREIGN|KEY|REFERENCES|CHECK|DEFAULT|UNIQUE|CASCADE|RESTRICT|LIMIT|OFFSET|ASC|DESC|ALL|ANY|SOME|TOP|FETCH|NEXT|ROWS|ONLY|OVER|PARTITION|ROW_NUMBER|RANK|DENSE_RANK|LAG|LEAD|ROLLUP|CUBE|GROUPING|PIVOT|UNPIVOT|CONNECT\s+BY|START\s+WITH|LEVEL|PRIOR|DECODE|NVL|COALESCE|SUBSTR|LENGTH|TRIM|UPPER|LOWER|TO_CHAR|TO_DATE|TO_NUMBER|SYSDATE|ROWNUM|ROWID|DUAL|VARCHAR|INT|BIGINT|CHAR|DATE|NUMBER|FLOAT|DECIMAL|BOOLEAN|TEXT|DATETIME|TINYINT|MODIFY|ADD|COLUMN|RENAME|IF|REPLACE|PROCEDURE|FUNCTION|TRIGGER|SEQUENCE|SYNONYM|WITH\s+RECURSIVE|EXCEPT|INTERSECT|MINUS|FULL|USING|RECURSIVE|WINDOW|RANGE|PRECEDING|FOLLOWING|UNBOUNDED|CURRENT\s+ROW)\b/gi;

const INFO_PATTERN = /^\s*[\[(（].*(?:테이블|TABLE|결과|조건|가정|참고|주어진|데이터).*[\])）]?\s*$/i;

function isSqlLine(line: string): boolean {
  if (SQL_STATEMENT_STARTERS.test(line)) return true;
  const matches = line.match(SQL_KEYWORDS);
  return matches !== null && matches.length >= 2;
}

function isInfoLine(line: string): boolean {
  return INFO_PATTERN.test(line);
}

function classifyLines(lines: string[]): Segment[] {
  const segments: Segment[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    if (isSqlLine(line)) {
      const sqlLines: string[] = [line];
      i++;
      while (i < lines.length && isSqlLine(lines[i])) {
        sqlLines.push(lines[i]);
        i++;
      }
      segments.push({ type: "sql", content: sqlLines.join("\n") });
    } else if (isInfoLine(line)) {
      segments.push({ type: "info", content: line.trim() });
      i++;
    } else {
      const textLines: string[] = [line];
      i++;
      while (i < lines.length && !isSqlLine(lines[i]) && !isInfoLine(lines[i])) {
        textLines.push(lines[i]);
        i++;
      }
      segments.push({ type: "text", content: textLines.join("\n") });
    }
  }

  return segments;
}

export function parseQuestion(content: string): ParsedQuestion {
  const lines = content.split("\n").filter((l) => l.trim());
  const bodyLines: string[] = [];
  const options: string[] = [];

  for (const line of lines) {
    if (OPTION_MARKERS.some((m) => line.trim().startsWith(m))) {
      options.push(line.trim().replace(/^[①②③④]\s*/, ""));
    } else if (options.length === 0) {
      bodyLines.push(line);
    }
  }

  const segments = classifyLines(bodyLines);

  return { segments, options };
}
