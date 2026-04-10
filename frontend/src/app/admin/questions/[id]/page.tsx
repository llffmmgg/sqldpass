"use client";

import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";
import {
  getQuestion,
  updateQuestion,
  type AdminQuestion,
  type AdminQuestionType,
} from "@/lib/adminApi";

export default function AdminQuestionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const [question, setQuestion] = useState<AdminQuestion | null>(null);
  const [content, setContent] = useState("");
  const [questionType, setQuestionType] = useState<AdminQuestionType>("MCQ");
  const [correctOption, setCorrectOption] = useState<number>(1);
  const [answer, setAnswer] = useState("");
  const [keywordsText, setKeywordsText] = useState("");
  const [explanation, setExplanation] = useState("");
  const [summary, setSummary] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    getQuestion(Number(id)).then((q) => {
      setQuestion(q);
      setContent(q.content);
      setQuestionType(q.questionType);
      setCorrectOption(q.correctOption ?? 1);
      setAnswer(q.answer ?? "");
      setKeywordsText((q.keywords ?? []).join(", "));
      setExplanation(q.explanation || "");
      setSummary(q.summary || "");
    });
  }, [id]);

  async function handleSave() {
    setSaving(true);
    setMessage("");
    try {
      const isMcq = questionType === "MCQ";
      const keywords = keywordsText
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
      await updateQuestion(Number(id), {
        content,
        questionType,
        correctOption: isMcq ? correctOption : null,
        answer: isMcq ? null : (answer || null),
        keywords: isMcq ? null : (keywords.length > 0 ? keywords : null),
        explanation,
        summary: summary || null,
      });
      setMessage("저장되었습니다.");
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  if (!question) {
    return <p className="text-muted">로딩 중...</p>;
  }

  const isMcq = questionType === "MCQ";

  return (
    <div className="max-w-2xl">
      <button
        onClick={() => router.push("/admin/questions")}
        className="text-sm text-muted hover:text-foreground transition-colors"
      >
        &larr; 목록으로
      </button>

      <h1 className="mt-4 text-2xl font-bold">문제 수정</h1>
      <p className="mt-1 flex flex-wrap items-center gap-2 text-sm text-muted">
        <span>{question.subjectName} &middot; ID: {question.id}</span>
        <span className="rounded bg-violet-500/10 px-1.5 py-0.5 text-xs text-violet-300">
          {questionType}
        </span>
        {question.verifiedAt ? (
          <span className="rounded bg-green-500/10 px-1.5 py-0.5 text-xs font-medium text-green-400">
            검수 완료
          </span>
        ) : (
          <span className="rounded bg-amber-500/10 px-1.5 py-0.5 text-xs font-medium text-amber-400">
            미검수
          </span>
        )}
      </p>

      <div className="mt-6 space-y-4">
        <div>
          <label className="block text-sm font-medium text-muted">문제 내용</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={10}
            className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm font-mono focus:border-primary focus:outline-none"
          />
        </div>

        {isMcq ? (
          <div>
            <label className="block text-sm font-medium text-muted">정답 번호 (1~4)</label>
            <select
              value={correctOption}
              onChange={(e) => setCorrectOption(Number(e.target.value))}
              className="mt-1 rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-primary focus:outline-none"
            >
              {[1, 2, 3, 4].map((n) => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
          </div>
        ) : (
          <>
            <div>
              <label className="block text-sm font-medium text-muted">정답 (모범답안)</label>
              <textarea
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                rows={3}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted">
                키워드 / Alias <span className="text-xs text-muted/70">(쉼표로 구분 — 단답형은 허용 alias, 약술형은 채점 키워드)</span>
              </label>
              <input
                value={keywordsText}
                onChange={(e) => setKeywordsText(e.target.value)}
                placeholder="예: OTU, O T U, otu"
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-primary focus:outline-none"
              />
            </div>
          </>
        )}

        <div>
          <label className="block text-sm font-medium text-muted">해설</label>
          <textarea
            value={explanation}
            onChange={(e) => setExplanation(e.target.value)}
            rows={6}
            className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-primary focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-muted">요약 (출제 관점)</label>
          <input
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
            className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-primary focus:outline-none"
          />
        </div>

        {message && (
          <p className={`text-sm ${message.includes("실패") ? "text-red-400" : "text-green-400"}`}>
            {message}
          </p>
        )}

        <button
          onClick={handleSave}
          disabled={saving}
          className="rounded-lg bg-primary px-6 py-2 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover disabled:opacity-50"
        >
          {saving ? "저장 중..." : "저장"}
        </button>
      </div>
    </div>
  );
}
