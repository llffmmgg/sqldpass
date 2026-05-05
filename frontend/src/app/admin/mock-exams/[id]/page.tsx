"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { use } from "react";
import {
  generateMockExamPdf,
  getAdminMockExamDetail,
  getAdminMockExams,
  getQuestion,
  markMockExamVerified,
  setPastExamMeta,
  toggleExpertVerified,
  updateQuestion,
  verifyAllQuestions,
  type AdminMockExam,
  type AdminMockExamDetail,
  type AdminQuestion,
  type VerificationExamType,
} from "@/lib/adminApi";

/**
 * 어드민: 모의고사 상세 — 문제별 인라인 수정.
 *
 * 흐름:
 *  1) /api/mock-exams/{id} 로 회차 + 문제 id 목록 + displayOrder 가져옴
 *  2) 각 문제는 /api/admin/questions/{id} 로 상세(정답/해설 포함) 조회
 *  3) 각 카드는 textarea 4개(content/correctOption/explanation/summary)로 인라인 편집
 *  4) "저장" 클릭 시 PUT /api/admin/questions/{id} 호출
 */
export default function AdminMockExamDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const examId = Number(id);

  const [exam, setExam] = useState<AdminMockExamDetail | null>(null);
  const [questions, setQuestions] = useState<AdminQuestion[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [verifyResult, setVerifyResult] = useState<string | null>(null);
  const [pdfBusy, setPdfBusy] = useState(false);
  const [pdfStatus, setPdfStatus] = useState<string | null>(null);
  const [allExams, setAllExams] = useState<AdminMockExam[] | null>(null);
  const [jsonMode, setJsonMode] = useState(false);
  const [jsonText, setJsonText] = useState("");
  const [jsonSaving, setJsonSaving] = useState(false);
  const [jsonResult, setJsonResult] = useState<string | null>(null);

  // 같은 시험유형 내 이전/다음 모의고사
  const { prevExam, nextExam } = useMemo(() => {
    if (!allExams || !exam) return { prevExam: null, nextExam: null };
    const sameType = allExams
      .filter((e) => e.examType === exam.examType)
      .sort((a, b) => a.sequence - b.sequence);
    const idx = sameType.findIndex((e) => e.id === examId);
    return {
      prevExam: idx > 0 ? sameType[idx - 1] : null,
      nextExam: idx < sameType.length - 1 ? sameType[idx + 1] : null,
    };
  }, [allExams, exam, examId]);

  useEffect(() => {
    getAdminMockExams().then(setAllExams).catch(() => {});
  }, []);

  useEffect(() => {
    let cancelled = false;
    setJsonMode(false);
    setJsonResult(null);
    (async () => {
      try {
        const detail = await getAdminMockExamDetail(examId);
        if (cancelled) return;
        setExam(detail);
        // 문제별 admin 상세 병렬 조회
        const fulls = await Promise.all(
          detail.questions
            .slice()
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .map((q) => getQuestion(q.id)),
        );
        if (cancelled) return;
        setQuestions(fulls);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : "불러오기 실패");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [examId]);

  async function handleVerify(force: boolean) {
    if (!exam || !questions) return;
    const target = force ? questions.length : questions.filter((q) => !q.verifiedAt).length;
    if (target === 0) {
      setVerifyResult("모든 문제가 이미 검수 완료되었습니다.");
      return;
    }
    const label = force ? `전체 ${target}문항` : `미검수 ${target}문항`;
    if (!confirm(`${label}에 대해 LLM 검증을 실행합니다. 시간이 걸릴 수 있습니다. 계속하시겠습니까?`)) return;

    setVerifying(true);
    setVerifyResult(null);
    try {
      const result = await verifyAllQuestions({
        examType: exam.examType as VerificationExamType,
        mockExamId: examId,
        limit: 200,
        force,
      });
      // 문제 목록 새로고침
      const fulls = await Promise.all(
        exam.questions
          .slice()
          .sort((a, b) => a.displayOrder - b.displayOrder)
          .map((q) => getQuestion(q.id)),
      );
      setQuestions(fulls);
      const suspicious = result.suspiciousQuestions?.length ?? 0;
      setVerifyResult(
        `검증 완료: ${result.processedCount}개 처리, ${suspicious}개 의심 문항`
      );
    } catch (e) {
      setVerifyResult(`검증 실패: ${e instanceof Error ? e.message : "알 수 없는 오류"}`);
    } finally {
      setVerifying(false);
    }
  }

  async function handleMarkVerified() {
    if (!exam || !questions) return;
    const unverified = questions.filter((q) => !q.verifiedAt).length;
    if (unverified === 0) {
      setVerifyResult("모든 문제가 이미 검수 완료되었습니다.");
      return;
    }
    if (!confirm(`${unverified}문항을 수동으로 검수 완료 처리합니다. 계속하시겠습니까?`)) return;

    try {
      const result = await markMockExamVerified(examId);
      const fulls = await Promise.all(
        exam.questions
          .slice()
          .sort((a, b) => a.displayOrder - b.displayOrder)
          .map((q) => getQuestion(q.id)),
      );
      setQuestions(fulls);
      setVerifyResult(`${result.marked}문항 수동 검수 완료 처리됨`);
    } catch (e) {
      setVerifyResult(`처리 실패: ${e instanceof Error ? e.message : "알 수 없는 오류"}`);
    }
  }

  function openJsonEditor() {
    if (!questions) return;
    const data = questions.map((q, i) => ({
      order: i + 1,
      id: q.id,
      subjectName: q.subjectName,
      questionType: q.questionType,
      content: q.content,
      correctOption: q.correctOption,
      answer: q.answer,
      keywords: q.keywords,
      explanation: q.explanation,
      summary: q.summary,
      verified: !!q.verifiedAt,
    }));
    setJsonText(JSON.stringify(data, null, 2));
    setJsonMode(true);
    setJsonResult(null);
  }

  async function handleJsonSave() {
    if (!questions) return;
    let parsed: Array<{ id: number; content: string; questionType: string; correctOption?: number | null; answer?: string | null; keywords?: string[] | null; explanation: string; summary?: string | null }>;
    try {
      parsed = JSON.parse(jsonText);
      if (!Array.isArray(parsed)) throw new Error("배열이 아닙니다");
    } catch (e) {
      setJsonResult(`JSON 파싱 실패: ${e instanceof Error ? e.message : "형식 오류"}`);
      return;
    }

    setJsonSaving(true);
    setJsonResult(null);
    let successCount = 0;
    const failures: string[] = [];

    await Promise.all(
      parsed.map(async (item) => {
        const existing = questions.find((q) => q.id === item.id);
        if (!existing) {
          failures.push(`ID ${item.id}: 해당 문제를 찾을 수 없음`);
          return;
        }
        try {
          const isMcq = (item.questionType ?? existing.questionType) === "MCQ";
          await updateQuestion(item.id, {
            content: item.content,
            questionType: (item.questionType ?? existing.questionType) as import("@/lib/adminApi").AdminQuestionType,
            correctOption: isMcq ? (item.correctOption ?? existing.correctOption) : null,
            answer: isMcq ? null : (item.answer ?? null),
            keywords: isMcq ? null : (item.keywords ?? null),
            explanation: item.explanation,
            summary: item.summary ?? null,
          });
          successCount++;
        } catch (e) {
          failures.push(`ID ${item.id}: ${e instanceof Error ? e.message : "저장 실패"}`);
        }
      }),
    );

    // 문제 목록 새로고침
    if (exam) {
      try {
        const fulls = await Promise.all(
          exam.questions
            .slice()
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .map((q) => getQuestion(q.id)),
        );
        setQuestions(fulls);
      } catch { /* ignore */ }
    }

    const msg = `${successCount}개 저장 완료` + (failures.length > 0 ? `, ${failures.length}개 실패:\n${failures.join("\n")}` : "");
    setJsonResult(msg);
    setJsonSaving(false);
  }

  function handleQuestionUpdated(updated: AdminQuestion) {
    setQuestions((prev) =>
      prev ? prev.map((q) => (q.id === updated.id ? updated : q)) : prev,
    );
  }

  if (error) {
    return (
      <div className="text-red-400">{error}</div>
    );
  }
  if (!exam || !questions) {
    return <div className="text-muted">로딩 중...</div>;
  }

  return (
    <div>
      <div className="flex items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <Link
              href="/admin/mock-exams"
              className="text-xs text-muted hover:text-foreground"
            >
              ← 목록
            </Link>
            {prevExam && (
              <Link
                href={`/admin/mock-exams/${prevExam.id}`}
                className="text-xs text-muted hover:text-foreground"
              >
                ← #{prevExam.sequence}
              </Link>
            )}
            {nextExam && (
              <Link
                href={`/admin/mock-exams/${nextExam.id}`}
                className="text-xs text-muted hover:text-foreground"
              >
                #{nextExam.sequence} →
              </Link>
            )}
          </div>
          <h1 className="mt-1 text-2xl font-bold">
            {exam.name}
            {exam.expertVerified && (
              <span className="ml-2 inline-flex items-center rounded-full border border-emerald-500/50 bg-emerald-500/15 px-2 py-0.5 align-middle text-[10px] font-bold text-emerald-300">
                전문가 검수 완료
              </span>
            )}
          </h1>
          <p className="mt-1 text-sm text-muted">
            {exam.examType} · {exam.totalQuestions}문항 · 회차 #{exam.sequence}
          </p>
        </div>
        <div className="flex flex-col items-end gap-2">
          <div className="flex gap-2">
            <button
              onClick={() => handleVerify(false)}
              disabled={verifying}
              className="rounded border border-emerald-500/40 bg-emerald-500/10 px-3 py-1.5 text-xs font-medium text-emerald-300 transition hover:bg-emerald-500/20 disabled:opacity-50"
            >
              {verifying ? "검증 중..." : `AI 검증 (미검수 ${questions?.filter((q) => !q.verifiedAt).length ?? 0})`}
            </button>
            <button
              onClick={() => handleVerify(true)}
              disabled={verifying}
              className="rounded border border-cyan-500/40 bg-cyan-500/10 px-3 py-1.5 text-xs font-medium text-cyan-300 transition hover:bg-cyan-500/20 disabled:opacity-50"
            >
              {verifying ? "검증 중..." : `AI 전체 검증 (${questions?.length ?? 0})`}
            </button>
            <button
              onClick={handleMarkVerified}
              className="rounded border border-amber-500/40 bg-amber-500/10 px-3 py-1.5 text-xs font-medium text-amber-300 transition hover:bg-amber-500/20"
            >
              전체 검수 완료
            </button>
            <button
              onClick={async () => {
                const result = await toggleExpertVerified(examId);
                setExam({ ...exam, expertVerified: result.expertVerified });
              }}
              className={`rounded border px-3 py-1.5 text-xs font-medium transition ${
                exam.expertVerified
                  ? "border-emerald-500/40 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20"
                  : "border-border text-muted hover:text-foreground"
              }`}
            >
              {exam.expertVerified ? "전문가 검수 해제" : "전문가 검수 완료"}
            </button>
            <button
              onClick={openJsonEditor}
              className="rounded border border-violet-500/40 bg-violet-500/10 px-3 py-1.5 text-xs font-medium text-violet-300 transition hover:bg-violet-500/20"
            >
              JSON 편집
            </button>
            <button
              onClick={() => downloadJson(exam, questions)}
              className="rounded border border-border px-3 py-1.5 text-xs text-muted transition hover:text-foreground"
            >
              JSON
            </button>
            <button
              onClick={() => downloadMd(exam, questions)}
              className="rounded border border-border px-3 py-1.5 text-xs text-muted transition hover:text-foreground"
            >
              MD
            </button>
            <button
              onClick={async () => {
                setPdfBusy(true);
                setPdfStatus("PDF 준비 중…");
                try {
                  const res = await generateMockExamPdf(examId);
                  setPdfStatus(res.cached ? "PDF (캐시) 다운로드 시작" : "PDF 새로 생성 → 다운로드");
                  window.open(res.url, "_blank", "noopener,noreferrer");
                } catch (e) {
                  setPdfStatus(e instanceof Error ? `실패: ${e.message}` : "PDF 생성 실패");
                } finally {
                  setPdfBusy(false);
                }
              }}
              disabled={pdfBusy}
              className="rounded border border-rose-500/40 bg-rose-500/10 px-3 py-1.5 text-xs font-medium text-rose-300 transition hover:bg-rose-500/20 disabled:opacity-50"
              title="동일 콘텐츠는 R2 캐시에서 즉시 반환. 새로 생성 시 5~15초 소요."
            >
              {pdfBusy ? "PDF 생성 중…" : "PDF 다운로드"}
            </button>
          </div>
          {verifyResult && (
            <p className="text-xs text-muted">{verifyResult}</p>
          )}
          {pdfStatus && (
            <p className="text-xs text-muted">{pdfStatus}</p>
          )}
        </div>
      </div>

      <PastExamMetaPanel
        examId={examId}
        exam={exam}
        onUpdated={(next) => setExam({ ...exam, ...next })}
      />

      {jsonMode && (
        <div className="mt-6 rounded-xl border border-violet-500/30 bg-surface p-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-violet-300">JSON 일괄 편집</h2>
            <div className="flex gap-2">
              <button
                onClick={handleJsonSave}
                disabled={jsonSaving}
                className="rounded bg-violet-500 px-4 py-1.5 text-xs font-medium text-white hover:bg-violet-600 disabled:opacity-50"
              >
                {jsonSaving ? "저장 중..." : "JSON 저장"}
              </button>
              <button
                onClick={() => { setJsonMode(false); setJsonResult(null); }}
                className="rounded border border-border px-3 py-1.5 text-xs text-muted hover:text-foreground"
              >
                닫기
              </button>
            </div>
          </div>
          <textarea
            value={jsonText}
            onChange={(e) => setJsonText(e.target.value)}
            rows={30}
            className="mt-3 w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
            spellCheck={false}
          />
          {jsonResult && (
            <pre className="mt-2 whitespace-pre-wrap text-xs text-muted">{jsonResult}</pre>
          )}
        </div>
      )}

      <div className="mt-6 space-y-4">
        {questions.map((q, idx) => (
          <QuestionEditCard
            key={q.id}
            order={idx + 1}
            initial={q}
            onUpdated={handleQuestionUpdated}
          />
        ))}
      </div>
    </div>
  );
}

// ===========================================================
// 문제 카드 — 인라인 편집
// ===========================================================

function QuestionEditCard({
  order,
  initial,
  onUpdated,
}: {
  order: number;
  initial: AdminQuestion;
  onUpdated: (q: AdminQuestion) => void;
}) {
  const isMcq = initial.questionType === "MCQ";
  const initialKeywordsText = (initial.keywords ?? []).join(", ");

  const [content, setContent] = useState(initial.content);
  const [correctOption, setCorrectOption] = useState<number>(initial.correctOption ?? 1);
  const [answer, setAnswer] = useState(initial.answer ?? "");
  const [keywordsText, setKeywordsText] = useState(initialKeywordsText);
  const [explanation, setExplanation] = useState(initial.explanation);
  const [summary, setSummary] = useState(initial.summary ?? "");
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const dirty =
    content !== initial.content ||
    (isMcq && correctOption !== (initial.correctOption ?? 1)) ||
    (!isMcq && answer !== (initial.answer ?? "")) ||
    (!isMcq && keywordsText !== initialKeywordsText) ||
    explanation !== initial.explanation ||
    (summary || null) !== (initial.summary || null);

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      const keywords = keywordsText
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
      const updated = await updateQuestion(initial.id, {
        content,
        questionType: initial.questionType,
        correctOption: isMcq ? correctOption : null,
        answer: isMcq ? null : (answer || null),
        keywords: isMcq ? null : (keywords.length > 0 ? keywords : null),
        explanation,
        summary: summary.trim() ? summary : null,
      });
      onUpdated(updated);
      setSavedAt(Date.now());
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setSaving(false);
    }
  }

  function handleReset() {
    setContent(initial.content);
    setCorrectOption(initial.correctOption ?? 1);
    setAnswer(initial.answer ?? "");
    setKeywordsText(initialKeywordsText);
    setExplanation(initial.explanation);
    setSummary(initial.summary ?? "");
    setError(null);
  }

  return (
    <div className="rounded-xl border border-border bg-surface p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-sm">
          <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-300">
            #{order}
          </span>
          <span className="rounded bg-zinc-500/10 px-2 py-0.5 text-xs text-muted">
            ID {initial.id}
          </span>
          <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs text-violet-300">
            {initial.questionType}
          </span>
          <span className="text-xs text-muted">{initial.subjectName}</span>
        </div>
        <div className="flex items-center gap-2">
          {savedAt && !dirty && (
            <span className="text-xs text-emerald-400">✓ 저장됨</span>
          )}
          {dirty && (
            <button
              onClick={handleReset}
              disabled={saving}
              className="rounded border border-border px-3 py-1 text-xs text-muted hover:text-foreground disabled:opacity-50"
            >
              되돌리기
            </button>
          )}
          <button
            onClick={handleSave}
            disabled={!dirty || saving}
            className="rounded bg-violet-500 px-3 py-1 text-xs font-medium text-white hover:bg-violet-600 disabled:opacity-50"
          >
            {saving ? "저장 중..." : "저장"}
          </button>
        </div>
      </div>

      <div className="mt-3 space-y-3">
        <Field label="문제 (content)">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={Math.min(20, Math.max(6, content.split("\n").length + 1))}
            className="w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
          />
        </Field>

        {isMcq ? (
          <div className="grid gap-3 sm:grid-cols-[120px_1fr]">
            <Field label="정답 옵션">
              <select
                value={correctOption}
                onChange={(e) => setCorrectOption(Number(e.target.value))}
                className="w-full rounded border border-border bg-background px-3 py-2 text-sm"
              >
                {[1, 2, 3, 4].map((n) => (
                  <option key={n} value={n}>
                    {n}번
                  </option>
                ))}
              </select>
            </Field>
            <Field label="요약 (summary)">
              <input
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                placeholder="(선택)"
                className="w-full rounded border border-border bg-background px-3 py-2 text-sm"
              />
            </Field>
          </div>
        ) : (
          <>
            <Field label="정답 (answer)">
              <textarea
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                rows={Math.min(8, Math.max(2, answer.split("\n").length + 1))}
                className="w-full rounded border border-border bg-background px-3 py-2 text-xs"
              />
            </Field>
            <Field label="키워드 / Alias (쉼표 구분 — 단답형은 alias, 약술형은 채점 키워드)">
              <input
                value={keywordsText}
                onChange={(e) => setKeywordsText(e.target.value)}
                placeholder="예: OTU, O T U"
                className="w-full rounded border border-border bg-background px-3 py-2 text-xs"
              />
            </Field>
            <Field label="요약 (summary)">
              <input
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                placeholder="(선택)"
                className="w-full rounded border border-border bg-background px-3 py-2 text-sm"
              />
            </Field>
          </>
        )}

        <Field label="해설 (explanation)">
          <textarea
            value={explanation}
            onChange={(e) => setExplanation(e.target.value)}
            rows={Math.min(15, Math.max(4, explanation.split("\n").length + 1))}
            className="w-full rounded border border-border bg-background px-3 py-2 text-xs"
          />
        </Field>
      </div>

      {error && (
        <div className="mt-3 rounded border border-red-500/30 bg-red-500/5 px-3 py-2 text-xs text-red-400">
          {error}
        </div>
      )}
    </div>
  );
}

function triggerDownload(content: string, filename: string, mime: string) {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

function downloadJson(exam: AdminMockExamDetail, questions: AdminQuestion[]) {
  const data = {
    examName: exam.name,
    examType: exam.examType,
    sequence: exam.sequence,
    totalQuestions: exam.totalQuestions,
    questions: questions.map((q, i) => ({
      order: i + 1,
      id: q.id,
      subjectName: q.subjectName,
      questionType: q.questionType,
      content: q.content,
      correctOption: q.correctOption,
      answer: q.answer,
      keywords: q.keywords,
      explanation: q.explanation,
      summary: q.summary,
      verified: !!q.verifiedAt,
    })),
  };
  const filename = `${exam.name.replace(/\s+/g, "_")}_${exam.examType}.json`;
  triggerDownload(JSON.stringify(data, null, 2), filename, "application/json");
}

function downloadMd(exam: AdminMockExamDetail, questions: AdminQuestion[]) {
  const lines: string[] = [];
  lines.push(`# ${exam.name}`);
  lines.push(`- 시험 유형: ${exam.examType}`);
  lines.push(`- 총 문항: ${exam.totalQuestions}`);
  lines.push(`- 회차: ${exam.sequence}`);
  lines.push("");

  questions.forEach((q, i) => {
    lines.push(`---`);
    lines.push(`## 문항 ${i + 1} (ID: ${q.id})`);
    lines.push(`- 과목: ${q.subjectName}`);
    lines.push(`- 유형: ${q.questionType}`);
    if (q.summary) lines.push(`- 요약: ${q.summary}`);
    lines.push(`- 검수: ${q.verifiedAt ? "완료" : "미검수"}`);
    lines.push("");
    lines.push("### 문제");
    lines.push(q.content);
    lines.push("");
    if (q.questionType === "MCQ" && q.correctOption) {
      lines.push(`### 정답: ${q.correctOption}번`);
    } else if (q.answer) {
      lines.push(`### 정답`);
      lines.push(q.answer);
    }
    if (q.keywords && q.keywords.length > 0) {
      lines.push(`### 키워드: ${q.keywords.join(", ")}`);
    }
    lines.push("");
    lines.push("### 해설");
    lines.push(q.explanation);
    lines.push("");
  });

  const filename = `${exam.name.replace(/\s+/g, "_")}_${exam.examType}.md`;
  triggerDownload(lines.join("\n"), filename, "text/markdown");
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-[11px] font-semibold uppercase tracking-wide text-muted">
        {label}
      </span>
      {children}
    </label>
  );
}

// ===========================================================
// 기출 복원 메타 — kind/연도/회차/시험일 설정 패널
// ===========================================================

function PastExamMetaPanel({
  examId,
  exam,
  onUpdated,
}: {
  examId: number;
  exam: AdminMockExamDetail;
  onUpdated: (next: Partial<AdminMockExamDetail>) => void;
}) {
  const [year, setYear] = useState<string>(exam.examYear ? String(exam.examYear) : "");
  const [round, setRound] = useState<string>(exam.examRound ? String(exam.examRound) : "");
  const [date, setDate] = useState<string>(exam.examDate ?? "");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const isPast = exam.kind === "PAST_EXAM";

  async function save(promote: boolean) {
    setSaving(true);
    setMessage(null);
    try {
      const yearNum = year.trim() ? Number(year) : null;
      const roundNum = round.trim() ? Number(round) : null;
      const updated = await setPastExamMeta(examId, {
        promote,
        examYear: yearNum,
        examRound: roundNum,
        examDate: date.trim() ? date : null,
      });
      onUpdated({
        kind: updated.kind,
        examYear: updated.examYear,
        examRound: updated.examRound,
        examDate: updated.examDate,
      });
      setMessage(promote ? "기출 복원으로 설정됨" : "AI 모의고사로 되돌림");
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mt-6 rounded-xl border border-border bg-surface p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h2 className="text-sm font-semibold">기출 복원 메타</h2>
          <span
            className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
              isPast
                ? "border border-amber-500/50 bg-amber-500/15 text-amber-300"
                : "border border-border text-muted"
            }`}
          >
            {isPast ? "PAST_EXAM" : "AI"}
          </span>
        </div>
        {message && <span className="text-xs text-muted">{message}</span>}
      </div>

      <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-4">
        <Field label="연도">
          <input
            type="number"
            value={year}
            onChange={(e) => setYear(e.target.value)}
            placeholder="2025"
            className="w-full rounded border border-border bg-background px-3 py-1.5 text-sm"
          />
        </Field>
        <Field label="회차">
          <input
            type="number"
            value={round}
            onChange={(e) => setRound(e.target.value)}
            placeholder="59"
            className="w-full rounded border border-border bg-background px-3 py-1.5 text-sm"
          />
        </Field>
        <Field label="시험일 (선택)">
          <input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-full rounded border border-border bg-background px-3 py-1.5 text-sm"
          />
        </Field>
        <div className="flex items-end gap-2">
          <button
            onClick={() => save(true)}
            disabled={saving}
            className="flex-1 rounded border border-amber-500/40 bg-amber-500/10 px-3 py-1.5 text-xs font-semibold text-amber-300 transition hover:bg-amber-500/20 disabled:opacity-50"
          >
            기출로 저장
          </button>
          {isPast && (
            <button
              onClick={() => save(false)}
              disabled={saving}
              className="flex-1 rounded border border-border px-3 py-1.5 text-xs text-muted transition hover:text-foreground disabled:opacity-50"
            >
              AI 로 되돌리기
            </button>
          )}
        </div>
      </div>
      <p className="mt-2 text-[11px] text-muted">
        PUBLISHED + 전문가 검수 완료 상태여야 <code className="text-amber-300">/past-exams</code> 에 노출됩니다.
      </p>
    </div>
  );
}
