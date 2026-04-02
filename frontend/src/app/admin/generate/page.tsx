"use client";

import { useState, useEffect } from "react";
import { generateQuestions, getGenerationStatus } from "@/lib/adminApi";

export default function AdminGeneratePage() {
  const [count, setCount] = useState(3);
  const [status, setStatus] = useState<string>("IDLE");
  const [message, setMessage] = useState("");

  useEffect(() => {
    getGenerationStatus().then((s) => setStatus(s.status));
  }, []);

  useEffect(() => {
    if (status !== "RUNNING") return;
    const interval = setInterval(() => {
      getGenerationStatus().then((s) => setStatus(s.status));
    }, 5000);
    return () => clearInterval(interval);
  }, [status]);

  async function handleGenerate() {
    if (!confirm("AI 문제 생성을 시작하시겠습니까?")) return;
    setMessage("");
    try {
      await generateQuestions(count);
      setStatus("RUNNING");
      setMessage("생성이 시작되었습니다. 다른 페이지로 이동해도 괜찮습니다.");
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "요청에 실패했습니다.");
    }
  }

  const isRunning = status === "RUNNING";

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold">AI 문제 생성</h1>
      <p className="mt-2 text-sm text-muted">
        모든 과목에 대해 선택한 개수만큼 AI가 문제를 생성합니다.
        생성 완료/실패 시 상단에 알림이 표시됩니다.
      </p>

      <div className="mt-6 flex items-center gap-4">
        <label className="text-sm text-muted">과목당 토픽 수</label>
        <select
          value={count}
          onChange={(e) => setCount(Number(e.target.value))}
          disabled={isRunning}
          className="rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-primary focus:outline-none"
        >
          {[1, 2, 3, 5].map((n) => (
            <option key={n} value={n}>{n}개 (토픽당 3문제 = {n * 3}문제/과목)</option>
          ))}
        </select>
      </div>

      {isRunning && (
        <div className="mt-4 rounded-lg border border-amber-500/30 bg-amber-500/5 px-4 py-3 text-sm text-amber-400">
          문제 생성이 진행 중입니다. 완료되면 상단에 알림이 표시됩니다.
        </div>
      )}

      <button
        onClick={handleGenerate}
        disabled={isRunning}
        className="mt-4 rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover disabled:opacity-50"
      >
        {isRunning ? "생성 진행 중..." : "문제 생성 시작"}
      </button>

      {message && (
        <p className="mt-4 text-sm text-muted">{message}</p>
      )}
    </div>
  );
}
