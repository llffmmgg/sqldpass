"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { isLoggedIn, getNickname, clearAuth } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";
import { type Theme, getInitialTheme, setStoredTheme, applyTheme } from "@/lib/theme";
import FeedbackModal from "@/components/FeedbackModal";
import NotificationBell from "@/components/NotificationBell";
import { CERT_LIST, type CertKey } from "@/lib/cert-tokens";

type NavItem =
  | { kind: "link"; href: string; label: string }
  | { kind: "dropdown"; label: string; basePath: string; build: (cert: CertKey) => string };

const NAV_LINKS: NavItem[] = [
  { kind: "link", href: "/", label: "홈" },
  { kind: "dropdown", label: "문제 풀기", basePath: "/solve", build: (cert) => `/solve?cert=${cert}` },
  { kind: "dropdown", label: "모의고사", basePath: "/mock-exams", build: (cert) => `/mock-exams?cert=${cert}` },
  { kind: "link", href: "/dashboard", label: "대시보드" },
  { kind: "link", href: "/wrong-answers", label: "오답 노트" },
  { kind: "link", href: "/blog", label: "시험 준비 팁" },
];

export default function NavBar() {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);
  const [nickname, setNickname] = useState<string | null>(null);
  const [theme, setTheme] = useState<Theme>("dark");
  const [feedbackOpen, setFeedbackOpen] = useState(false);

  useEffect(() => {
    setLoggedIn(isLoggedIn());
    setNickname(getNickname());
    setTheme(getInitialTheme());
  }, [pathname]);

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
    <header className="sticky top-0 z-50 border-b border-border bg-bg/80 backdrop-blur-md">
      <nav className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link
          href="/"
          className="flex items-center gap-2 text-lg font-bold tracking-tight text-text font-mono"
        >
          <Image
            src="/logo/logo.webp"
            alt="sqldpass"
            width={80}
            height={80}
            className="h-8 w-8 rounded-md"
            priority
          />
          sqld<span className="text-primary">pass</span>
        </Link>

        {/* Desktop */}
        <div className="hidden items-center gap-1 sm:flex">
          <ul className="flex items-center gap-0.5">
            {NAV_LINKS.map((item) => {
              if (item.kind === "link") {
                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      className={`flex h-9 items-center rounded-md px-3 text-sm font-medium transition-colors ${
                        isActive(item.href)
                          ? "bg-primary/10 text-primary"
                          : "text-text-muted hover:text-text hover:bg-surface-hover"
                      }`}
                    >
                      {item.label}
                    </Link>
                  </li>
                );
              }
              return (
                <NavDropdown
                  key={item.basePath}
                  label={item.label}
                  active={isActive(item.basePath)}
                  buildHref={item.build}
                />
              );
            })}
          </ul>

          <div className="ml-4 flex items-center gap-1.5">
            <button
              onClick={() => setFeedbackOpen(true)}
              className="flex h-9 items-center gap-1.5 rounded-md px-3 text-xs font-medium text-text-muted transition-colors hover:bg-primary/10 hover:text-primary"
              aria-label="피드백 보내기"
              title="건의/오류 제보"
            >
              <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              피드백
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
                  className="group inline-flex h-9 items-center gap-1.5 rounded-md border border-border bg-surface px-3 text-sm text-text-muted transition-colors hover:border-border-strong hover:bg-surface-hover hover:text-text"
                  title="닉네임 변경 / 프로필"
                >
                  <span>{nickname}</span>
                  <svg
                    className="h-3.5 w-3.5 opacity-60 transition-opacity group-hover:opacity-100"
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
                  className="flex h-9 items-center rounded-md px-3 text-sm font-medium text-text-muted transition-colors hover:bg-surface-hover hover:text-text"
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

        {/* Mobile controls */}
        <div className="flex items-center gap-1 sm:hidden">
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

      {/* Mobile menu */}
      <div
        className={`overflow-hidden border-t border-border transition-all duration-300 ease-out sm:hidden ${
          menuOpen ? "max-h-[80vh] opacity-100" : "max-h-0 opacity-0"
        }`}
      >
        <div className="px-4 pb-4 pt-2">
          <ul className="space-y-0.5">
            {NAV_LINKS.map((item) => {
              if (item.kind === "link") {
                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      onClick={() => setMenuOpen(false)}
                      className={`block rounded-md px-3 py-2.5 text-sm font-medium transition-colors ${
                        isActive(item.href)
                          ? "bg-primary/10 text-primary"
                          : "text-text-muted hover:bg-surface-hover hover:text-text"
                      }`}
                    >
                      {item.label}
                    </Link>
                  </li>
                );
              }
              return (
                <li key={item.basePath}>
                  <div className="mt-2 px-3 py-1 text-[11px] font-semibold uppercase tracking-wider text-text-subtle">
                    {item.label}
                  </div>
                  {CERT_LIST.map((cert) => (
                    <Link
                      key={cert.key}
                      href={item.build(cert.key)}
                      onClick={() => setMenuOpen(false)}
                      className="flex items-center gap-2.5 rounded-md px-3 py-2.5 text-sm text-text-muted hover:bg-surface-hover hover:text-text"
                    >
                      <span className={`h-1.5 w-1.5 rounded-full ${cert.tailwind.dot}`} />
                      {cert.label}
                    </Link>
                  ))}
                </li>
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
}: {
  label: string;
  active: boolean;
  buildHref: (cert: CertKey) => string;
}) {
  const [open, setOpen] = useState(false);
  return (
    <li
      className="relative"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <button
        onClick={() => setOpen((v) => !v)}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        className={`inline-flex h-9 items-center gap-1 rounded-md px-3 text-sm font-medium transition-colors ${
          active
            ? "bg-primary/10 text-primary"
            : "text-text-muted hover:text-text hover:bg-surface-hover"
        }`}
        aria-expanded={open}
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
          className="absolute left-0 top-full z-50 min-w-[260px] pt-2"
          onMouseEnter={() => setOpen(true)}
          onMouseLeave={() => setOpen(false)}
        >
          <div className="animate-[dropdown-enter_0.15s_ease-out] overflow-hidden rounded-xl border border-border bg-bg-elevated shadow-[var(--shadow-lg)]">
            {CERT_LIST.map((cert, idx) => (
              <Link
                key={cert.key}
                href={buildHref(cert.key)}
                onClick={() => setOpen(false)}
                className={`flex items-center gap-3 px-4 py-2.5 transition-colors hover:bg-surface-hover ${
                  idx !== 0 ? "border-t border-border/50" : ""
                }`}
              >
                <span className={`h-2 w-2 shrink-0 rounded-full ${cert.tailwind.dot}`} />
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-text">{cert.label}</p>
                  <p className="mt-0.5 text-xs text-text-muted">{cert.labelLong}</p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </li>
  );
}
