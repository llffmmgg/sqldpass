"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, useRef, useCallback } from "react";
import { isAuthenticated, clearToken, getGenerationStatus, resetGeneration, type GenerationResult, type GenerationStatus } from "@/lib/adminApi";

const SIDEBAR_LINKS = [
  { href: "/admin", label: "대시보드", icon: "📊" },
  { href: "/admin/questions", label: "문제 관리", icon: "📝" },
  { href: "/admin/mock-exams", label: "모의고사", icon: "📚" },
  { href: "/admin/members", label: "회원 관리", icon: "👥" },
  { href: "/admin/feedback", label: "피드백", icon: "📮" },
  { href: "/admin/generate", label: "문제 생성", icon: "🤖" },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [checked, setChecked] = useState(false);
  const [genStatus, setGenStatus] = useState<GenerationStatus | null>(null);
  const [completedResult, setCompletedResult] = useState<GenerationResult | null>(null);
  const [failedMessage, setFailedMessage] = useState<string | null>(null);
  const [connectionError, setConnectionError] = useState(false);
  const prevStatus = useRef<string>("IDLE");
  const failCount = useRef(0);

  useEffect(() => {
    if (pathname === "/admin/login") {
      setChecked(true);
      return;
    }
    if (!isAuthenticated()) {
      router.replace("/admin/login");
      return;
    }
    setChecked(true);
  }, [pathname, router]);

  const pollStatus = useCallback(() => {
    if (!isAuthenticated() || pathname === "/admin/login") return;

    getGenerationStatus().then((status) => {
      failCount.current = 0;
      setConnectionError(false);
      setGenStatus(status);

      // RUNNING → COMPLETED
      if (prevStatus.current === "RUNNING" && status.status === "COMPLETED" && status.result) {
        try {
          setCompletedResult(JSON.parse(status.result) as GenerationResult);
        } catch { /* ignore */ }
      }

      // RUNNING → FAILED
      if (prevStatus.current === "RUNNING" && status.status === "FAILED") {
        setFailedMessage(status.result || "알 수 없는 오류가 발생했습니다.");
      }

      // 페이지 진입 시 이미 COMPLETED/FAILED인 경우
      if (prevStatus.current === "IDLE" && status.status === "COMPLETED" && status.result) {
        try {
          setCompletedResult(JSON.parse(status.result) as GenerationResult);
        } catch { /* ignore */ }
      }
      if (prevStatus.current === "IDLE" && status.status === "FAILED") {
        setFailedMessage(status.result || "알 수 없는 오류가 발생했습니다.");
      }

      prevStatus.current = status.status;
    }).catch(() => {
      failCount.current++;
      if (failCount.current >= 3) setConnectionError(true);
    });
  }, [pathname]);

  useEffect(() => {
    if (pathname === "/admin/login") return;
    pollStatus();
    const interval = setInterval(pollStatus, 5000);
    return () => clearInterval(interval);
  }, [pathname, pollStatus]);

  function handleDismiss() {
    resetGeneration();
    setCompletedResult(null);
    setFailedMessage(null);
    prevStatus.current = "IDLE";
  }

  if (!checked) return null;
  if (pathname === "/admin/login") return <>{children}</>;

  function handleLogout() {
    clearToken();
    router.push("/admin/login");
  }

  // stale 감지는 백엔드(30분)에 일임 — 백엔드가 stale 감지 시 자동으로 FAILED로 전환하므로
  // 프론트에서 로컬 판정하지 않는다. 이전 버그: 프론트 15분 vs 백엔드 30분 불일치로 경고 오탐.
  const isRunning = genStatus?.status === "RUNNING";

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <aside className="w-56 shrink-0 border-r border-border bg-surface p-4">
        <Link href="/admin" className="block text-lg font-bold">
          SQLD <span className="text-primary">Admin</span>
        </Link>

        <nav className="mt-6 space-y-1">
          {SIDEBAR_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                (link.href === "/admin" ? pathname === "/admin" : pathname.startsWith(link.href))
                  ? "bg-primary/10 text-primary"
                  : "text-muted hover:text-foreground"
              }`}
            >
              <span>{link.icon}</span>
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="mt-auto pt-6">
          <button
            onClick={handleLogout}
            className="w-full rounded-lg border border-border px-3 py-2 text-sm text-muted transition hover:text-foreground"
          >
            로그아웃
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col">
        {connectionError && (
          <div className="border-b border-red-500/30 bg-red-500/5 px-6 py-2 text-sm text-red-400">
            서버 연결에 실패했습니다. 네트워크를 확인하세요.
          </div>
        )}

        {isRunning && (
          <div className="border-b border-amber-500/30 bg-amber-500/5 px-6 py-2 text-sm text-amber-400">
            AI 문제 자동 생성이 진행 중입니다...
          </div>
        )}

        {completedResult && (
          <div className="border-b border-green-500/30 bg-green-500/5 px-6 py-3 flex items-center justify-between">
            <span className="text-sm text-green-400">
              AI 문제 생성 완료! 생성 {completedResult.totalGenerated}개 / 저장 {completedResult.totalSaved}개
              {completedResult.errors.length > 0 && ` / 오류 ${completedResult.errors.length}건`}
            </span>
            <button
              onClick={handleDismiss}
              className="rounded border border-green-500/30 px-3 py-1 text-xs text-green-400 hover:bg-green-500/10"
            >
              확인
            </button>
          </div>
        )}

        {failedMessage && !completedResult && (
          <div className="border-b border-red-500/30 bg-red-500/5 px-6 py-3 flex items-center justify-between">
            <span className="text-sm text-red-400">
              AI 문제 생성 실패: {failedMessage}
            </span>
            <button
              onClick={handleDismiss}
              className="rounded border border-red-500/30 px-3 py-1 text-xs text-red-400 hover:bg-red-500/10"
            >
              확인
            </button>
          </div>
        )}

        <main className="flex-1 p-8">{children}</main>
      </div>
    </div>
  );
}
