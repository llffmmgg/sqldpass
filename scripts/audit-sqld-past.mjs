#!/usr/bin/env node
// SQLD 기출복원 문제 전수조사
// V43~V89 의 SQLD past_exam 마이그레이션을 정적 파싱해 최종 상태를 재현하고,
// 1) 선택지 불완전 2) 정답 placeholder 3) 이미지 누락 4) 풀이 조건 빈약 5) 본문 결함
// 5개 항목을 검사해 문제 있는 항목만 데스크탑에 md 로 출력한다.

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO = path.resolve(__dirname, "..");
const MIG_DIR = path.join(REPO, "backend/src/main/resources/db/migration");
const IMG_DIR = path.join(REPO, "frontend/public/exam-images");
const OUT = "C:\\Users\\admin\\Desktop\\sqld-past-exam-audit.md";

const INSERT_FILES = [
  ["V43__past_exam_sqld_56.sql", 56],
  ["V46__past_exam_sqld_55.sql", 55],
  ["V47__past_exam_sqld_52.sql", 52],
  ["V48__past_exam_sqld_57.sql", 57],
  ["V49__past_exam_sqld_53.sql", 53],
  ["V50__past_exam_sqld_54.sql", 54],
];

const UPDATE_FILES = [
  ["V44__past_exam_sqld_56_erd_images.sql", 56],
  ["V45__past_exam_sqld_56_cleanup_memos.sql", 56],
  ["V66__past_exam_sqld_56_reconstruct_missing_questions.sql", 56],
  ["V67__past_exam_sqld_56_drop_reconstruction_notice.sql", 56],
  ["V69__past_exam_sqld_subject_fix.sql", null],
  ["V89__fix_past_exam_sqld_55_q09_blanks.sql", 55],
  ["V95__fix_past_exam_sqld_56_q47_q48.sql", 56],
];

// ─────────────────────────────────────────────────────────────────────────────
// SQL parser (충분히 작고 본 마이그레이션 포맷에만 맞춰진 미니멀 파서)

function stripComments(sql) {
  // SQL line comment: -- starts a comment only if NOT inside a string literal
  // AND it's at start of line or preceded by whitespace (to avoid matching `|---|---|` in markdown).
  let out = "";
  let i = 0;
  let inStr = false;
  while (i < sql.length) {
    const c = sql[i];
    if (inStr) {
      out += c;
      if (c === "\\") { if (i + 1 < sql.length) out += sql[i + 1]; i += 2; continue; }
      if (c === "'") {
        if (sql[i + 1] === "'") { out += "'"; i += 2; continue; }
        inStr = false;
      }
      i++;
      continue;
    }
    if (c === "'") { inStr = true; out += c; i++; continue; }
    if (c === "-" && sql[i + 1] === "-") {
      const prev = i === 0 ? "\n" : sql[i - 1];
      if (prev === "\n" || prev === " " || prev === "\t" || prev === "\r") {
        // skip until newline
        while (i < sql.length && sql[i] !== "\n") i++;
        continue;
      }
    }
    out += c;
    i++;
  }
  return out;
}

// 위치 i 가 따옴표 안인지 추적하며 토큰 단위로 진행.
class Tok {
  constructor(sql) {
    this.s = sql;
    this.i = 0;
  }
  eof() { return this.i >= this.s.length; }
  peek(n = 0) { return this.s[this.i + n]; }
  rest() { return this.s.slice(this.i, this.i + 40); }
  skipWS() {
    while (this.i < this.s.length && /\s/.test(this.s[this.i])) this.i++;
  }
  match(re) {
    this.skipWS();
    re.lastIndex = this.i;
    const m = re.exec(this.s);
    if (!m || m.index !== this.i) return null;
    this.i = re.lastIndex;
    return m;
  }
  expect(ch) {
    this.skipWS();
    if (this.s[this.i] !== ch) {
      throw new Error(`expected '${ch}' at ${this.i}, got '${this.s[this.i]}' (near: ${this.rest()})`);
    }
    this.i++;
  }
  // single-quoted string with '' doubling AND backslash escapes
  readString() {
    this.skipWS();
    if (this.s[this.i] !== "'") throw new Error(`expected string at ${this.i} (near: ${this.rest()})`);
    this.i++;
    let out = "";
    while (this.i < this.s.length) {
      const c = this.s[this.i];
      if (c === "'") {
        if (this.s[this.i + 1] === "'") { out += "'"; this.i += 2; continue; }
        this.i++;
        return out;
      }
      if (c === "\\") {
        const n = this.s[this.i + 1];
        if (n === "n") { out += "\n"; this.i += 2; continue; }
        if (n === "t") { out += "\t"; this.i += 2; continue; }
        if (n === "r") { out += "\r"; this.i += 2; continue; }
        if (n === "\\") { out += "\\"; this.i += 2; continue; }
        if (n === "'") { out += "'"; this.i += 2; continue; }
        if (n === "0") { out += "\0"; this.i += 2; continue; }
        out += n; this.i += 2; continue;
      }
      out += c; this.i++;
    }
    throw new Error("unterminated string");
  }
  // value: string | NULL | integer | IDENT | IDENT(...) — 우리는 string/integer 만 정확히 보면 됨.
  readValue() {
    this.skipWS();
    const c = this.s[this.i];
    if (c === "'") return { kind: "str", value: this.readString() };
    // identifier or function call (e.g. NOW(6), @mock_exam_id, NULL, ROLLUP(...) — but we just skip)
    if (/[A-Za-z@_]/.test(c)) {
      const start = this.i;
      while (this.i < this.s.length && /[A-Za-z0-9_@]/.test(this.s[this.i])) this.i++;
      const ident = this.s.slice(start, this.i);
      // optional function call
      this.skipWS();
      if (this.s[this.i] === "(") {
        // consume balanced parens
        let depth = 0;
        while (this.i < this.s.length) {
          const ch = this.s[this.i];
          if (ch === "(") depth++;
          else if (ch === ")") { depth--; this.i++; if (depth === 0) break; continue; }
          else if (ch === "'") this.readString() && (this.i--, this.i++); // consume string
          this.i++;
        }
      }
      if (ident.toUpperCase() === "NULL") return { kind: "null" };
      return { kind: "ident", name: ident };
    }
    // integer (possibly negative)
    if (/[-0-9]/.test(c)) {
      const start = this.i;
      if (c === "-") this.i++;
      while (this.i < this.s.length && /[0-9]/.test(this.s[this.i])) this.i++;
      return { kind: "int", value: parseInt(this.s.slice(start, this.i), 10) };
    }
    throw new Error(`unknown value at ${this.i}: '${c}' (near: ${this.rest()})`);
  }
}

// Find all INSERT INTO question (...) VALUES (...), (...), ...; blocks
function parseInsertQuestion(sql) {
  // 우리가 다루는 V43, V46-V50 은 "INSERT INTO question ... VALUES (..), (..), ...;" 패턴 1개.
  const re = /INSERT\s+INTO\s+question\s*\(([^)]+)\)\s*VALUES\s*/gi;
  const m = re.exec(sql);
  if (!m) return [];
  const cols = m[1].split(",").map((s) => s.trim());
  const tok = new Tok(sql);
  tok.i = re.lastIndex;
  const rows = [];
  while (!tok.eof()) {
    tok.skipWS();
    if (tok.s[tok.i] !== "(") break;
    tok.expect("(");
    const values = [];
    while (true) {
      tok.skipWS();
      values.push(tok.readValue());
      tok.skipWS();
      if (tok.s[tok.i] === ",") { tok.i++; continue; }
      if (tok.s[tok.i] === ")") { tok.i++; break; }
      throw new Error(`expected , or ) at ${tok.i} (near: ${tok.rest()})`);
    }
    const row = {};
    for (let k = 0; k < cols.length; k++) row[cols[k]] = values[k];
    rows.push(row);
    tok.skipWS();
    if (tok.s[tok.i] === ",") { tok.i++; continue; }
    if (tok.s[tok.i] === ";") break;
    break;
  }
  return rows;
}

// Parse UPDATE question SET <assignments> WHERE mock_exam_id = @... AND display_order = N
// Supports:
//   col = '<string>'
//   col = REPLACE(col, '<from>', '<to>')   (nested OK)
//   col = REPLACE(REPLACE(...), '<from>', '<to>')
//   col = <integer>
//   col = NOW(6)   (ignored)
// Returns array of { round, display_order, assignments: { col: (current) => newValue } }
function parseUpdateBlocks(sql, defaultRound) {
  const updates = [];
  const re = /UPDATE\s+question\s+SET\s+/gi;
  let m;
  while ((m = re.exec(sql))) {
    const startIdx = m.index + m[0].length;
    // find the matching WHERE ... ; (string-aware scan)
    let i = startIdx;
    let displayOrder = null;
    let inStr = false;
    let escape = false;
    let endIdx = -1;
    while (i < sql.length) {
      const c = sql[i];
      if (inStr) {
        if (escape) { escape = false; i++; continue; }
        if (c === "\\") { escape = true; i++; continue; }
        if (c === "'") {
          if (sql[i + 1] === "'") { i += 2; continue; }
          inStr = false; i++; continue;
        }
        i++; continue;
      }
      if (c === "'") { inStr = true; i++; continue; }
      if (c === ";") { endIdx = i; break; }
      i++;
    }
    if (endIdx === -1) break;
    const stmtBody = sql.slice(startIdx, endIdx);
    // split SET and WHERE
    // Find " WHERE " not inside string. We can use indexOf safely since we already excluded strings? No — we need string-aware split.
    const whereIdx = findUnquoted(stmtBody, /\bWHERE\b/i);
    if (whereIdx === -1) { re.lastIndex = endIdx + 1; continue; }
    const setPart = stmtBody.slice(0, whereIdx);
    const wherePart = stmtBody.slice(whereIdx);
    const doMatch = wherePart.match(/display_order\s*=\s*(\d+)/i);
    if (!doMatch) { re.lastIndex = endIdx + 1; continue; }
    displayOrder = parseInt(doMatch[1], 10);
    // exam_round override (e.g. when same file targets multiple rounds — handled in this file but V44/45/66/67 target single round via SET @mid)
    // For our INSERT files we set the round from filename mapping.
    const assigns = parseAssignments(setPart);
    updates.push({ round: defaultRound, display_order: displayOrder, assigns });
    re.lastIndex = endIdx + 1;
  }
  return updates;
}

function findUnquoted(s, re) {
  // walk s with quote awareness; return first match index of re in unquoted regions
  let i = 0;
  let inStr = false;
  while (i < s.length) {
    if (inStr) {
      if (s[i] === "\\") { i += 2; continue; }
      if (s[i] === "'") {
        if (s[i + 1] === "'") { i += 2; continue; }
        inStr = false; i++; continue;
      }
      i++; continue;
    }
    if (s[i] === "'") { inStr = true; i++; continue; }
    const sub = s.slice(i, i + 20);
    const m = sub.match(re);
    if (m && m.index === 0) return i;
    i++;
  }
  return -1;
}

// 매우 단순한 assignment 파서: "col = expr" 를 콤마로 분리. 콤마는 따옴표/괄호 밖에서만 분리자.
function parseAssignments(setPart) {
  const parts = splitTopLevelCommas(setPart);
  const out = [];
  for (const p of parts) {
    const trimmed = p.trim();
    if (!trimmed) continue;
    const eq = findUnquotedChar(trimmed, "=");
    if (eq === -1) continue;
    const col = trimmed.slice(0, eq).trim();
    const expr = trimmed.slice(eq + 1).trim();
    out.push({ col, expr });
  }
  return out;
}

function splitTopLevelCommas(s) {
  const parts = [];
  let depth = 0;
  let inStr = false;
  let start = 0;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (inStr) {
      if (c === "\\") { i++; continue; }
      if (c === "'") {
        if (s[i + 1] === "'") { i++; continue; }
        inStr = false;
      }
      continue;
    }
    if (c === "'") { inStr = true; continue; }
    if (c === "(") depth++;
    else if (c === ")") depth--;
    else if (c === "," && depth === 0) {
      parts.push(s.slice(start, i));
      start = i + 1;
    }
  }
  parts.push(s.slice(start));
  return parts;
}

function findUnquotedChar(s, ch) {
  let inStr = false;
  let depth = 0;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (inStr) {
      if (c === "\\") { i++; continue; }
      if (c === "'") {
        if (s[i + 1] === "'") { i++; continue; }
        inStr = false;
      }
      continue;
    }
    if (c === "'") { inStr = true; continue; }
    if (c === "(") depth++;
    else if (c === ")") depth--;
    else if (c === ch && depth === 0) return i;
  }
  return -1;
}

// Evaluate an assignment expression given current row state. Returns the new value, or undefined to skip.
function evalExpr(expr, currentColValue) {
  const e = expr.trim();
  if (/^NULL$/i.test(e)) return null;
  if (/^NOW\s*\(/i.test(e)) return undefined; // ignore timestamps
  if (/^-?\d+$/.test(e)) return parseInt(e, 10);
  // string literal
  if (e.startsWith("'")) {
    const tok = new Tok(e);
    return tok.readString();
  }
  // REPLACE(...) — possibly nested
  if (/^REPLACE\s*\(/i.test(e)) {
    return evalReplace(e, currentColValue);
  }
  // bare identifier referring to current column (e.g. "content")
  if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(e)) {
    return currentColValue;
  }
  // fallback: ignore
  return undefined;
}

// Parse REPLACE(arg1, arg2, arg3) where arg1 can be another REPLACE or bare identifier.
function evalReplace(expr, currentColValue) {
  // Find arguments: REPLACE( <a1>, <a2>, <a3> )
  // strip outer "REPLACE(" ... ")"
  const m = expr.match(/^REPLACE\s*\(/i);
  if (!m) return undefined;
  let i = m[0].length;
  // collect 3 args by walking with paren/string awareness
  const args = [];
  let start = i;
  let depth = 1;
  let inStr = false;
  while (i < expr.length) {
    const c = expr[i];
    if (inStr) {
      if (c === "\\") { i += 2; continue; }
      if (c === "'") {
        if (expr[i + 1] === "'") { i += 2; continue; }
        inStr = false; i++; continue;
      }
      i++; continue;
    }
    if (c === "'") { inStr = true; i++; continue; }
    if (c === "(") { depth++; i++; continue; }
    if (c === ")") {
      depth--;
      if (depth === 0) { args.push(expr.slice(start, i).trim()); i++; break; }
      i++; continue;
    }
    if (c === "," && depth === 1) {
      args.push(expr.slice(start, i).trim());
      start = i + 1;
      i++; continue;
    }
    i++;
  }
  if (args.length !== 3) return undefined;
  const subject = evalExpr(args[0], currentColValue);
  const from = evalExpr(args[1], currentColValue);
  const to = evalExpr(args[2], currentColValue);
  if (typeof subject !== "string" || typeof from !== "string" || typeof to !== "string") return undefined;
  return subject.split(from).join(to);
}

// ─────────────────────────────────────────────────────────────────────────────
// 메인: 마이그레이션 적용 → 최종 questions 맵

function readMig(name) {
  return stripComments(fs.readFileSync(path.join(MIG_DIR, name), "utf8"));
}

const questions = new Map(); // key = `${round}/${display_order}` → { round, display_order, content, correct_option, explanation, question_type, topic }

for (const [name, round] of INSERT_FILES) {
  const sql = readMig(name);
  const rows = parseInsertQuestion(sql);
  for (const row of rows) {
    const display = row.display_order.value;
    const key = `${round}/${display}`;
    questions.set(key, {
      round,
      display_order: display,
      content: row.content.value,
      correct_option: row.correct_option.kind === "int" ? row.correct_option.value : null,
      explanation: row.explanation.value,
      question_type: row.question_type.value,
      topic: row.topic.kind === "str" ? row.topic.value : null,
    });
  }
}

for (const [name, defaultRound] of UPDATE_FILES) {
  const sql = readMig(name);
  if (defaultRound === null) continue; // V69 — subject_id only, irrelevant for our audit
  const updates = parseUpdateBlocks(sql, defaultRound);
  for (const u of updates) {
    const key = `${u.round}/${u.display_order}`;
    const q = questions.get(key);
    if (!q) continue;
    for (const a of u.assigns) {
      const newVal = evalExpr(a.expr, q[a.col]);
      if (newVal === undefined) continue;
      q[a.col] = newVal;
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// 감사 로직

function stripCode(content) {
  // remove fenced code blocks, inline backticks, markdown image links, markdown table rows, html-ish tags
  let s = content;
  s = s.replace(/```[\s\S]*?```/g, " ");
  s = s.replace(/!\[[^\]]*\]\([^)]*\)/g, " "); // images
  s = s.replace(/`[^`]*`/g, " ");
  s = s.replace(/<svg[\s\S]*?<\/svg>/g, " ");
  s = s.replace(/\|[^\n]*\|/g, " "); // table rows
  return s;
}

function bodyText(content) {
  // content 에서 보기(① ② ③ ④) 이후, 코드/이미지/표 제거 후, 본문(질문 stem) 만 남긴 길이 추정.
  const idxOptions = content.search(/[①②③④]/);
  const stem = idxOptions >= 0 ? content.slice(0, idxOptions) : content;
  return stripCode(stem).replace(/\s+/g, " ").trim();
}

function auditQuestion(q) {
  const flags = [];
  const c = q.content;
  // stem 만 발췌 — 보기(① 이후) 제외
  const stemIdx = c.search(/[①②③④]/);
  const stem = stemIdx >= 0 ? c.slice(0, stemIdx) : c;

  // 1) 선택지 불완전 (MCQ 만)
  if (q.question_type === "MCQ") {
    const markers = ["①", "②", "③", "④"];
    const counts = markers.map((m) => (c.match(new RegExp(m, "g")) || []).length);
    const missing = markers.filter((_, i) => counts[i] === 0);
    const dup = markers.filter((_, i) => counts[i] > 1);
    if (missing.length > 0) flags.push({ kind: "선택지 불완전", detail: `누락된 마커: ${missing.join(" ")}` });
    else if (dup.length > 0) flags.push({ kind: "선택지 불완전", detail: `중복 마커: ${dup.join(" ")}` });
    else {
      const optTexts = [];
      for (let i = 0; i < 4; i++) {
        const start = c.indexOf(markers[i]);
        const nextStart = i < 3 ? c.indexOf(markers[i + 1], start + 1) : c.length;
        const txt = c.slice(start + 1, nextStart === -1 ? c.length : nextStart).trim();
        optTexts.push(txt);
      }
      const blank = optTexts.filter((t) => t.length === 0);
      if (blank.length > 0) flags.push({ kind: "선택지 불완전", detail: `빈 선택지 ${blank.length}개` });
      const seen = new Map();
      const dupText = new Set();
      for (let i = 0; i < 4; i++) {
        const t = optTexts[i].replace(/\s+/g, " ").trim();
        if (!t) continue;
        if (seen.has(t)) dupText.add(t);
        seen.set(t, i);
      }
      if (dupText.size > 0) flags.push({ kind: "선택지 불완전", detail: `동일 선택지 텍스트` });
    }
  }

  // 2) 정답 placeholder — explanation 에 ⚠️ 또는 "정답 재확인" / "정답 미상"
  if (/⚠️/.test(q.explanation) || /정답\s*(재확인|미상)/.test(q.explanation)) {
    flags.push({ kind: "정답 placeholder", detail: q.explanation.split("\n")[0].slice(0, 120) });
  }

  // 3) 이미지 누락
  const imgRe = /!\[[^\]]*\]\(([^)]+)\)/g;
  let im;
  const imgPaths = [];
  while ((im = imgRe.exec(c))) imgPaths.push(im[1]);
  // 3a) markdown 이미지 링크가 있으면 실파일 존재 확인
  for (const p of imgPaths) {
    if (!p.startsWith("/exam-images/")) continue;
    const rel = p.replace(/^\//, "");
    const full = path.join(REPO, "frontend/public", rel);
    if (!fs.existsSync(full)) {
      flags.push({ kind: "이미지 누락", detail: `참조 \`${p}\` 실파일 없음` });
    }
  }
  // 3b) [그림: ...] 텍스트 placeholder 가 남아있는 경우
  const fig = c.match(/\[그림:\s*([^\]]+)\]/);
  if (fig) flags.push({ kind: "이미지 누락", detail: `텍스트 placeholder 남음: [그림: ${fig[1].slice(0, 60)}...]` });
  // 3c) stem 이 시각자료 참조하는데 이미지/SVG/표/구조 블록이 전혀 없음
  const refsVisual = /(다음\s*ERD|주어진\s*ERD|다음\s*그림|다음\s*다이어그램|아래\s*그림|위\s*그림|다음\s*테이블\s*구조)/.test(stem);
  // **<...>** 블록은 텍스트로 ERD/조건/표를 명시한 구조 블록으로 간주
  const hasStructBlock = /\*\*<[^>]+>\*\*/.test(c);
  const hasVisual = imgPaths.length > 0 || /<svg/.test(c) || /\|[^\n]*\|/.test(c) || hasStructBlock;
  if (refsVisual && !hasVisual) {
    flags.push({ kind: "이미지 누락", detail: `stem 이 시각자료 참조하나 이미지/SVG/표/구조 블록 없음` });
  }

  // 4) 풀이 조건 빈약 — stem 이 지시 표현(다음/위/아래/주어진)으로 외부 자료를 참조하지만 동반 자산 없음.
  // 한글 합성어 안의 "위/아래"(순위, 하위 등) 가 잘못 매칭되지 않도록 한글 경계 lookbehind 적용.
  const refsAsset = /(?<![가-힣])(다음|위|아래|주어진)\s*(SQL|쿼리|결과|표|코드|UPDATE|INSERT|DELETE|SELECT|함수)/.test(stem);
  const hasCode = /```/.test(c);
  const hasTable = /\|[^\n]+\|/.test(c);
  const hasInlineCode = /`[^`\n]+`/.test(c);
  if (refsAsset && !hasCode && !hasTable && !hasInlineCode && !imgPaths.length && !hasStructBlock) {
    flags.push({ kind: "풀이 조건 빈약", detail: `stem 이 자료 참조하나 코드/표/이미지/구조 블록 없음` });
  }

  // 5) 작업 메모 흔적 — content/explanation 에 [복원 메모, **복원 보강 안내**, TODO, 확인필요, FIXME 등 사후 작업 표시가 남은 경우
  const memoMarkers = [
    /\[복원\s*메모/,
    /\*\*복원\s*보강\s*안내\*\*/,
    /확인\s*필요/,
    /\bTODO\b/i,
    /\bFIXME\b/i,
  ];
  for (const re of memoMarkers) {
    if (re.test(c) || re.test(q.explanation)) {
      flags.push({ kind: "작업 메모 흔적", detail: `최종 상태에 작업 메모 잔존: ${re.source}` });
      break;
    }
  }

  // 6) 안내 blockquote — V45 가 삽입한 `> ⚠️ **안내**: ...` 가 V66 으로 덮이지 못해 남아있는 경우
  if (/^>\s*⚠️\s*\*\*안내/m.test(c)) {
    flags.push({ kind: "복원 안내 잔존", detail: `content 에 '> ⚠️ **안내**' 블록쿼트 잔존` });
  }

  // 7) 메타 서술 stem — V45 가 [복원 메모] 만 제거하면서 본문이 메타 요약("...문제", "...관련 문제 중 ...")
  // 상태로 남은 케이스. 정상 SQLD 문제는 의문문(?) 형태로 끝난다.
  // 보기에 SQL 키워드나 절명이 하나도 없고, 보기가 절 수정 지시이면 원본 SQL 부재로 풀이 불가.
  const stemTrimmed = stem.trim();
  const stemEndsWithMemoTitle = /(문제|첫\s*번째|두\s*번째|세\s*번째|네\s*번째)\s*$/.test(stemTrimmed);
  const stemHasQuestionMark = /[?？]/.test(stemTrimmed);
  if (stemEndsWithMemoTitle && !stemHasQuestionMark) {
    const optionsRefClause = /(절을\s*수정|절을\s*추가|절을\s*삭제|절에\s*DESC\s*를?\s*추가)/.test(c);
    const optionsHaveSqlKeyword = /(SELECT|FROM|WHERE|JOIN|UPDATE|INSERT|DELETE|GROUP\s*BY|ORDER\s*BY|HAVING)/i.test(c);
    if (optionsRefClause) {
      flags.push({ kind: "메타 서술 stem", detail: `stem 이 의문문 아닌 메타 요약 ("…문제"), 보기는 절 수정 지시 — 원본 SQL 부재로 풀이 불가` });
    } else if (!optionsHaveSqlKeyword) {
      flags.push({ kind: "메타 서술 stem", detail: `stem 이 의문문 아닌 메타 요약, 보기에 SQL 키워드/절명 없음 — 원본 자료 부재` });
    }
  }

  return flags;
}

// ─────────────────────────────────────────────────────────────────────────────
// 리포트 생성

const allKeys = [...questions.keys()].sort((a, b) => {
  const [ra, da] = a.split("/").map(Number);
  const [rb, db] = b.split("/").map(Number);
  if (ra !== rb) return rb - ra; // 최신 회차 위
  return da - db;
});

const results = [];
for (const k of allKeys) {
  const q = questions.get(k);
  const flags = auditQuestion(q);
  if (flags.length > 0) results.push({ q, flags });
}

// 요약 통계
const rounds = [...new Set([...questions.values()].map((q) => q.round))].sort((a, b) => b - a);
const summary = rounds.map((r) => {
  const total = [...questions.values()].filter((q) => q.round === r).length;
  const flagged = results.filter((x) => x.q.round === r);
  const byKind = {};
  for (const x of flagged) for (const f of x.flags) byKind[f.kind] = (byKind[f.kind] || 0) + 1;
  return { round: r, total, flagged: flagged.length, byKind };
});

// 통계
const allQ = [...questions.values()];
const stats = rounds.map((r) => {
  const qs = allQ.filter((q) => q.round === r);
  const stemLens = qs.map((q) => {
    const idx = q.content.search(/[①②③④]/);
    return idx >= 0 ? stripCode(q.content.slice(0, idx)).replace(/\s+/g, " ").trim().length : stripCode(q.content).length;
  });
  const explLens = qs.map((q) => q.explanation.length);
  const types = {};
  for (const q of qs) types[q.question_type] = (types[q.question_type] || 0) + 1;
  return {
    round: r,
    total: qs.length,
    stemMin: Math.min(...stemLens),
    stemAvg: Math.round(stemLens.reduce((a, b) => a + b, 0) / stemLens.length),
    explMin: Math.min(...explLens),
    explAvg: Math.round(explLens.reduce((a, b) => a + b, 0) / explLens.length),
    types: Object.entries(types).map(([k, v]) => `${k}:${v}`).join(" "),
  };
});

// 짧은 stem TOP 10 (전 회차 통합)
const shortStems = allQ
  .map((q) => {
    const idx = q.content.search(/[①②③④]/);
    const stemText = idx >= 0 ? stripCode(q.content.slice(0, idx)).replace(/\s+/g, " ").trim() : stripCode(q.content);
    return { round: q.round, display: q.display_order, len: stemText.length, text: stemText };
  })
  .sort((a, b) => a.len - b.len)
  .slice(0, 10);

// 출력
const lines = [];
lines.push("# SQLD 기출복원 전수조사 리포트");
lines.push("");
lines.push(`- 조사 시각: ${new Date().toISOString()}`);
lines.push(`- 대상: SQLD past_exam 전체 (${allQ.length}문항, ${rounds.length}회차)`);
lines.push(`- 검출: ${results.length}문항 (감사 항목 중 하나 이상 해당)`);
lines.push("");
lines.push("## 검사 기준");
lines.push("");
lines.push("- **선택지 불완전** — MCQ 의 ①②③④ 마커 누락/중복/공백/동일 텍스트");
lines.push("- **정답 placeholder** — explanation 에 ⚠️ 또는 \"정답 재확인/미상\" 잔존");
lines.push("- **이미지 누락** — markdown 이미지 링크가 가리키는 파일 없음, `[그림: ...]` 텍스트 placeholder 잔존, stem 이 시각자료 참조하나 이미지/SVG/표/구조 블록 없음");
lines.push("- **풀이 조건 빈약** — stem 이 지시 표현(다음/위/아래/주어진)으로 자료 참조하나 코드/표/이미지/구조 블록 동반 없음");
lines.push("- **작업 메모 흔적** — `[복원 메모`, `**복원 보강 안내**`, `TODO`, `FIXME`, `확인 필요` 등 사후 작업 표시 잔존");
lines.push("- **복원 안내 잔존** — V45 가 삽입한 `> ⚠️ **안내**` blockquote 가 V66 으로 덮이지 못한 경우");
lines.push("- **메타 서술 stem** — stem 이 의문문(`?`) 형태가 아닌 메타 요약(예: \"…구하는 문제\", \"…관련 문제 중 첫 번째\") 으로, 보기가 절 수정 지시이거나 식별 가능한 쿼리 키워드 없는 경우 — 원본 SQL/조건 부재로 풀이 불가");
lines.push("");
lines.push("## 회차별 통계");
lines.push("");
lines.push("| 회차 | 총 | stem 평균(자) | stem 최단 | explanation 평균 | explanation 최단 | 유형 |");
lines.push("| --- | ---: | ---: | ---: | ---: | ---: | --- |");
for (const s of stats) {
  lines.push(`| 제${s.round}회 | ${s.total} | ${s.stemAvg} | ${s.stemMin} | ${s.explAvg} | ${s.explMin} | ${s.types} |`);
}
lines.push("");
lines.push("## 짧은 stem TOP 10 (수동 점검용)");
lines.push("");
lines.push("자동 검사를 통과했지만 stem 본문이 짧은 문항이다. 보기 자체가 비교 대상인 단답형 개념 문제는 짧아도 정상이며, 그런 경우 false positive 이다.");
lines.push("");
lines.push("| 회차 | 문항 | stem 길이 | stem |");
lines.push("| --- | ---: | ---: | --- |");
for (const s of shortStems) {
  lines.push(`| 제${s.round}회 | Q${s.display} | ${s.len} | ${s.text.slice(0, 60)} |`);
}
lines.push("");
lines.push("");
lines.push("## 검출 요약");
lines.push("");
lines.push("| 회차 | 총 문항 | 검출 | 선택지 불완전 | 정답 placeholder | 이미지 누락 | 풀이 조건 빈약 | 작업 메모 흔적 | 복원 안내 잔존 | 메타 서술 stem |");
lines.push("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |");
for (const s of summary) {
  lines.push(
    `| 제${s.round}회 | ${s.total} | ${s.flagged} | ${s.byKind["선택지 불완전"] || 0} | ${s.byKind["정답 placeholder"] || 0} | ${s.byKind["이미지 누락"] || 0} | ${s.byKind["풀이 조건 빈약"] || 0} | ${s.byKind["작업 메모 흔적"] || 0} | ${s.byKind["복원 안내 잔존"] || 0} | ${s.byKind["메타 서술 stem"] || 0} |`
  );
}
lines.push("");
if (results.length === 0) {
  lines.push("> ✅ 검출 0건 — 6개 감사 항목 모두 통과. 사후 정리(V44/45/66/67/89)로 알려진 결함이 모두 해소된 상태로 보인다.");
  lines.push("");
}
lines.push("## 검출 항목");

let curRound = null;
for (const { q, flags } of results) {
  if (q.round !== curRound) {
    curRound = q.round;
    lines.push("");
    lines.push(`### SQLD 제${q.round}회`);
  }
  const kinds = [...new Set(flags.map((f) => f.kind))].join(" · ");
  lines.push("");
  lines.push(`#### Q${q.display_order} — ${kinds}`);
  lines.push(`- 회차: 제${q.round}회 / display_order ${q.display_order}`);
  lines.push(`- 유형: ${q.question_type}${q.correct_option ? ` / 정답 ${q.correct_option}` : ""}`);
  if (q.topic) lines.push(`- topic: ${q.topic}`);
  for (const f of flags) {
    lines.push(`- ❌ **${f.kind}** — ${f.detail}`);
  }
  const stem = bodyText(q.content).slice(0, 140);
  lines.push("- 본문 발췌:");
  lines.push("  > " + stem.replace(/\n/g, " "));
}

if (results.length === 0) {
  lines.push("");
  lines.push("(검출 0건)");
}

// 전체 덤프 — 수동 검수용. AUDIT_DUMP_ALL=1 이면 회차별 파일을 임시 위치에 작성.
if (process.env.AUDIT_DUMP_ALL) {
  const dumpDir = path.join(__dirname, "_audit_dump");
  fs.mkdirSync(dumpDir, { recursive: true });
  for (const r of rounds) {
    const qs = allQ.filter((q) => q.round === r).sort((a, b) => a.display_order - b.display_order);
    const out = [];
    out.push(`# SQLD 제${r}회 — 전수 덤프 (수동 검수용)`);
    for (const q of qs) {
      out.push("");
      out.push(`## Q${q.display_order} (정답 ${q.correct_option}) — topic: ${q.topic ?? "(없음)"}`);
      out.push("");
      out.push("### content");
      out.push(q.content);
      out.push("");
      out.push("### explanation");
      out.push(q.explanation);
      out.push("");
      out.push("---");
    }
    fs.writeFileSync(path.join(dumpDir, `sqld-${r}.md`), out.join("\n"), "utf8");
  }
  console.log(`✓ 회차별 전수 덤프: ${dumpDir}`);
}

// debug dump — AUDIT_DUMP=56:14,56:34,56:43 같은 형식으로 특정 문항 최종 상태 확인
if (process.env.AUDIT_DUMP) {
  for (const spec of process.env.AUDIT_DUMP.split(",")) {
    const q = questions.get(spec.trim());
    if (!q) { console.log(`[dump] ${spec}: not found`); continue; }
    console.log(`\n[dump] ${spec}`);
    console.log("  type:", q.question_type, "/ correct:", q.correct_option);
    console.log("  content:", JSON.stringify(q.content.slice(0, 400)));
    console.log("  explanation (first 200):", JSON.stringify(q.explanation.slice(0, 200)));
  }
}

fs.writeFileSync(OUT, lines.join("\n"), "utf8");
console.log(`✓ 리포트 저장: ${OUT}`);
console.log(`  - 총 ${[...questions.values()].length}문항 중 ${results.length}건 검출`);
for (const s of summary) {
  console.log(`  - 제${s.round}회: ${s.flagged}/${s.total}건`);
}
