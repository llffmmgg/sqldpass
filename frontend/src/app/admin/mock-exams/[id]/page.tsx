"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { use } from "react";
import { getMockExam, type MockExamDetail } from "@/lib/mockExamApi";
import {
  getQuestion,
  updateQuestion,
  type AdminQuestion,
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

  const [exam, setExam] = useState<MockExamDetail | null>(null);
  const [questions, setQuestions] = useState<AdminQuestion[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const detail = await getMockExam(examId);
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
          <Link
            href="/admin/mock-exams"
            className="text-xs text-muted hover:text-foreground"
          >
            ← 모의고사 목록
          </Link>
          <h1 className="mt-1 text-2xl font-bold">{exam.name}</h1>
          <p className="mt-1 text-sm text-muted">
            {exam.examType} · {exam.totalQuestions}문항 · 회차 #{exam.sequence}
          </p>
        </div>
      </div>

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
  const [content, setContent] = useState(initial.content);
  const [correctOption, setCorrectOption] = useState(initial.correctOption);
  const [explanation, setExplanation] = useState(initial.explanation);
  const [summary, setSummary] = useState(initial.summary ?? "");
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const dirty =
    content !== initial.content ||
    correctOption !== initial.correctOption ||
    explanation !== initial.explanation ||
    (summary || null) !== (initial.summary || null);

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      const updated = await updateQuestion(initial.id, {
        content,
        correctOption,
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
    setCorrectOption(initial.correctOption);
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
