import fs from "fs";
import path from "path";

/**
 * 업데이트 내역 MDX 원본 로드.
 * 단일 파일 기반이라 파싱 없이 통째로 MDXRemote에 전달.
 * 새 엔트리는 content/CHANGELOG.mdx 최상단에 `## 기능 제목` 섹션으로 추가.
 */
export function getChangelogContent(): string {
  const filePath = path.join(process.cwd(), "content", "CHANGELOG.mdx");
  return fs.readFileSync(filePath, "utf8");
}
