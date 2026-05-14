"use client";

/* eslint-disable react-hooks/set-state-in-effect -- localStorage 인증 상태와 메뉴 토글 동기화는 마운트 effect 안에서 setState 가 자연스러움 */

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { isLoggedIn, getNickname, clearAuth } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";
import { type Theme, getInitialTheme, setStoredTheme, applyTheme } from "@/lib/theme";
import FeedbackModal from "@/components/FeedbackModal";
import NotificationBell from "@/components/NotificationBell";
import { CERT_LIST, slugFromCert, type CertKey } from "@/lib/cert-tokens";
import { useMockExamNewSignal } from "@/lib/hooks/useMockExamNewSignal";

type MenuItem = { href: string; label: string; description?: string };

type NavItem =
  | { kind: "link"; href: string; label: string; accent?: "gold" }
  | { kind: "dropdown"; label: string; basePath: string; build: (cert: CertKey) => string }
  | { kind: "menu"; label: string; basePath: string; activePaths: string[]; items: MenuItem[] };

const NAV_LINKS: NavItem[] = [
  { kind: "link", href: "/", label: "홈" },
  { kind: "dropdown", label: "문제", basePath: "/solve", build: (cert) => `/solve?cert=${cert}` },
  { kind: "dropdown", label: "모의고사", basePath: "/mock-exams", build: (cert) => `/mock-exams?cert=${cert}` },
  { kind: "dropdown", label: "기출", basePath: "/past-exams", build: (cert) => `/past-exams/${slugFromCert(cert)}` },
  { kind: "link", href: "/dashboard", label: "대시보드" },
  { kind: "link", href: "/wrong-answers", label: "오답 노트" },
  {
    kind: "menu",
    label: "게시판",
    basePath: "/board",
    activePaths: ["/board", "/blog"],
    items: [
      { href: "/board", label: "합격 후기", description: "선배들의 합격 노트와 팁" },
      { href: "/blog", label: "시험 준비 팁", description: "출제 경향·공부법·합격 노하우" },
    ],
  },
  { kind: "link", href: "/plan", label: "플랜", accent: "gold" },
];

// 골드 sparkle — accent: "gold" 탭(현재 /plan)에만 붙는 4점 별빛 아이콘.
function GoldSparkle({ className = "" }: { className?: string }) {
  return (
    <svg aria-hidden viewBox="0 0 24 24" fill="currentColor" className={className}>
      <path d="M12 0 L14 10 L24 12 L14 14 L12 24 L10 14 L0 12 L10 10 Z" />
    </svg>
  );
}

// "이 드롭다운에 NEW 신호를 붙일지" — 모의고사 신호만 신뢰. /past-exams 는
// 모의고사 추가로 잘못 NEW 가 뜨던 이슈가 있어 제외 (기출 카탈로그 페이지의
// 카드별 NEW 가 진짜 신호를 담당).
const DROPDOWN_NEW_SIGNAL = new Set<string>(["/mock-exams"]);

export default function NavBar() {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);
  const [nickname, setNickname] = useState<string | null>(null);
  const [theme, setTheme] = useState<Theme>("dark");
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  // 모바일 아코디언 — 현재 열린 드롭다운 basePath (하나만 열림). null = 전부 닫힘
  const [openMobileDropdown, setOpenMobileDropdown] = useState<string | null>(null);
  const { newCount, hasNewByCert } = useMockExamNewSignal();

  useEffect(() => {
    setLoggedIn(isLoggedIn());
    setNickname(getNickname());
    setTheme(getInitialTheme());
  }, [pathname]);

  useEffect(() => {
    if (!menuOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [menuOpen]);

  // 햄버거 메뉴가 열리는 순간 현재 경로와 매칭되는 드롭다운 하나만 펼쳐 둠
  useEffect(() => {
    if (!menuOpen) {
      setOpenMobileDropdown(null);
      return;
    }
    const match = NAV_LINKS.find((item) => {
      if (item.kind === "dropdown") return pathname?.startsWith(item.basePath);
      if (item.kind === "menu") return item.activePaths.some((p) => pathname?.startsWith(p));
      return false;
    });
    setOpenMobileDropdown(
      match && (match.kind === "dropdown" || match.kind === "menu") ? match.basePath : null,
    );
  }, [menuOpen, pathname]);

  function toggleTheme() {
    const next = theme === "dark" ? "light" : "dark";
    setTheme(next);
    setStoredTheme(next);
    applyTheme(next);
  }

  function isActive(href: string) {
    if (href === "/") return pathname === "/";
    return pathname.startsWith(href);
  }

  function handleLogin() {
    window.location.href = getGoogleLoginUrl();
  }

  function handleLogout() {
    clearAuth();
    setLoggedIn(false);
    setNickname(null);
    router.push("/");
  }

  if (pathname.startsWith("/admin")) return null;

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-bg">
      <nav className="mx-auto flex h-14 max-w-7xl flex-nowrap items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
        <Link
          href="/"
          className="flex shrink-0 items-center gap-2 text-lg font-bold tracking-tight text-text"
        >
          <Image
            src="/logo/logo.webp"
            alt="문어CBT"
            width={160}
            height={90}
            sizes="80px"
            className="h-11 w-20 rounded-md object-contain"
            priority
          />
          <span>
            <span className="font-black">문어</span>
            <span className="font-mono text-primary">CBT</span>
          </span>
        </Link>

        {/* Desktop (lg 이상에서만 전체 메뉴 — 그 아래는 햄버거로 전환) */}
        <div className="hidden items-center gap-1 lg:flex">
          <ul className="flex items-center gap-0.5">
            {NAV_LINKS.map((item) => {
              if (item.kind === "link") {
                const gold = item.accent === "gold";
                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      className={`flex h-9 items-center gap-1.5 whitespace-nowrap rounded-md px-3 text-sm font-medium transition-colors ${
                        gold
                          ? "text-amber-500 hover:bg-surface-hover dark:text-amber-300"
                          : isActive(item.href)
                            ? "bg-primary/10 text-primary"
                            : "text-text-muted hover:text-text hover:bg-surface-hover"
                      }`}
                    >
                      {item.label}
                      {gold && <GoldSparkle className="h-3 w-3 text-amber-500 dark:text-amber-300" />}
                    </Link>
                  </li>
                );
              }
              if (item.kind === "menu") {
                return (
                  <NavMenuDropdown
                    key={item.basePath}
                    label={item.label}
                    active={item.activePaths.some((p) => isActive(p))}
                    items={item.items}
                  />
                );
              }
              return (
                <NavDropdown
                  key={item.basePath}
                  label={item.label}
                  active={isActive(item.basePath)}
                  buildHref={item.build}
                  showDot={DROPDOWN_NEW_SIGNAL.has(item.basePath) && newCount > 0}
                  hasNewByCert={
                    DROPDOWN_NEW_SIGNAL.has(item.basePath) ? hasNewByCert : undefined
                  }
                />
              );
            })}
          </ul>

          <div className="ml-3 flex shrink-0 items-center gap-1.5">
            <button
              onClick={() => setFeedbackOpen(true)}
              className="flex h-9 shrink-0 items-center gap-1.5 rounded-md px-2.5 text-xs font-medium text-text-muted transition-colors hover:bg-primary/10 hover:text-primary xl:px-3"
              aria-label="피드백 보내기"
              title="건의/오류 제보"
            >
              <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <span className="hidden xl:inline">피드백</span>
            </button>
            {loggedIn && <NotificationBell />}
            <button
              onClick={toggleTheme}
              className="flex h-9 w-9 items-center justify-center rounded-md text-text-muted transition-colors hover:bg-surface-hover hover:text-text"
              aria-label="테마 전환"
            >
              {theme === "dark" ? (
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              ) : (
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                </svg>
              )}
            </button>
            {loggedIn ? (
              <>
                <Link
                  href="/profile"
                  className="group inline-flex h-9 shrink-0 items-center gap-1.5 rounded-md border border-border bg-surface px-3 text-sm text-text-muted transition-colors hover:border-border-strong hover:bg-surface-hover hover:text-text"
                  title={`${nickname ?? ""} — 닉네임 변경 / 프로필`}
                >
                  <span className="block max-w-[120px] truncate">{nickname}</span>
                  <svg
                    className="h-3.5 w-3.5 shrink-0 opacity-60 transition-opacity group-hover:opacity-100"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={2}
                    aria-hidden="true"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </Link>
                <button
                  onClick={handleLogout}
                  className="flex h-9 shrink-0 items-center rounded-md px-3 text-sm font-medium text-text-muted transition-colors hover:bg-surface-hover hover:text-text"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <button
                onClick={handleLogin}
                className="inline-flex h-9 items-center gap-2 rounded-lg border border-border bg-surface px-3.5 text-sm font-medium text-text transition-all hover:border-primary/40 hover:bg-surface-hover"
              >
                <svg className="h-4 w-4" viewBox="0 0 24 24">
                  <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
                  <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                  <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                  <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                </svg>
                로그인
              </button>
            )}
          </div>
        </div>

        {/* Mobile / Tablet controls — lg 미만에서 모두 햄버거 */}
        <div className="flex items-center gap-1 lg:hidden">
          <button
            onClick={toggleTheme}
            className="flex h-9 w-9 items-center justify-center rounded-md text-text-muted transition-colors hover:bg-surface-hover hover:text-text"
            aria-label="테마 전환"
          >
            {theme === "dark" ? (
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            ) : (
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
              </svg>
            )}
          </button>
          <button
            className="flex h-9 w-9 items-center justify-center rounded-md text-text-muted hover:bg-surface-hover hover:text-text"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="메뉴"
            aria-expanded={menuOpen}
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              {menuOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>
      </nav>

      <FeedbackModal open={feedbackOpen} onClose={() => setFeedbackOpen(false)} />

      {/* Mobile / Tablet menu */}
      <div
        className={`border-t border-border transition-all duration-300 ease-out lg:hidden ${
          menuOpen
            ? "max-h-[80vh] overflow-y-auto overscroll-contain opacity-100"
            : "max-h-0 overflow-hidden opacity-0"
        }`}
      >
        <div className="px-4 pb-4 pt-2">
          <ul className="space-y-0.5">
            {NAV_LINKS.map((item) => {
              if (item.kind === "link") {
                const gold = item.accent === "gold";
                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      onClick={() => setMenuOpen(false)}
                      className={`flex items-center gap-1.5 rounded-md px-3 py-2.5 text-sm font-medium transition-colors ${
                        gold
                          ? "text-amber-500 hover:bg-surface-hover dark:text-amber-300"
                          : isActive(item.href)
                            ? "bg-primary/10 text-primary"
                            : "text-text-muted hover:bg-surface-hover hover:text-text"
                      }`}
                    >
                      {item.label}
                      {gold && <GoldSparkle className="h-3 w-3 text-amber-500 dark:text-amber-300" />}
                    </Link>
                  </li>
                );
              }
              if (item.kind === "menu") {
                return (
                  <MobileMenuAccordion
                    key={item.basePath}
                    label={item.label}
                    isActive={item.activePaths.some((p) => isActive(p))}
                    isOpen={openMobileDropdown === item.basePath}
                    onToggle={() =>
                      setOpenMobileDropdown((prev) =>
                        prev === item.basePath ? null : item.basePath,
                      )
                    }
                    items={item.items}
                    onItemClick={() => setMenuOpen(false)}
                  />
                );
              }
              return (
                <MobileNavAccordion
                  key={item.basePath}
                  label={item.label}
                  isActive={isActive(item.basePath)}
                  isOpen={openMobileDropdown === item.basePath}
                  onToggle={() =>
                    setOpenMobileDropdown((prev) =>
                      prev === item.basePath ? null : item.basePath,
                    )
                  }
                  buildHref={item.build}
                  onItemClick={() => setMenuOpen(false)}
                  showDot={DROPDOWN_NEW_SIGNAL.has(item.basePath) && newCount > 0}
                  hasNewByCert={
                    DROPDOWN_NEW_SIGNAL.has(item.basePath) ? hasNewByCert : undefined
                  }
                />
              );
            })}
          </ul>
          <div className="mt-3 border-t border-border pt-3">
            <button
              onClick={() => { setFeedbackOpen(true); setMenuOpen(false); }}
              className="flex w-full items-center gap-2 rounded-md px-3 py-2.5 text-sm font-medium text-text-muted hover:bg-primary/10 hover:text-primary"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              피드백 보내기
            </button>
            {loggedIn ? (
              <div className="mt-1 flex items-center justify-between px-3 py-2">
                <Link
                  href="/profile"
                  onClick={() => setMenuOpen(false)}
                  className="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-2.5 py-1 text-sm text-text-muted transition-colors hover:border-primary/40 hover:text-text"
                  title="닉네임 변경 / 프로필"
                >
                  <span>{nickname}</span>
                  <svg
                    className="h-3.5 w-3.5 opacity-60"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={2}
                    aria-hidden="true"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </Link>
                <button
                  onClick={() => { handleLogout(); setMenuOpen(false); }}
                  className="text-sm text-text-muted hover:text-text"
                >
                  로그아웃
                </button>
              </div>
            ) : (
              <button
                onClick={() => { handleLogin(); setMenuOpen(false); }}
                className="mt-1 flex w-full items-center justify-center gap-2 rounded-lg border border-border bg-surface px-4 py-2.5 text-sm font-medium text-text transition-all hover:border-primary/40"
              >
                <svg className="h-4 w-4" viewBox="0 0 24 24">
                  <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
                  <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                  <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                  <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                </svg>
                Google로 로그인
              </button>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}

function NavDropdown({
  label,
  active,
  buildHref,
  showDot = false,
  hasNewByCert,
}: {
  label: string;
  active: boolean;
  buildHref: (cert: CertKey) => string;
  showDot?: boolean;
  hasNewByCert?: (cert: CertKey) => boolean;
}) {
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  function focusFirstItem() {
    requestAnimationFrame(() => {
      const firstLink = menuRef.current?.querySelector<HTMLAnchorElement>("a[href]");
      firstLink?.focus();
    });
  }

  function handleTriggerKeyDown(e: React.KeyboardEvent<HTMLButtonElement>) {
    if (e.key === "ArrowDown" || e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      setOpen(true);
      focusFirstItem();
      return;
    }
    if (e.key === "Escape") {
      setOpen(false);
      triggerRef.current?.focus();
    }
  }

  function handleMenuKeyDown(e: React.KeyboardEvent<HTMLDivElement>) {
    if (e.key === "Escape") {
      e.preventDefault();
      setOpen(false);
      triggerRef.current?.focus();
    }
  }

  return (
    <li
      className="relative"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node | null)) {
          setOpen(false);
        }
      }}
    >
      <button
        ref={triggerRef}
        onClick={() => setOpen((v) => !v)}
        onFocus={() => setOpen(true)}
        onKeyDown={handleTriggerKeyDown}
        className={`inline-flex h-9 items-center gap-1 whitespace-nowrap rounded-md px-3 text-sm font-medium transition-colors ${
          active
            ? "bg-primary/10 text-primary"
            : "text-text-muted hover:text-text hover:bg-surface-hover"
        }`}
        aria-expanded={open}
        aria-haspopup="menu"
      >
        {label}
        {showDot && (
          <span
            className="ml-1 h-1.5 w-1.5 rounded-full bg-emerald-500"
            aria-label="새로운 모의고사 있음"
          />
        )}
        <svg
          className={`h-3 w-3 transition-transform ${open ? "rotate-180" : ""}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2.5}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {open && (
        <div
          ref={menuRef}
          className="absolute left-0 top-full z-50 min-w-[260px] pt-2"
          onMouseEnter={() => setOpen(true)}
          onMouseLeave={() => setOpen(false)}
          onKeyDown={handleMenuKeyDown}
          role="menu"
        >
          <div className="animate-[dropdown-enter_0.15s_ease-out] overflow-hidden rounded-xl border border-border bg-bg-elevated shadow-[var(--shadow-lg)]">
            {CERT_LIST.map((cert, idx) => {
              const certHasNew = hasNewByCert?.(cert.key) ?? false;
              return (
                <Link
                  key={cert.key}
                  href={buildHref(cert.key)}
                  onClick={() => setOpen(false)}
                  role="menuitem"
                  className={`flex items-center gap-3 px-4 py-2.5 transition-colors hover:bg-surface-hover ${
                    idx !== 0 ? "border-t border-border/50" : ""
                  }`}
                >
                  <span className={`h-2 w-2 shrink-0 rounded-full ${cert.tailwind.dot}`} />
                  <div className="min-w-0 flex-1">
                    <p className="flex items-center gap-1.5 text-sm font-semibold text-text">
                      {cert.label}
                      {certHasNew && (
                        <span className="text-[10px] font-semibold text-emerald-600 dark:text-emerald-400">
                          NEW
                        </span>
                      )}
                    </p>
                    <p className="mt-0.5 text-xs text-text-muted">{cert.labelLong}</p>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      )}
    </li>
  );
}

function NavMenuDropdown({
  label,
  active,
  items,
}: {
  label: string;
  active: boolean;
  items: MenuItem[];
}) {
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  function focusFirstItem() {
    requestAnimationFrame(() => {
      const firstLink = menuRef.current?.querySelector<HTMLAnchorElement>("a[href]");
      firstLink?.focus();
    });
  }

  function handleTriggerKeyDown(e: React.KeyboardEvent<HTMLButtonElement>) {
    if (e.key === "ArrowDown" || e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      setOpen(true);
      focusFirstItem();
      return;
    }
    if (e.key === "Escape") {
      setOpen(false);
      triggerRef.current?.focus();
    }
  }

  function handleMenuKeyDown(e: React.KeyboardEvent<HTMLDivElement>) {
    if (e.key === "Escape") {
      e.preventDefault();
      setOpen(false);
      triggerRef.current?.focus();
    }
  }

  return (
    <li
      className="relative"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node | null)) {
          setOpen(false);
        }
      }}
    >
      <button
        ref={triggerRef}
        onClick={() => setOpen((v) => !v)}
        onFocus={() => setOpen(true)}
        onKeyDown={handleTriggerKeyDown}
        className={`inline-flex h-9 items-center gap-1 whitespace-nowrap rounded-md px-3 text-sm font-medium transition-colors ${
          active
            ? "bg-primary/10 text-primary"
            : "text-text-muted hover:text-text hover:bg-surface-hover"
        }`}
        aria-expanded={open}
        aria-haspopup="menu"
      >
        {label}
        <svg
          className={`h-3 w-3 transition-transform ${open ? "rotate-180" : ""}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2.5}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {open && (
        <div
          ref={menuRef}
          className="absolute left-0 top-full z-50 min-w-[260px] pt-2"
          onMouseEnter={() => setOpen(true)}
          onMouseLeave={() => setOpen(false)}
          onKeyDown={handleMenuKeyDown}
          role="menu"
        >
          <div className="animate-[dropdown-enter_0.15s_ease-out] overflow-hidden rounded-xl border border-border bg-bg-elevated shadow-[var(--shadow-lg)]">
            {items.map((it, idx) => (
              <Link
                key={it.href}
                href={it.href}
                onClick={() => setOpen(false)}
                role="menuitem"
                className={`block px-4 py-2.5 transition-colors hover:bg-surface-hover ${
                  idx !== 0 ? "border-t border-border/50" : ""
                }`}
              >
                <p className="text-sm font-semibold text-text">{it.label}</p>
                {it.description && (
                  <p className="mt-0.5 text-xs text-text-muted">{it.description}</p>
                )}
              </Link>
            ))}
          </div>
        </div>
      )}
    </li>
  );
}

function MobileMenuAccordion({
  label,
  isActive,
  isOpen,
  onToggle,
  items,
  onItemClick,
}: {
  label: string;
  isActive: boolean;
  isOpen: boolean;
  onToggle: () => void;
  items: MenuItem[];
  onItemClick: () => void;
}) {
  return (
    <li>
      <button
        onClick={onToggle}
        aria-expanded={isOpen}
        className={`flex w-full items-center justify-between rounded-md px-3 py-2.5 text-sm font-medium transition-colors ${
          isActive
            ? "bg-primary/10 text-primary"
            : "text-text-muted hover:bg-surface-hover hover:text-text"
        }`}
      >
        <span>{label}</span>
        <svg
          className={`h-3.5 w-3.5 transition-transform ${isOpen ? "rotate-180" : ""}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2.5}
          aria-hidden="true"
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      <div
        className={`grid transition-all duration-200 ease-out ${
          isOpen ? "mt-0.5 grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
        }`}
      >
        <div className="overflow-hidden">
          <div className="ml-2 border-l border-border/60 pl-2">
            {items.map((it) => (
              <Link
                key={it.href}
                href={it.href}
                onClick={onItemClick}
                className="block rounded-md px-3 py-2.5 text-sm text-text-muted hover:bg-surface-hover hover:text-text"
              >
                <span className="font-medium text-text">{it.label}</span>
                {it.description && (
                  <span className="ml-2 text-xs text-text-subtle">{it.description}</span>
                )}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </li>
  );
}

function MobileNavAccordion({
  label,
  isActive,
  isOpen,
  onToggle,
  buildHref,
  onItemClick,
  showDot = false,
  hasNewByCert,
}: {
  label: string;
  isActive: boolean;
  isOpen: boolean;
  onToggle: () => void;
  buildHref: (cert: CertKey) => string;
  onItemClick: () => void;
  showDot?: boolean;
  hasNewByCert?: (cert: CertKey) => boolean;
}) {
  return (
    <li>
      <button
        onClick={onToggle}
        aria-expanded={isOpen}
        className={`flex w-full items-center justify-between rounded-md px-3 py-2.5 text-sm font-medium transition-colors ${
          isActive
            ? "bg-primary/10 text-primary"
            : "text-text-muted hover:bg-surface-hover hover:text-text"
        }`}
      >
        <span className="flex items-center gap-1.5">
          {label}
          {showDot && (
            <span
              className="h-1.5 w-1.5 rounded-full bg-emerald-500"
              aria-label="새로운 모의고사 있음"
            />
          )}
        </span>
        <svg
          className={`h-3.5 w-3.5 transition-transform ${isOpen ? "rotate-180" : ""}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2.5}
          aria-hidden="true"
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      <div
        className={`grid transition-all duration-200 ease-out ${
          isOpen ? "mt-0.5 grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
        }`}
      >
        <div className="overflow-hidden">
          <div className="ml-2 border-l border-border/60 pl-2">
            {CERT_LIST.map((cert) => {
              const certHasNew = hasNewByCert?.(cert.key) ?? false;
              return (
                <Link
                  key={cert.key}
                  href={buildHref(cert.key)}
                  onClick={onItemClick}
                  className="flex items-center gap-2.5 rounded-md px-3 py-2.5 text-sm text-text-muted hover:bg-surface-hover hover:text-text"
                >
                  <span className={`h-1.5 w-1.5 rounded-full ${cert.tailwind.dot}`} />
                  <span>{cert.label}</span>
                  {certHasNew && (
                    <span className="ml-auto text-[10px] font-semibold text-emerald-600 dark:text-emerald-400">
                      NEW
                    </span>
                  )}
                </Link>
              );
            })}
          </div>
        </div>
      </div>
    </li>
  );
}
