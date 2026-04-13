"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { getSubjects, type Subject } from "@/lib/api";
import {
  deleteQuestion,
  downloadBucketMarkdown,
  exportQuestions,
  getQuestion,
  getQuestionVerifyHistory,
  getQuestions,
  getStats,
  getVerifyIssueCounts,
  getVerifyIssues,
  resetExportMark,
  verifyAllQuestions,
  type AdminQuestion,
  type AdminQuestionPage,
  type AdminStats,
  type ExportExamType,
  type QuestionVerifyHistory,
  type QuestionVerifyRun,
  type VerificationCategory,
  type VerificationExamType,
  type VerificationIssue,
} from "@/lib/adminApi";
import { formatDate } from "@/lib/format";

type VerifyExamFilter = "ALL" | VerificationExamType;

type SubjectOption = {
  id: number;
  label: string;
  examType: VerificationExamType;
};

const ENGINEER_ROOT_NAME = "정보처리기사 실기";
const COMPUTER_LITERACY_ROOT_NAME = "컴퓨터활용능력 1급 필기";
const COMPUTER_LITERACY_2_ROOT_NAME = "컴퓨터활용능력 2급 필기";

const EXAM_LABEL: Record<VerificationExamType, string> = {
  SQLD: "SQLD",
  ENGINEER_PRACTICAL: "정보처리기사 실기",
  COMPUTER_LITERACY_1: "컴활 1급 필기",
  COMPUTER_LITERACY_2: "컴활 2급 필기",
  ENGINEER_WRITTEN: "정보처리기사 필기",
};

function resolveExamType(rootName: string): VerificationExamType {
  if (rootName === ENGINEER_ROOT_NAME) return "ENGINEER_PRACTICAL";
  if (rootName === COMPUTER_LITERACY_ROOT_NAME) return "COMPUTER_LITERACY_1";
  if (rootName === COMPUTER_LITERACY_2_ROOT_NAME) return "COMPUTER_LITERACY_2";
  return "SQLD";
}

function buildSubjectOptions(subjects: Subject[], selectedExamType: VerifyExamFilter): SubjectOption[] {
  const options: SubjectOption[] = [];

  for (const root of subjects) {
    const examType = resolveExamType(root.name);
    if (selectedExamType !== "ALL" && examType !== selectedExamType) continue;

    options.push({ id: root.id, label: root.name, examType });
    for (const child of root.children) {
      options.push({
        id: child.id,
        label: `${root.name} > ${child.name}`,
        examType,
      });
    }
  }

  return options;
}

function formatVerifyScope(run: { examType: VerificationExamType | null; subjectName: string | null }) {
  const examLabel = run.examType ? EXAM_LABEL[run.examType] : "전체 시험";
  if (!run.subjectName) return examLabel;
  return `${examLabel} / ${run.subjectName}`;
}

export default function AdminQuestionsPage() {
  const router = useRouter();
  const [data, setData] = useState<AdminQuestionPage | null>(null);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [verifyHistory, setVerifyHistory] = useState<QuestionVerifyHistory[]>([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [exportingKey, setExportingKey] = useState<string | null>(null);
  const [jumpId, setJumpId] = useState("");

  const [verifyLimit, setVerifyLimit] = useState(50);
  const [verifyExamType, setVerifyExamType] = useState<VerifyExamFilter>("ALL");
  const [verifySubjectId, setVerifySubjectId] = useState<number | undefined>();
  const [forceRecheck, setForceRecheck] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [verifyRun, setVerifyRun] = useState<QuestionVerifyRun | null>(null);
  const [stats, setStats] = useState<AdminStats | null>(null);

  // 카테고리별 미해결 문제 조회
  const [issueCounts, setIssueCounts] = useState<Record<VerificationCategory, number> | null>(null);
  const [issueTab, setIssueTab] = useState<VerificationCategory>("MANUAL_REVIEW");
  const [issuePage, setIssuePage] = useState(0);
  const [issueData, setIssueData] = useState<{ items: VerificationIssue[]; totalPages: number; totalElements: number } | null>(null);
  const [issueLoading, setIssueLoading] = useState(false);

  async function refreshIssueCounts() {
    try {
      const counts = await getVerifyIssueCounts();
      setIssueCounts(counts);
    } catch {
      /* ignore */
    }
  }

  /** AdminQuestion → markdown 텍스트 */
  function questionToMarkdown(q: AdminQuestion): string {
    const lines: string[] = [];
    lines.push(`# 문제 #${q.id}`);
    lines.push("");
    lines.push(`- 과목: ${q.subjectName}`);
    lines.push(`- 유형: ${q.questionType}`);
    if (q.summary) lines.push(`- 요약: ${q.summary}`);
    lines.push(`- 생성: ${q.createdAt}`);
    lines.push("");
    lines.push("## 본문");
    lines.push("");
    lines.push(q.content ?? "");
    lines.push("");
    if (q.questionType === "MCQ") {
      lines.push("## 정답");
      lines.push("");
      lines.push(`정답: ${q.correctOption}번`);
    } else {
      lines.push("## 모범 답안");
      lines.push("");
      lines.push(q.answer ?? "");
      if (q.keywords && q.keywords.length > 0) {
        lines.push("");
        lines.push(`키워드: ${q.keywords.join(", ")}`);
      }
    }
    lines.push("");
    lines.push("## 해설");
    lines.push("");
    lines.push(q.explanation ?? "");
    lines.push("");
    return lines.join("\n");
  }

  async function copyIssueAsMarkdown(id: number) {
    try {
      const q = await getQuestion(id);
      const md = questionToMarkdown(q);
      await navigator.clipboard.writeText(md);
      alert(`#${id} 마크다운 복사됨`);
    } catch (e) {
      alert(`복사 실패: ${e instanceof Error ? e.message : String(e)}`);
    }
  }

  async function downloadIssueAsMarkdown(id: number) {
    try {
      const q = await getQuestion(id);
      const md = questionToMarkdown(q);
      const blob = new Blob([md], { type: "text/markdown;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `question-${id}.md`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (e) {
      alert(`다운로드 실패: ${e instanceof Error ? e.message : String(e)}`);
    }
  }

  async function loadIssues(category: VerificationCategory, p: number) {
    setIssueLoading(true);
    try {
      const r = await getVerifyIssues(category, p, 20);
      setIssueData({ items: r.content, totalPages: r.totalPages, totalElements: r.totalElements });
    } catch {
      setIssueData({ items: [], totalPages: 0, totalElements: 0 });
    } finally {
      setIssueLoading(false);
    }
  }

  const subjectOptions = buildSubjectOptions(subjects, verifyExamType);

  function refreshStats() {
    getStats().then(setStats).catch(() => {});
  }

  useEffect(() => {
    getSubjects().then(setSubjects);
    getQuestionVerifyHistory(5).then(setVerifyHistory);
    refreshIssueCounts();
    refreshStats();
  }, []);

  useEffect(() => {
    loadIssues(issueTab, issuePage);
  }, [issueTab, issuePage]);

  useEffect(() => {
    if (verifySubjectId && !subjectOptions.some((option) => option.id === verifySubjectId)) {
      setVerifySubjectId(undefined);
    }
  }, [verifySubjectId, subjectOptions]);

  useEffect(() => {
    setLoading(true);
    getQuestions(page, 20)
      .then(setData)
      .finally(() => setLoading(false));
  }, [page]);

  function handleJumpToId(e: React.FormEvent) {
    e.preventDefault();
    const id = parseInt(jumpId.trim(), 10);
    if (!Number.isFinite(id) || id <= 0) {
      alert("유효한 문제 ID를 입력해주세요.");
      return;
    }
    router.push(`/admin/questions/${id}`);
  }

  async function handleVerify() {
    const examLabel = verifyExamType === "ALL" ? "전체 시험" : EXAM_LABEL[verifyExamType];
    const subjectLabel = subjectOptions.find((option) => option.id === verifySubjectId)?.label ?? "전체 과목";
    const modeLabel = forceRecheck ? "재검증 포함" : "미검증 문제만";

    if (!confirm(`${examLabel} / ${subjectLabel} / ${modeLabel} 기준으로 최근 ${verifyLimit}개를 검증합니다. 계속할까요?`)) {
      return;
    }

    setVerifying(true);
    setVerifyRun(null);

    try {
      const run = await verifyAllQuestions({
        limit: verifyLimit,
        examType: verifyExamType === "ALL" ? undefined : verifyExamType,
        subjectId: verifySubjectId,
        force: forceRecheck,
      });
      setVerifyRun(run);
      setVerifyHistory(run.recentRuns);
      // 검증 후 카테고리 카운트, 탭 리스트, 전체 stats 모두 갱신
      refreshIssueCounts();
      loadIssues(issueTab, 0);
      setIssuePage(0);
      refreshStats();
    } catch (e) {
      alert(`검증 실패: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setVerifying(false);
    }
  }

  async function handleDelete(id: number) {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    await deleteQuestion(id);
    setLoading(true);
    getQuestions(page, 20)
      .then(setData)
      .finally(() => setLoading(false));
  }

  async function handleExport(examType: ExportExamType, force: boolean) {
    if (force && !confirm("이미 검증한 문제까지 다시 다운로드합니다. 계속할까요?")) return;
    const key = `${examType}-${force ? "force" : "new"}`;
    setExportingKey(key);
    try {
      const count = await exportQuestions(examType, force);
      if (count === 0) {
        alert("다운로드할 신규 문제가 없습니다. 전체 강제 다운로드를 사용해주세요.");
      } else {
        alert(`${count}개 문제를 다운로드했습니다.`);
      }
    } catch (e) {
      alert(`다운로드 실패: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setExportingKey(null);
    }
  }

  async function handleResetMark(examType: ExportExamType) {
    const label = EXAM_LABEL[examType];
    if (!confirm(`${label}의 export 마크를 모두 초기화합니다. 계속할까요?`)) return;

    const key = `${examType}-reset`;
    setExportingKey(key);
    try {
      const reset = await resetExportMark(examType);
      alert(`${reset}개 문제의 마크를 초기화했습니다.`);
    } catch (e) {
      alert(`리셋 실패: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setExportingKey(null);
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold">문제 관리</h1>

      {stats && (
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <SummaryCard label="전체 문제" value={`${stats.totalQuestions}건`} />
          <SummaryCard
            label="검증 완료"
            value={
              stats.totalQuestions > 0
                ? `${stats.verifiedQuestions}건 (${Math.round(
                    (stats.verifiedQuestions / stats.totalQuestions) * 100,
                  )}%)`
                : `${stats.verifiedQuestions}건`
            }
          />
          <SummaryCard label="미검증" value={`${stats.unverifiedQuestions}건`} accent />
        </div>
      )}

      <section className="mt-6 rounded-lg border border-border bg-surface p-4">
        <h2 className="text-sm font-semibold text-muted">문제 ID로 바로 이동</h2>
        <form onSubmit={handleJumpToId} className="mt-2 flex gap-2">
          <input
            type="number"
            min={1}
            value={jumpId}
            onChange={(e) => setJumpId(e.target.value)}
            placeholder="문제 ID"
            className="flex-1 rounded border border-border bg-background px-3 py-2 text-sm"
          />
          <button
            type="submit"
            className="rounded bg-violet-500 px-4 py-2 text-sm font-medium text-white hover:bg-violet-600"
          >
            이동
          </button>
        </form>
      </section>

      <section className="mt-4 rounded-lg border border-border bg-surface p-4">
        <h2 className="text-sm font-semibold text-muted">LLM 직접 검증</h2>
        <p className="mt-1 text-xs text-muted">
          시험/과목 범위를 고른 뒤 직접 검증합니다. 기본값은 아직 직접 검증하지 않은 문제만 대상으로 합니다.
        </p>

        <div className="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <label className="text-xs text-muted">
            시험
            <select
              value={verifyExamType}
              onChange={(e) => setVerifyExamType(e.target.value as VerifyExamFilter)}
              className="mt-1 block w-full rounded border border-border bg-background px-2 py-2 text-sm text-foreground"
            >
              <option value="ALL">전체 시험</option>
              <option value="SQLD">SQLD</option>
              <option value="ENGINEER_PRACTICAL">정보처리기사 실기</option>
              <option value="COMPUTER_LITERACY_1">컴활 1급 필기</option>
              <option value="COMPUTER_LITERACY_2">컴활 2급 필기</option>
            </select>
          </label>

          <label className="text-xs text-muted">
            과목
            <select
              value={verifySubjectId ?? ""}
              onChange={(e) => setVerifySubjectId(e.target.value ? Number(e.target.value) : undefined)}
              className="mt-1 block w-full rounded border border-border bg-background px-2 py-2 text-sm text-foreground"
            >
              <option value="">전체 과목</option>
              {subjectOptions.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="text-xs text-muted">
            검증 개수
            <select
              value={verifyLimit}
              onChange={(e) => setVerifyLimit(Number(e.target.value))}
              className="mt-1 block w-full rounded border border-border bg-background px-2 py-2 text-sm text-foreground"
            >
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
              <option value={200}>200</option>
              <option value={500}>500</option>
            </select>
          </label>

          <label className="flex items-end gap-2 rounded border border-border bg-background px-3 py-2 text-sm text-foreground">
            <input
              type="checkbox"
              checked={forceRecheck}
              onChange={(e) => setForceRecheck(e.target.checked)}
            />
            재검증 포함
          </label>
        </div>

        <div className="mt-3">
          <button
            onClick={handleVerify}
            disabled={verifying}
            className="rounded bg-amber-500 px-4 py-2 text-sm font-medium text-black hover:bg-amber-600 disabled:opacity-50"
          >
            {verifying ? "검증 중..." : "일괄 검증 시작"}
          </button>
        </div>

        {verifyRun && (
          <div className="mt-4 grid gap-3 md:grid-cols-3 xl:grid-cols-6">
            <SummaryCard label="검증 범위" value={formatVerifyScope(verifyRun)} />
            <SummaryCard label="처리 건수" value={`${verifyRun.processedCount}건`} />
            <SummaryCard label="의심 문제" value={`${verifyRun.suspiciousCount}건`} accent />
            <SummaryCard
              label="자동 수정 (검토 권장)"
              value={`${verifyRun.fixedCount}건`}
              action={
                verifyRun.fixedCount > 0 && (
                  <button
                    type="button"
                    onClick={() => downloadBucketMarkdown(verifyRun, "fixed")}
                    className="rounded border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[10px] text-emerald-300 hover:bg-emerald-500/20"
                    title="자동 수정 결과 md 다운로드"
                  >
                    📥 md
                  </button>
                )
              }
            />
            <SummaryCard
              label="수동 검토"
              value={`${verifyRun.unfixableCount}건`}
              action={
                verifyRun.unfixableCount > 0 && (
                  <button
                    type="button"
                    onClick={() => downloadBucketMarkdown(verifyRun, "unfixable")}
                    className="rounded border border-amber-500/40 bg-amber-500/10 px-2 py-0.5 text-[10px] text-amber-300 hover:bg-amber-500/20"
                    title="수동 검토 대상 md 다운로드"
                  >
                    📥 md
                  </button>
                )
              }
            />
            <SummaryCard
              label="판단 불가 / 에러"
              value={`${verifyRun.errorCount}건`}
              action={
                verifyRun.errorCount > 0 && (
                  <button
                    type="button"
                    onClick={() => downloadBucketMarkdown(verifyRun, "error")}
                    className="rounded border border-zinc-500/40 bg-zinc-500/10 px-2 py-0.5 text-[10px] text-zinc-300 hover:bg-zinc-500/20"
                    title="판단 불가 목록 md 다운로드"
                  >
                    📥 md
                  </button>
                )
              }
            />
          </div>
        )}

        {verifyRun && (
          <div className="mt-4">
            <p className="text-xs text-muted">
              의심 문제 <span className="font-bold text-amber-400">{verifyRun.suspiciousQuestions.length}</span>건
            </p>
            {verifyRun.suspiciousQuestions.length > 0 ? (
              <ul className="mt-2 max-h-96 space-y-1 overflow-y-auto">
                {verifyRun.suspiciousQuestions.map((result) => (
                  <li
                    key={result.questionId}
                    className="flex items-start justify-between gap-2 rounded border border-border bg-background px-3 py-2 text-xs"
                  >
                    <div className="min-w-0 flex-1">
                      <span className="rounded bg-violet-500/10 px-1.5 py-0.5 font-medium text-violet-400">
                        {result.subjectName}
                      </span>
                      <span className="ml-2 text-muted">{result.summary || "(요약 없음)"}</span>
                      <p className="mt-1 text-amber-400">{result.reason}</p>
                    </div>
                    <Link
                      href={`/admin/questions/${result.questionId}`}
                      className="shrink-0 rounded border border-border px-2 py-1 hover:text-foreground"
                    >
                      #{result.questionId} 수정
                    </Link>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-2 text-sm text-green-400">이번 실행에서는 의심 문제가 없었습니다.</p>
            )}
          </div>
        )}

        {/* 카테고리별 미해결 문제 */}
        <div className="mt-8 rounded-lg border border-border bg-background p-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-foreground">검증 카테고리별 미해결 문제</h3>
            <button
              type="button"
              onClick={() => {
                refreshIssueCounts();
                loadIssues(issueTab, issuePage);
              }}
              className="text-xs text-muted hover:text-foreground"
            >
              새로고침
            </button>
          </div>
          <p className="mt-1 text-xs text-muted">
            문제를 수정하면 해당 카테고리에서 자동으로 빠집니다 (verification_category → NONE).
          </p>

          <div className="mt-3 flex gap-1">
            {([
              { key: "MANUAL_REVIEW", label: "수동 검토", color: "amber" },
              { key: "AUTO_FIXED", label: "자동 수정 (검토 권장)", color: "violet" },
              { key: "ERROR", label: "판단 불가 / 에러", color: "rose" },
            ] as const).map((tab) => {
              const active = issueTab === tab.key;
              const count = issueCounts?.[tab.key] ?? 0;
              return (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => {
                    setIssueTab(tab.key);
                    setIssuePage(0);
                  }}
                  className={`relative -mb-px flex items-center gap-2 border-b-2 px-3 py-2 text-xs font-medium transition ${
                    active
                      ? `border-${tab.color}-500 text-${tab.color}-300`
                      : "border-transparent text-muted hover:text-foreground"
                  }`}
                >
                  <span>{tab.label}</span>
                  <span
                    className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold ${
                      count > 0 ? "bg-amber-500/20 text-amber-300" : "bg-zinc-700/50 text-muted"
                    }`}
                  >
                    {count}
                  </span>
                </button>
              );
            })}
          </div>

          {issueLoading && <p className="mt-3 text-xs text-muted">로딩 중…</p>}

          {!issueLoading && issueData && issueData.items.length === 0 && (
            <p className="mt-4 text-center text-xs text-muted">이 카테고리에 해당하는 문제가 없습니다.</p>
          )}

          {!issueLoading && issueData && issueData.items.length > 0 && (
            <>
              <ul className="mt-3 space-y-1 max-h-96 overflow-y-auto">
                {issueData.items.map((it) => (
                  <li
                    key={it.id}
                    className="flex items-start gap-2 rounded border border-border bg-surface px-3 py-2 text-xs"
                  >
                    <span className="rounded bg-zinc-700/40 px-1.5 py-0.5 font-mono tabular-nums text-muted">#{it.id}</span>
                    <div className="min-w-0 flex-1">
                      {it.subjectName && (
                        <span className="rounded bg-violet-500/10 px-1.5 py-0.5 font-medium text-violet-400">
                          {it.subjectName}
                        </span>
                      )}
                      {it.summary && <span className="ml-2 text-muted">{it.summary}</span>}
                      {it.contentPreview && (
                        <p className="mt-1 truncate text-foreground/70">{it.contentPreview}</p>
                      )}
                    </div>
                    <div className="flex shrink-0 items-center gap-1">
                      <button
                        type="button"
                        onClick={() => copyIssueAsMarkdown(it.id)}
                        className="rounded border border-border px-2 py-1 text-muted hover:text-foreground"
                        title="마크다운 클립보드 복사"
                      >
                        MD 복사
                      </button>
                      <button
                        type="button"
                        onClick={() => downloadIssueAsMarkdown(it.id)}
                        className="rounded border border-border px-2 py-1 text-muted hover:text-foreground"
                        title=".md 파일로 다운로드"
                      >
                        .md
                      </button>
                      <Link
                        href={`/admin/questions/${it.id}`}
                        className="rounded border border-border px-2 py-1 hover:text-foreground"
                      >
                        수정
                      </Link>
                    </div>
                  </li>
                ))}
              </ul>

              {issueData.totalPages > 1 && (
                <div className="mt-3 flex items-center justify-center gap-2">
                  <button
                    type="button"
                    onClick={() => setIssuePage((p) => Math.max(0, p - 1))}
                    disabled={issuePage === 0}
                    className="rounded border border-border px-2 py-1 text-xs disabled:opacity-30"
                  >
                    이전
                  </button>
                  <span className="text-xs text-muted">
                    {issuePage + 1} / {issueData.totalPages} ({issueData.totalElements}건)
                  </span>
                  <button
                    type="button"
                    onClick={() => setIssuePage((p) => p + 1)}
                    disabled={issuePage >= issueData.totalPages - 1}
                    className="rounded border border-border px-2 py-1 text-xs disabled:opacity-30"
                  >
                    다음
                  </button>
                </div>
              )}
            </>
          )}
        </div>

        <div className="mt-6">
          <h3 className="text-xs font-semibold text-muted">최근 실행 이력</h3>
          {verifyHistory.length === 0 ? (
            <p className="mt-2 text-sm text-muted">아직 직접 검증 이력이 없습니다.</p>
          ) : (
            <ul className="mt-2 space-y-2">
              {verifyHistory.map((history) => (
                <li
                  key={history.runId}
                  className="flex flex-wrap items-center justify-between gap-3 rounded border border-border bg-background px-3 py-2 text-xs"
                >
                  <div className="min-w-0">
                    <p className="font-medium text-foreground">{formatVerifyScope(history)}</p>
                    <p className="mt-1 text-muted">
                      {formatDate(history.completedAt)} · 요청 {history.limitRequested}건 · 처리 {history.processedCount}건
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-muted">{history.forceRecheck ? "재검증 포함" : "미검증만"}</span>
                    <span className="rounded bg-amber-500/10 px-2 py-1 text-amber-300">
                      의심 {history.suspiciousCount}
                    </span>
                    {history.fixedCount > 0 && (
                      <span className="rounded bg-emerald-500/10 px-2 py-1 text-emerald-300">
                        자동수정 {history.fixedCount}
                      </span>
                    )}
                    {history.unfixableCount > 0 && (
                      <span className="rounded bg-rose-500/10 px-2 py-1 text-rose-300">
                        수동 {history.unfixableCount}
                      </span>
                    )}
                    {history.errorCount > 0 && (
                      <span className="rounded bg-zinc-500/10 px-2 py-1 text-zinc-300">
                        에러 {history.errorCount}
                      </span>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="mt-6 rounded-lg border border-border bg-surface p-4">
        <h2 className="text-sm font-semibold text-muted">LLM 검증용 다운로드</h2>
        <p className="mt-1 text-xs text-muted">
          문제를 markdown으로 다운로드해 외부 LLM에 맡겨 검증할 수 있습니다. 새 문제만 받을지, 이전 문제까지 포함할지 선택할 수 있습니다.
        </p>
        <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <ExportGroup
            label="SQLD"
            examType="SQLD"
            exportingKey={exportingKey}
            onExport={handleExport}
            onReset={handleResetMark}
          />
          <ExportGroup
            label="정보처리기사 실기"
            examType="ENGINEER_PRACTICAL"
            exportingKey={exportingKey}
            onExport={handleExport}
            onReset={handleResetMark}
          />
          <ExportGroup
            label="컴활 1급 필기"
            examType="COMPUTER_LITERACY_1"
            exportingKey={exportingKey}
            onExport={handleExport}
            onReset={handleResetMark}
          />
          <ExportGroup
            label="컴활 2급 필기"
            examType="COMPUTER_LITERACY_2"
            exportingKey={exportingKey}
            onExport={handleExport}
            onReset={handleResetMark}
          />
        </div>
      </section>

      {loading && <p className="mt-6 text-muted">로딩 중...</p>}

      {data && (
        <>
          <p className="mt-2 text-sm text-muted">총 {data.totalElements}개</p>

          <div className="mt-4 space-y-2">
            {data.content.map((question) => (
              <div
                key={question.id}
                className="flex items-center justify-between rounded-lg border border-border bg-surface px-4 py-3"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <Link href={`/admin/questions/${question.id}`} className="text-xs font-mono text-muted/60 hover:text-violet-300 transition-colors">#{question.id}</Link>
                    <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-400">
                      {question.subjectName}
                    </span>
                    {question.verifiedAt ? (
                      <span className="rounded bg-green-500/10 px-2 py-0.5 text-[10px] font-medium text-green-400">
                        검수 완료
                      </span>
                    ) : (
                      <span className="rounded bg-amber-500/10 px-2 py-0.5 text-[10px] font-medium text-amber-400">
                        미검수
                      </span>
                    )}
                    <span className="text-xs text-muted">{formatDate(question.createdAt)}</span>
                  </div>
                  <Link href={`/admin/questions/${question.id}`} className="mt-1 block truncate text-sm hover:text-violet-300 transition-colors">{question.content.split("\n")[0]}</Link>
                  {question.summary && <p className="mt-0.5 text-xs text-muted">{question.summary}</p>}
                </div>
                <div className="ml-4 flex shrink-0 gap-2">
                  <Link
                    href={`/admin/questions/${question.id}`}
                    className="rounded border border-border px-3 py-1 text-xs text-muted transition hover:text-foreground"
                  >
                    수정
                  </Link>
                  <button
                    onClick={() => handleDelete(question.id)}
                    className="rounded border border-red-500/30 px-3 py-1 text-xs text-red-400 transition hover:bg-red-500/10"
                  >
                    삭제
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-6 flex items-center justify-center gap-2">
            <button
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              disabled={page === 0}
              className="rounded border border-border px-3 py-1 text-sm disabled:opacity-30"
            >
              이전
            </button>
            <span className="text-sm text-muted">
              {page + 1} / {data.totalPages || 1}
            </span>
            <button
              onClick={() => setPage((current) => current + 1)}
              disabled={page >= data.totalPages - 1}
              className="rounded border border-border px-3 py-1 text-sm disabled:opacity-30"
            >
              다음
            </button>
          </div>
        </>
      )}
    </div>
  );
}

function SummaryCard({
  label,
  value,
  accent = false,
  action,
}: {
  label: string;
  value: string;
  accent?: boolean;
  action?: React.ReactNode;
}) {
  return (
    <div className="rounded border border-border bg-background px-3 py-3">
      <div className="flex items-start justify-between gap-2">
        <p className="text-xs text-muted">{label}</p>
        {action}
      </div>
      <p className={`mt-1 text-sm font-semibold ${accent ? "text-amber-300" : "text-foreground"}`}>{value}</p>
    </div>
  );
}

function ExportGroup({
  label,
  examType,
  exportingKey,
  onExport,
  onReset,
}: {
  label: string;
  examType: ExportExamType;
  exportingKey: string | null;
  onExport: (examType: ExportExamType, force: boolean) => void;
  onReset: (examType: ExportExamType) => void;
}) {
  const newKey = `${examType}-new`;
  const forceKey = `${examType}-force`;
  const resetKey = `${examType}-reset`;
  const busy = (key: string) => exportingKey === key;
  const anyBusy = exportingKey !== null;

  return (
    <div className="rounded border border-border bg-background p-3">
      <div className="text-sm font-medium">{label}</div>
      <div className="mt-2 flex flex-wrap gap-2">
        <button
          onClick={() => onExport(examType, false)}
          disabled={anyBusy}
          className="rounded border border-violet-500/40 bg-violet-500/10 px-3 py-1 text-xs text-violet-300 transition hover:bg-violet-500/20 disabled:opacity-50"
        >
          {busy(newKey) ? "다운로드 중..." : "신규만 다운로드"}
        </button>
        <button
          onClick={() => onExport(examType, true)}
          disabled={anyBusy}
          className="rounded border border-amber-500/40 bg-amber-500/10 px-3 py-1 text-xs text-amber-300 transition hover:bg-amber-500/20 disabled:opacity-50"
        >
          {busy(forceKey) ? "다운로드 중..." : "전체 강제 다운로드"}
        </button>
        <button
          onClick={() => onReset(examType)}
          disabled={anyBusy}
          className="rounded border border-border px-3 py-1 text-xs text-muted transition hover:text-foreground disabled:opacity-50"
        >
          {busy(resetKey) ? "리셋 중..." : "마크 리셋"}
        </button>
      </div>
    </div>
  );
}
