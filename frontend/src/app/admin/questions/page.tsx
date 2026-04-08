"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { getSubjects, type Subject } from "@/lib/api";
import {
  deleteQuestion,
  exportQuestions,
  getQuestionVerifyHistory,
  getQuestions,
  resetExportMark,
  verifyAllQuestions,
  type AdminQuestionPage,
  type ExportExamType,
  type QuestionVerifyHistory,
  type QuestionVerifyRun,
  type VerificationExamType,
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

const EXAM_LABEL: Record<VerificationExamType, string> = {
  SQLD: "SQLD",
  ENGINEER_PRACTICAL: "정보처리기사 실기",
  COMPUTER_LITERACY_1: "컴활 1급 필기",
};

function resolveExamType(rootName: string): VerificationExamType {
  if (rootName === ENGINEER_ROOT_NAME) return "ENGINEER_PRACTICAL";
  if (rootName === COMPUTER_LITERACY_ROOT_NAME) return "COMPUTER_LITERACY_1";
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

  const subjectOptions = buildSubjectOptions(subjects, verifyExamType);

  useEffect(() => {
    getSubjects().then(setSubjects);
    getQuestionVerifyHistory(5).then(setVerifyHistory);
  }, []);

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
            <SummaryCard label="자동 수정" value={`${verifyRun.fixedCount}건`} />
            <SummaryCard label="수동 검토" value={`${verifyRun.unfixableCount}건`} />
            <SummaryCard label="판단 불가" value={`${verifyRun.errorCount}건`} />
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
                    <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-400">
                      {question.subjectName}
                    </span>
                    <span className="text-xs text-muted">{formatDate(question.createdAt)}</span>
                  </div>
                  <p className="mt-1 truncate text-sm">{question.content.split("\n")[0]}</p>
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

function SummaryCard({ label, value, accent = false }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className="rounded border border-border bg-background px-3 py-3">
      <p className="text-xs text-muted">{label}</p>
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
