// 정보처리기사 실기 샘플 JSON → Flyway seed SQL 변환.
// 사용: node scripts/generate-engineer-seed.mjs <input.json> <output.sql>
// 예:  node scripts/generate-engineer-seed.mjs 정보처리기사_실기_샘플문제_27.json backend/src/main/resources/db/migration/V15__engineer_practical_seed.sql

import { readFileSync, writeFileSync } from "node:fs";

const [, , inputPath, outputPath] = process.argv;
if (!inputPath || !outputPath) {
  console.error("usage: node generate-engineer-seed.mjs <input.json> <output.sql>");
  process.exit(1);
}

const raw = readFileSync(inputPath, "utf-8");
const items = JSON.parse(raw);

// MySQL 문자열 이스케이프 — 작은따옴표 + 백슬래시 + NUL
function esc(s) {
  if (s === null || s === undefined) return "NULL";
  return "'" + String(s).replace(/\\/g, "\\\\").replace(/'/g, "\\'").replace(/\x00/g, "\\0") + "'";
}

function escJson(arr) {
  if (!arr || arr.length === 0) return "NULL";
  // JSON.stringify + MySQL 이스케이프
  const json = JSON.stringify(arr);
  return esc(json);
}

// subject 조회 서브쿼리 — 정보처리기사 실기 아래의 카테고리
function subjectSubquery(category) {
  return `(SELECT id FROM subject WHERE name = ${esc(category)} AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s))`;
}

const lines = [];
lines.push("-- 정보처리기사 실기 샘플 문제 시드 (자동 생성)");
lines.push("-- Source: scripts/generate-engineer-seed.mjs");
lines.push(`-- 총 ${items.length}문항`);
lines.push("");

for (const q of items) {
  const cols = [
    "subject_id",
    "content",
    "question_type",
    "correct_option",
    "answer",
    "keywords",
    "explanation",
    "summary",
    "topic",
    "difficulty",
    "created_at",
    "updated_at",
  ];
  const vals = [
    subjectSubquery(q.category),
    esc(q.content),
    esc(q.questionType),
    "NULL", // correct_option — 비MCQ
    esc(q.answer),
    escJson(q.keywords),
    esc(q.explanation),
    esc(q.summary),
    esc(q.topic),
    String(q.difficulty),
    "NOW(6)",
    "NOW(6)",
  ];
  lines.push(`-- id=${q.id} ${q.category} / ${q.topic}`);
  lines.push(`INSERT INTO question (${cols.join(", ")}) VALUES`);
  lines.push(`    (${vals.join(", ")});`);
  lines.push("");
}

writeFileSync(outputPath, lines.join("\n"), "utf-8");
console.log(`✓ ${items.length} questions written to ${outputPath}`);
