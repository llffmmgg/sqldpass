"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, useRef, useCallback } from "react";
import { isAuthenticated, clearToken, getGenerationStatus, resetGeneration, type GenerationResult, type GenerationStatus } from "@/lib/adminApi";

type NavItem = { href: string; label: string; icon: React.ReactNode };
type NavSection = { label: string; items: NavItem[] };

const ICON = {
  dashboard: (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75Zm6.75-4.5c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625Zm6.75-4.5c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" />
    </svg>
  ),
  questions: (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5.586a1 1 0 0 1 .707.293l5.414 5.414a1 1 0 0 1 .293.707V19a2 2 0 0 1-2 2z" />
    </svg>
  ),
  exams: (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.042A8.967 8.967 0 0 0 6 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 0 1 6 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 0 1 6-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0 0 18 18a8.967 8.967 0 0 0-6 2.292m0-14.25v14.25" />
    </svg>
  ),
  pastExams: (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
    </svg>
  ),
  members: (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
    </svg>
  ),
  feedback: (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 12.76c0 1.6 1.123 2.994 2.707 3.227 1.068.157 2.148.279 3.238.364.466.037.893.281 1.153.671L12 21l2.652-3.978c.26-.39.687-.634 1.153-.67 1.09-.086 2.17-.208 3.238-.365 1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0 0 12 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018Z" />
    </svg>
  ),
  notices: (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M10.34 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 0 1 1.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.56.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.894.149c-.424.07-.764.383-.929.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 0 1-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.398.165-.71.505-.781.929l-.149.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 0 1-.12-1.45l.527-.737c.25-.35.272-.806.108-1.204-.166-.397-.506-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.764-.383.93-.78.165-.398.143-.854-.108-1.204l-.526-.738a1.125 1.125 0 0 1 .12-1.45l.773-.773a1.125 1.125 0 0 1 1.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894Z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
    </svg>
  ),
  generate: (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="m9.813 15.904-.75.363-.75-.363a.75.75 0 0 1 0-1.336l.75-.363.75.363a.75.75 0 0 1 0 1.336Zm3.75-3.75-.75.363-.75-.363a.75.75 0 0 1 0-1.336l.75-.363.75.363a.75.75 0 0 1 0 1.336Z M12 9l-1.91 5.818a2 2 0 0 1-1.273 1.272L3 18l5.818 1.91a2 2 0 0 1 1.272 1.273L12 27l1.91-5.818a2 2 0 0 1 1.273-1.272L21 18l-5.818-1.91a2 2 0 0 1-1.272-1.273L12 9ZM18 3v4M22 5h-4" />
    </svg>
  ),
} as const;

const SIDEBAR_SECTIONS: NavSection[] = [
  {
    label: "개요",
    items: [{ href: "/admin", label: "대시보드", icon: ICON.dashboard }],
  },
  {
    label: "콘텐츠",
    items: [
      { href: "/admin/questions", label: "문제 관리", icon: ICON.questions },
      { href: "/admin/mock-exams", label: "모의고사", icon: ICON.exams },
      { href: "/admin/past-exams", label: "기출 복원", icon: ICON.pastExams },
      // /admin/generate 는 모의고사 생성으로 대체. 메뉴 숨김(페이지 코드는 유지).
    ],
  },
  {
    label: "커뮤니티",
    items: [
      { href: "/admin/members", label: "회원 관리", icon: ICON.members },
      { href: "/admin/feedback", label: "피드백", icon: ICON.feedback },
      { href: "/admin/notices", label: "공지사항", icon: ICON.notices },
    ],
  },
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

  const isActive = (href: string) =>
    href === "/admin" ? pathname === "/admin" : pathname.startsWith(href);

  const sidebarNav = (
    <nav className="flex flex-col gap-5">
      {SIDEBAR_SECTIONS.map((section) => (
        <div key={section.label}>
          <p className="mb-2 px-3 text-[10px] font-semibold uppercase tracking-[0.12em] text-muted/70">
            {section.label}
          </p>
          <div className="space-y-0.5">
            {section.items.map((link) => {
              const active = isActive(link.href);
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`group relative flex items-center gap-2.5 rounded-md px-3 py-2 text-sm transition-colors ${
                    active
                      ? "bg-primary/10 text-primary font-semibold"
                      : "text-muted hover:bg-surface hover:text-foreground"
                  }`}
                >
                  {active && (
                    <span
                      className="absolute left-0 top-1.5 bottom-1.5 w-0.5 rounded-r bg-primary"
                      aria-hidden="true"
                    />
                  )}
                  <span className={active ? "text-primary" : "text-muted group-hover:text-foreground"}>
                    {link.icon}
                  </span>
                  {link.label}
                </Link>
              );
            })}
          </div>
        </div>
      ))}
    </nav>
  );

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {/* 데스크탑 사이드바 */}
      <aside className="hidden w-60 shrink-0 border-r border-border bg-surface/60 md:flex md:flex-col">
        <Link
          href="/admin"
          className="flex items-center gap-2 border-b border-border px-5 py-4"
        >
          <span
            className="flex h-7 w-7 items-center justify-center rounded-md bg-primary/10 text-xs font-bold text-primary"
            aria-hidden="true"
          >
            S
          </span>
          <span className="text-base font-bold tracking-tight">
            sqldpass <span className="font-normal text-muted">/ admin</span>
          </span>
        </Link>

        <div className="flex-1 overflow-y-auto px-3 py-5">{sidebarNav}</div>

        <div className="border-t border-border px-3 py-3">
          <button
            onClick={handleLogout}
            className="flex w-full items-center justify-center gap-1.5 rounded-md border border-border bg-background/40 px-3 py-2 text-xs font-medium text-muted transition-colors hover:border-foreground/30 hover:text-foreground"
          >
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15m3 0 3-3m0 0-3-3m3 3H9" />
            </svg>
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
          <aside className="fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-border bg-surface md:hidden">
            <div className="flex items-center justify-between border-b border-border px-5 py-4">
              <Link
                href="/admin"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-2"
              >
                <span className="flex h-7 w-7 items-center justify-center rounded-md bg-primary/10 text-xs font-bold text-primary">
                  S
                </span>
                <span className="text-base font-bold tracking-tight">
                  sqldpass <span className="font-normal text-muted">/ admin</span>
                </span>
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
            <div className="flex-1 overflow-y-auto px-3 py-5">{sidebarNav}</div>
            <div className="border-t border-border px-3 py-3">
              <button
                onClick={() => { handleLogout(); setMobileMenuOpen(false); }}
                className="flex w-full items-center justify-center gap-1.5 rounded-md border border-border bg-background/40 px-3 py-2 text-xs font-medium text-muted transition-colors hover:border-foreground/30 hover:text-foreground"
              >
                <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15m3 0 3-3m0 0-3-3m3 3H9" />
                </svg>
                로그아웃
              </button>
            </div>
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
          <Link href="/admin" className="flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded bg-primary/10 text-[11px] font-bold text-primary">
              S
            </span>
            <span className="text-sm font-bold tracking-tight">
              sqldpass <span className="font-normal text-muted">/ admin</span>
            </span>
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
