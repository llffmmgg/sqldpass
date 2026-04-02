"use client";

import { useState, useRef, useEffect } from "react";
import { getGenerationStatus, type GenerationResult } from "@/lib/adminApi";

interface ProgressEvent {
  type: "progress" | "error";
  message: string;
}

export default function AdminGeneratePage() {
  const [count, setCount] = useState(3);
  const [running, setRunning] = useState(false);
  const [alreadyRunning, setAlreadyRunning] = useState(false);
  const [logs, setLogs] = useState<{ type: string; message: string }[]>([]);
  const [result, setResult] = useState<GenerationResult | null>(null);
  const logsEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    getGenerationStatus().then((status) => {
      if (status.running) {
        setAlreadyRunning(true);
      }
    });
  }, []);

  function addLog(type: string, message: string) {
    setLogs((prev) => [...prev, { type, message }]);
    setTimeout(() => logsEndRef.current?.scrollIntoView({ behavior: "smooth" }), 50);
  }

  async function handleGenerate() {
    if (!confirm("AI 문제 생성을 시작하시겠습니까?")) return;
    setRunning(true);
    setLogs([]);
    setResult(null);

    const token = localStorage.getItem("admin_token");

    try {
      const response = await fetch(`/api/admin/generate?count=${count}`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        addLog("error", `요청 실패: ${response.status}`);
        setRunning(false);
        return;
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        addLog("error", "스트림을 읽을 수 없습니다.");
        setRunning(false);
        return;
      }

      let buffer = "";
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() || "";

        let currentEventName = "";
        for (const line of lines) {
          if (line.startsWith("event:")) {
            currentEventName = line.slice(6).trim();
          } else if (line.startsWith("data:")) {
            const data = line.slice(5).trim();
            try {
              const parsed = JSON.parse(data);
              if (currentEventName === "complete") {
                setResult(parsed);
                addLog("complete", "생성 완료!");
              } else if (parsed.type === "progress") {
                addLog("progress", parsed.message);
              } else if (parsed.type === "error") {
                addLog("error", parsed.message);
              }
            } catch {
              // non-JSON line, skip
            }
            currentEventName = "";
          }
        }
      }
    } catch (err) {
      addLog("error", err instanceof Error ? err.message : "연결 실패");
    } finally {
      setRunning(false);
      setAlreadyRunning(false);
    }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold">AI 문제 생성</h1>
      <p className="mt-2 text-sm text-muted">
        모든 과목에 대해 선택한 개수만큼 AI가 문제를 생성합니다.
      </p>

      <div className="mt-6 flex items-center gap-4">
        <label className="text-sm text-muted">과목당 문제 수</label>
        <select
          value={count}
          onChange={(e) => setCount(Number(e.target.value))}
          disabled={running}
          className="rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-primary focus:outline-none"
        >
          {[1, 2, 3, 5, 10].map((n) => (
            <option key={n} value={n}>{n}개</option>
          ))}
        </select>
        <span className="text-xs text-muted">
          (5과목 × {count}개 = 총 {5 * count}문제)
        </span>
      </div>

      {alreadyRunning && (
        <div className="mt-4 rounded-lg border border-amber-500/30 bg-amber-500/5 px-4 py-3 text-sm text-amber-400">
          현재 다른 요청에 의해 문제 생성이 진행 중입니다.
        </div>
      )}

      <button
        onClick={handleGenerate}
        disabled={running || alreadyRunning}
        className="mt-4 rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover disabled:opacity-50"
      >
        {running ? "생성 진행 중..." : alreadyRunning ? "생성 진행 중 (다른 요청)" : "문제 생성 시작"}
      </button>

      {/* Progress logs */}
      {logs.length > 0 && (
        <div className="mt-6 max-h-64 overflow-y-auto rounded-lg border border-border bg-surface p-4 font-mono text-xs">
          {logs.map((log, i) => (
            <div
              key={i}
              className={`py-0.5 ${
                log.type === "error"
                  ? "text-red-400"
                  : log.type === "complete"
                  ? "text-green-400 font-semibold"
                  : "text-muted"
              }`}
            >
              {log.message}
            </div>
          ))}
          <div ref={logsEndRef} />
        </div>
      )}

      {/* Result */}
      {result && (
        <div className="mt-4 rounded-xl border border-border bg-surface p-6">
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

          {result.errors.length > 0 && (
            <div className="mt-4 rounded-lg border border-red-500/30 bg-red-500/5 px-4 py-3">
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
