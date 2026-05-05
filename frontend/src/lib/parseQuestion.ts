export interface ParsedQuestion {
  body: string;
  options: string[];
}

export const OPTION_MARKERS = ["①", "②", "③", "④"];

/**
 * 본문에서 ①②③④ 옵션을 분리한다.
 * - 옵션 마커 등장 이전 줄은 모두 본문(body)
 * - 옵션 마커로 시작하는 줄은 options 배열에 적재 (마커 제거)
 * - 옵션 마커로 시작하지 않지만 이미 옵션 수집이 시작됐다면 직전 옵션에 이어붙임 (들여쓰기 보존)
 * - 본문은 마크다운 그대로 보존하여 호출처에서 react-markdown으로 렌더한다.
 */
export function parseQuestion(content: string): ParsedQuestion {
  const lines = content.split("\n");
  const bodyLines: string[] = [];
  const options: string[] = [];

  for (const line of lines) {
    const trimmed = line.trim();
    if (OPTION_MARKERS.some((m) => trimmed.startsWith(m))) {
      options.push(trimmed.replace(/^[①②③④]\s*/, ""));
    } else if (options.length === 0) {
      bodyLines.push(line);
    } else {
      // 다중 라인 옵션: 들여쓰기·줄바꿈 보존하고 직전 옵션에 이어붙인다.
      // 정처기 필기처럼 보기 안에 코드 조각이 들어가는 케이스에 필요.
      options[options.length - 1] += "\n" + line;
    }
  }

  return { body: bodyLines.join("\n"), options };
}
