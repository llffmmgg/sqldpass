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

// SQL 시작 키워드 (한 줄로 시작하는 경우 SQL 블록 시작으로 인식)
const SQL_STATEMENT_STARTERS =
  /^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|GRANT|REVOKE|TRUNCATE|WITH|MERGE|EXPLAIN|COMMIT|ROLLBACK|SAVEPOINT)\b/i;

// SQL 본문 안에 자주 나오는 줄 시작 키워드 (SELECT 등 시작 후 이어지는 줄)
const SQL_CONTINUATION_STARTERS =
  /^\s*(FROM|WHERE|AND|OR|GROUP\s+BY|ORDER\s+BY|HAVING|JOIN|INNER\s+JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|FULL\s+JOIN|CROSS\s+JOIN|ON|UNION|UNION\s+ALL|INTERSECT|EXCEPT|MINUS|LIMIT|OFFSET|FETCH|VALUES|SET|RETURNING|WHEN|THEN|ELSE|END|CASE|INTO|USING|PARTITION\s+BY|WINDOW)\b/i;

// 명백히 텍스트성으로 시작하는 줄 (한국어, 숫자.목록 등)
const PROBABLY_TEXT =
  /^\s*([가-힣]|[0-9]+[.)]|[-•*]\s|문제|해설|설명|아래|다음|위|보기|정답|풀이|참고)/;

// 정보 블록 (대괄호로 감싼 메타 설명)
const INFO_PATTERN =
  /^\s*[\[(（].*(?:테이블|TABLE|결과|조건|가정|참고|주어진|데이터).*[\])）]?\s*$/i;

function isSqlStartLine(line: string): boolean {
  return SQL_STATEMENT_STARTERS.test(line);
}

function isSqlContinuationLine(line: string): boolean {
  if (SQL_CONTINUATION_STARTERS.test(line)) return true;
  // 들여쓰기된 줄 (SQL 본문 내부 들여쓰기 가능성)
  if (/^\s{2,}/.test(line) && !PROBABLY_TEXT.test(line)) return true;
  return false;
}

function isInfoLine(line: string): boolean {
  return INFO_PATTERN.test(line);
}

/**
 * 마크다운 ```sql ... ``` 블록을 먼저 추출.
 * 매칭되는 영역은 SQL segment, 나머지는 raw text로 반환.
 */
function extractFencedBlocks(content: string): Segment[] {
  const fenceRegex = /```(?:sql|SQL)?\s*\n?([\s\S]*?)```/g;
  const result: Segment[] = [];
  let lastIdx = 0;
  let match;

  while ((match = fenceRegex.exec(content)) !== null) {
    if (match.index > lastIdx) {
      const before = content.slice(lastIdx, match.index);
      if (before.trim()) {
        result.push(...classifyByLines(before));
      }
    }
    result.push({ type: "sql", content: match[1].trim() });
    lastIdx = match.index + match[0].length;
  }

  if (lastIdx < content.length) {
    const rest = content.slice(lastIdx);
    if (rest.trim()) {
      result.push(...classifyByLines(rest));
    }
  }

  return result.length > 0 ? result : classifyByLines(content);
}

/**
 * 줄 단위 휴리스틱 분류 (fenced block 없는 경우):
 * - SQL 시작 키워드를 만나면 그 줄부터 SQL 블록 시작
 * - SQL 블록은 ; 만나거나 빈 줄 만나거나 명백한 텍스트 줄을 만날 때까지 계속
 * - 빈 줄은 SQL 블록 종료 신호 + 단락 구분
 */
function classifyByLines(content: string): Segment[] {
  const lines = content.split("\n");
  const segments: Segment[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // 빈 줄은 그냥 스킵
    if (!line.trim()) {
      i++;
      continue;
    }

    // SQL 시작 줄
    if (isSqlStartLine(line)) {
      const sqlLines: string[] = [line];
      let endedBySemicolon = /;\s*$/.test(line);
      i++;
      while (i < lines.length && !endedBySemicolon) {
        const next = lines[i];
        if (!next.trim()) {
          // 빈 줄 → SQL 블록 종료
          break;
        }
        if (
          isSqlStartLine(next) ||
          isSqlContinuationLine(next) ||
          /^\s*[(),]/.test(next) ||  // 괄호/콤마 시작
          !PROBABLY_TEXT.test(next)
        ) {
          sqlLines.push(next);
          endedBySemicolon = /;\s*$/.test(next);
          i++;
        } else {
          break;
        }
      }
      segments.push({ type: "sql", content: sqlLines.join("\n").trim() });
      continue;
    }

    // 정보 블록
    if (isInfoLine(line)) {
      segments.push({ type: "info", content: line.trim() });
      i++;
      continue;
    }

    // 일반 텍스트 (다음 SQL 시작/정보 줄까지 모음)
    const textLines: string[] = [line];
    i++;
    while (
      i < lines.length &&
      lines[i].trim() &&
      !isSqlStartLine(lines[i]) &&
      !isInfoLine(lines[i])
    ) {
      textLines.push(lines[i]);
      i++;
    }
    segments.push({ type: "text", content: textLines.join("\n") });
  }

  return segments;
}

export function parseQuestion(content: string): ParsedQuestion {
  // 1) 옵션과 본문 분리
  const lines = content.split("\n");
  const bodyLines: string[] = [];
  const options: string[] = [];

  for (const line of lines) {
    const trimmed = line.trim();
    if (OPTION_MARKERS.some((m) => trimmed.startsWith(m))) {
      options.push(trimmed.replace(/^[①②③④]\s*/, ""));
    } else if (options.length === 0) {
      bodyLines.push(line);
    }
  }

  // 2) 본문에서 fenced SQL 블록 + 휴리스틱 분류
  const segments = extractFencedBlocks(bodyLines.join("\n"));

  return { segments, options };
}

/** 해설 등 일반 마크다운 텍스트를 segments로 파싱 (옵션 추출 없음) */
export function parseMarkdown(content: string): Segment[] {
  return extractFencedBlocks(content);
}
