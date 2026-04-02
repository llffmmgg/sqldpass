"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, useRef, useCallback } from "react";
import { isAuthenticated, clearToken, getGenerationStatus, clearGenerationResult, type GenerationResult } from "@/lib/adminApi";

const SIDEBAR_LINKS = [
  { href: "/admin", label: "대시보드", icon: "📊" },
  { href: "/admin/questions", label: "문제 관리", icon: "📝" },
  { href: "/admin/members", label: "회원 관리", icon: "👥" },
  { href: "/admin/generate", label: "문제 생성", icon: "🤖" },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [checked, setChecked] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [completedResult, setCompletedResult] = useState<GenerationResult | null>(null);
  const wasRunning = useRef(false);

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

  // 전역 폴링
  const pollStatus = useCallback(() => {
    if (!isAuthenticated() || pathname === "/admin/login") return;

    getGenerationStatus().then((status) => {
      setGenerating(status.running);

      if (wasRunning.current && !status.running && status.result) {
        try {
          const result = JSON.parse(status.result) as GenerationResult;
          setCompletedResult(result);
        } catch {
          // ignore parse error
        }
      }
      wasRunning.current = status.running;
    }).catch(() => {});
  }, [pathname]);

  useEffect(() => {
    if (pathname === "/admin/login") return;
    pollStatus();
    const interval = setInterval(pollStatus, 5000);
    return () => clearInterval(interval);
  }, [pathname, pollStatus]);

  function handleDismissResult() {
    clearGenerationResult();
    setCompletedResult(null);
  }

  if (!checked) return null;

  if (pathname === "/admin/login") {
    return <>{children}</>;
  }

  function handleLogout() {
    clearToken();
    router.push("/admin/login");
  }

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {/* Sidebar */}
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

      {/* Main content */}
      <div className="flex-1 flex flex-col">
        {/* 생성 진행 중 배너 */}
        {generating && (
          <div className="border-b border-amber-500/30 bg-amber-500/5 px-6 py-2 text-sm text-amber-400">
            문제 생성이 진행 중입니다...
          </div>
        )}

        {/* 생성 완료 알림 */}
        {completedResult && (
          <div className="border-b border-green-500/30 bg-green-500/5 px-6 py-3 flex items-center justify-between">
            <span className="text-sm text-green-400">
              문제 생성 완료! 생성 {completedResult.totalGenerated}개 / 저장 {completedResult.totalSaved}개
              {completedResult.errors.length > 0 && ` / 오류 ${completedResult.errors.length}건`}
            </span>
            <button
              onClick={handleDismissResult}
              className="rounded border border-green-500/30 px-3 py-1 text-xs text-green-400 hover:bg-green-500/10"
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
