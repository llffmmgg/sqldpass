"use client";

import { useState } from "react";
import { generateQuestions, type GenerationResult } from "@/lib/adminApi";

export default function AdminGeneratePage() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<GenerationResult | null>(null);
  const [error, setError] = useState("");

  async function handleGenerate() {
    if (!confirm("AI 문제 생성을 시작하시겠습니까?")) return;
    setLoading(true);
    setError("");
    setResult(null);
    try {
      const res = await generateQuestions();
      setResult(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : "생성에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold">AI 문제 생성</h1>
      <p className="mt-2 text-sm text-muted">
        모든 리프 과목에 대해 AI가 문제를 생성합니다.
      </p>

      <button
        onClick={handleGenerate}
        disabled={loading}
        className="mt-6 rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover disabled:opacity-50"
      >
        {loading ? "생성 중... (1~2분 소요)" : "문제 생성 시작"}
      </button>

      {error && (
        <div className="mt-4 rounded-lg border border-red-500/30 bg-red-500/5 px-4 py-3 text-sm text-red-400">
          {error}
        </div>
      )}

      {result && (
        <div className="mt-6 space-y-4">
          <div className="rounded-xl border border-border bg-surface p-6">
            <h2 className="text-lg font-semibold">생성 결과</h2>
            <div className="mt-4 grid grid-cols-3 gap-4">
              <div>
                <p className="text-sm text-muted">생성</p>
                <p className="text-2xl font-bold text-amber-400">{result.totalGenerated}</p>
              </div>
              <div>
                <p className="text-sm text-muted">검증 통과</p>
                <p className="text-2xl font-bold text-green-400">{result.totalVerified}</p>
              </div>
              <div>
                <p className="text-sm text-muted">저장</p>
                <p className="text-2xl font-bold text-violet-400">{result.totalSaved}</p>
              </div>
            </div>
          </div>

          {result.errors.length > 0 && (
            <div className="rounded-lg border border-red-500/30 bg-red-500/5 px-4 py-3">
              <p className="text-sm font-medium text-red-400">오류</p>
              <ul className="mt-1 space-y-1">
                {result.errors.map((err, i) => (
                  <li key={i} className="text-sm text-muted">{err}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
