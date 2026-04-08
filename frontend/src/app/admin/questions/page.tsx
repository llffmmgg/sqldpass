"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  getQuestions,
  deleteQuestion,
  exportQuestions,
  resetExportMark,
  verifyAllQuestions,
  type AdminQuestionPage,
  type ExportExamType,
  type QuestionVerifyResult,
} from "@/lib/adminApi";
import { formatDate } from "@/lib/format";

export default function AdminQuestionsPage() {
  const router = useRouter();
  const [data, setData] = useState<AdminQuestionPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [exportingKey, setExportingKey] = useState<string | null>(null);
  const [jumpId, setJumpId] = useState("");
  const [verifyLimit, setVerifyLimit] = useState(50);
  const [verifying, setVerifying] = useState(false);
  const [verifyResults, setVerifyResults] = useState<QuestionVerifyResult[] | null>(null);

  function handleJumpToId(e: React.FormEvent) {
    e.preventDefault();
    const id = parseInt(jumpId.trim(), 10);
    if (!Number.isFinite(id) || id <= 0) {
      alert("유효한 문제 ID를 입력하세요.");
      return;
    }
    router.push(`/admin/questions/${id}`);
  }

  async function handleVerify() {
    if (!confirm(`최근 ${verifyLimit}개 문제를 LLM으로 검증합니다. 비용/시간이 발생합니다. 계속할까요?`)) return;
    setVerifying(true);
    setVerifyResults(null);
    try {
      const results = await verifyAllQuestions(verifyLimit);
      setVerifyResults(results);
    } catch (e) {
      alert(`검증 실패: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setVerifying(false);
    }
  }

  function load(p: number) {
    setLoading(true);
    getQuestions(p, 20)
      .then(setData)
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(page); }, [page]);

  async function handleDelete(id: number) {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    await deleteQuestion(id);
    load(page);
  }

  async function handleExport(examType: ExportExamType, force: boolean) {
    if (force && !confirm("이미 검증한 문제까지 다시 다운로드합니다. 계속할까요?")) return;
    const key = `${examType}-${force ? "force" : "new"}`;
    setExportingKey(key);
    try {
      const count = await exportQuestions(examType, force);
      if (count === 0) {
        alert("다운로드할 신규 문제가 없습니다. (전체 강제 다운로드를 사용하세요)");
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
    const label =
      examType === "SQLD"
        ? "SQLD"
        : examType === "ENGINEER_PRACTICAL"
          ? "정처기"
          : "컴활 1급";
    if (!confirm(`${label}의 export 마크를 모두 초기화합니다. 계속할까요?`)) return;
    const key = `${examType}-reset`;
    setExportingKey(key);
    try {
      const reset = await resetExportMark(examType);
      alert(`${reset}개 문제의 마크를 리셋했습니다.`);
    } catch (e) {
      alert(`리셋 실패: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setExportingKey(null);
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold">문제 관리</h1>

      {/* ID로 바로 가기 */}
      <section className="mt-6 rounded-lg border border-border bg-surface p-4">
        <h2 className="text-sm font-semibold text-muted">🔎 ID로 바로 수정</h2>
        <form onSubmit={handleJumpToId} className="mt-2 flex gap-2">
          <input
            type="number"
            min={1}
            value={jumpId}
            onChange={(e) => setJumpId(e.target.value)}
            placeholder="문제 ID (예: 1234)"
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

      {/* LLM 일괄 검증 */}
      <section className="mt-4 rounded-lg border border-border bg-surface p-4">
        <h2 className="text-sm font-semibold text-muted">🤖 LLM 일괄 검증</h2>
        <p className="mt-1 text-xs text-muted">
          최근 N개 문제를 LLM(verifier)으로 점검 → 의심 문제 ID + 사유 반환. 결과의 ID를 클릭해 즉시 수정.
        </p>
        <div className="mt-3 flex items-center gap-2">
          <label className="text-xs text-muted">검사 개수</label>
          <select
            value={verifyLimit}
            onChange={(e) => setVerifyLimit(Number(e.target.value))}
            className="rounded border border-border bg-background px-2 py-1 text-sm"
          >
            <option value={20}>20</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
            <option value={200}>200</option>
            <option value={500}>500</option>
          </select>
          <button
            onClick={handleVerify}
            disabled={verifying}
            className="rounded bg-amber-500 px-4 py-2 text-sm font-medium text-black hover:bg-amber-600 disabled:opacity-50"
          >
            {verifying ? "검증 중..." : "일괄 검증 시작"}
          </button>
        </div>
        {verifyResults && (
          <div className="mt-4">
            <p className="text-xs text-muted">
              의심 문제 <span className="font-bold text-amber-400">{verifyResults.length}</span>건
            </p>
            {verifyResults.length > 0 && (
              <ul className="mt-2 space-y-1 max-h-96 overflow-y-auto">
                {verifyResults.map((r) => (
                  <li
                    key={r.questionId}
                    className="flex items-start justify-between gap-2 rounded border border-border bg-background px-3 py-2 text-xs"
                  >
                    <div className="flex-1 min-w-0">
                      <span className="rounded bg-violet-500/10 px-1.5 py-0.5 font-medium text-violet-400">
                        {r.subjectName}
                      </span>
                      <span className="ml-2 text-muted">{r.summary || "(요약 없음)"}</span>
                      <p className="mt-1 text-amber-400">{r.reason}</p>
                    </div>
                    <Link
                      href={`/admin/questions/${r.questionId}`}
                      className="shrink-0 rounded border border-border px-2 py-1 hover:text-foreground"
                    >
                      #{r.questionId} 수정
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </section>

      {/* LLM 검증용 Markdown export 패널 */}
      <section className="mt-6 rounded-lg border border-border bg-surface p-4">
        <h2 className="text-sm font-semibold text-muted">📥 LLM 검증용 다운로드</h2>
        <p className="mt-1 text-xs text-muted">
          문제를 .md 파일로 다운로드 → 외부 LLM에 던져 검증 → 결과 보고 어드민에서 수정.
          신규 다운로드는 한 번 받은 문제를 다음에 자동으로 제외합니다.
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
            label="정처기 실기"
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
            {data.content.map((q) => (
              <div
                key={q.id}
                className="flex items-center justify-between rounded-lg border border-border bg-surface px-4 py-3"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-400">
                      {q.subjectName}
                    </span>
                    <span className="text-xs text-muted">{formatDate(q.createdAt)}</span>
                  </div>
                  <p className="mt-1 truncate text-sm">{q.content.split("\n")[0]}</p>
                  {q.summary && (
                    <p className="mt-0.5 text-xs text-muted">{q.summary}</p>
                  )}
                </div>
                <div className="ml-4 flex shrink-0 gap-2">
                  <Link
                    href={`/admin/questions/${q.id}`}
                    className="rounded border border-border px-3 py-1 text-xs text-muted transition hover:text-foreground"
                  >
                    수정
                  </Link>
                  <button
                    onClick={() => handleDelete(q.id)}
                    className="rounded border border-red-500/30 px-3 py-1 text-xs text-red-400 transition hover:bg-red-500/10"
                  >
                    삭제
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Pagination */}
          <div className="mt-6 flex items-center justify-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded border border-border px-3 py-1 text-sm disabled:opacity-30"
            >
              이전
            </button>
            <span className="text-sm text-muted">
              {page + 1} / {data.totalPages || 1}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
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
  const busy = (k: string) => exportingKey === k;
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
          {busy(newKey) ? "다운로드 중..." : "📥 신규만 다운로드"}
        </button>
        <button
          onClick={() => onExport(examType, true)}
          disabled={anyBusy}
          className="rounded border border-amber-500/40 bg-amber-500/10 px-3 py-1 text-xs text-amber-300 transition hover:bg-amber-500/20 disabled:opacity-50"
        >
          {busy(forceKey) ? "다운로드 중..." : "⚡ 전체 강제 다운로드"}
        </button>
        <button
          onClick={() => onReset(examType)}
          disabled={anyBusy}
          className="rounded border border-border px-3 py-1 text-xs text-muted transition hover:text-foreground disabled:opacity-50"
        >
          {busy(resetKey) ? "리셋 중..." : "🔄 마크 리셋"}
        </button>
      </div>
    </div>
  );
}
