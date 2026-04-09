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
  { href: "/admin/notices", label: "공지사항", icon: "📢" },
  { href: "/admin/generate", label: "문제 생성", icon: "🤖" },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [checked, setChecked] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
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
    setMobileMenuOpen(false);
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

  const sidebarNav = (
    <nav className="space-y-1">
      {SIDEBAR_LINKS.map((link) => (
        <Link
          key={link.href}
          href={link.href}
          onClick={() => setMobileMenuOpen(false)}
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
  );

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {/* 데스크탑 사이드바 */}
      <aside className="hidden w-56 shrink-0 border-r border-border bg-surface p-4 md:flex md:flex-col">
        <Link href="/admin" className="block text-lg font-bold">
          SQLD <span className="text-primary">Admin</span>
        </Link>

        <div className="mt-6">{sidebarNav}</div>

        <div className="mt-auto pt-6">
          <button
            onClick={handleLogout}
            className="w-full rounded-lg border border-border px-3 py-2 text-sm text-muted transition hover:text-foreground"
          >
            로그아웃
          </button>
        </div>
      </aside>

      {/* 모바일 드로어 */}
      {mobileMenuOpen && (
        <>
          <div
            className="fixed inset-0 z-40 bg-black/50 md:hidden"
            onClick={() => setMobileMenuOpen(false)}
            aria-hidden="true"
          />
          <aside className="fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-border bg-surface p-4 md:hidden">
            <div className="flex items-center justify-between">
              <Link
                href="/admin"
                onClick={() => setMobileMenuOpen(false)}
                className="text-lg font-bold"
              >
                SQLD <span className="text-primary">Admin</span>
              </Link>
              <button
                onClick={() => setMobileMenuOpen(false)}
                className="rounded p-1 text-muted hover:text-foreground"
                aria-label="닫기"
              >
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="mt-6 flex-1">{sidebarNav}</div>
            <button
              onClick={() => { handleLogout(); setMobileMenuOpen(false); }}
              className="w-full rounded-lg border border-border px-3 py-2 text-sm text-muted transition hover:text-foreground"
            >
              로그아웃
            </button>
          </aside>
        </>
      )}

      <div className="flex flex-1 flex-col min-w-0">
        {/* 모바일 상단 헤더 (햄버거 + 로고) */}
        <header className="sticky top-0 z-30 flex items-center justify-between border-b border-border bg-surface/95 px-4 py-3 backdrop-blur md:hidden">
          <button
            onClick={() => setMobileMenuOpen(true)}
            className="rounded p-1 text-muted hover:text-foreground"
            aria-label="메뉴 열기"
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <Link href="/admin" className="text-base font-bold">
            SQLD <span className="text-primary">Admin</span>
          </Link>
          <div className="w-8" />
        </header>

        {connectionError && (
          <div className="border-b border-red-500/30 bg-red-500/5 px-4 py-2 text-xs text-red-400 sm:px-6 sm:text-sm">
            서버 연결에 실패했습니다. 네트워크를 확인하세요.
          </div>
        )}

        {isRunning && (
          <div className="border-b border-amber-500/30 bg-amber-500/5 px-4 py-2 text-xs text-amber-400 sm:px-6 sm:text-sm">
            AI 문제 자동 생성이 진행 중입니다...
          </div>
        )}

        {completedResult && (
          <div className="flex flex-col items-start justify-between gap-2 border-b border-green-500/30 bg-green-500/5 px-4 py-3 sm:flex-row sm:items-center sm:gap-0 sm:px-6">
            <span className="text-xs text-green-400 sm:text-sm">
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
          <div className="flex flex-col items-start justify-between gap-2 border-b border-red-500/30 bg-red-500/5 px-4 py-3 sm:flex-row sm:items-center sm:gap-0 sm:px-6">
            <span className="text-xs text-red-400 sm:text-sm">
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

        <main className="flex-1 p-4 sm:p-6 md:p-8">{children}</main>
      </div>
    </div>
  );
}
