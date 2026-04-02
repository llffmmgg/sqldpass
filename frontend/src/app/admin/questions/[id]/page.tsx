"use client";

import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";
import { getQuestion, updateQuestion, type AdminQuestion } from "@/lib/adminApi";

export default function AdminQuestionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const [question, setQuestion] = useState<AdminQuestion | null>(null);
  const [content, setContent] = useState("");
  const [correctOption, setCorrectOption] = useState(1);
  const [explanation, setExplanation] = useState("");
  const [summary, setSummary] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    getQuestion(Number(id)).then((q) => {
      setQuestion(q);
      setContent(q.content);
      setCorrectOption(q.correctOption);
      setExplanation(q.explanation || "");
      setSummary(q.summary || "");
    });
  }, [id]);

  async function handleSave() {
    setSaving(true);
    setMessage("");
    try {
      await updateQuestion(Number(id), { content, correctOption, explanation, summary: summary || null });
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

  return (
    <div className="max-w-2xl">
      <button
        onClick={() => router.push("/admin/questions")}
        className="text-sm text-muted hover:text-foreground transition-colors"
      >
        &larr; 목록으로
      </button>

      <h1 className="mt-4 text-2xl font-bold">문제 수정</h1>
      <p className="mt-1 text-sm text-muted">
        {question.subjectName} &middot; ID: {question.id}
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

        <div>
          <label className="block text-sm font-medium text-muted">해설</label>
          <textarea
            value={explanation}
            onChange={(e) => setExplanation(e.target.value)}
            rows={4}
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
