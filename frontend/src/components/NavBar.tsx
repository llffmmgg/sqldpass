"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { isLoggedIn, getNickname, clearAuth } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";

const NAV_LINKS = [
  { href: "/", label: "홈" },
  { href: "/solve", label: "문제 풀기" },
  { href: "/history", label: "풀이 기록" },
  { href: "/wrong-answers", label: "오답 노트" },
];

export default function NavBar() {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);
  const [nickname, setNickname] = useState<string | null>(null);

  useEffect(() => {
    setLoggedIn(isLoggedIn());
    setNickname(getNickname());
  }, [pathname]);

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

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background/80 backdrop-blur">
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3 sm:px-6">
        <Link href="/" className="text-lg font-bold tracking-tight text-foreground">
          SQLD <span className="text-primary">Pass</span>
        </Link>

        {/* Desktop */}
        <div className="hidden items-center gap-1 sm:flex">
          <ul className="flex gap-1">
            {NAV_LINKS.map((link) => (
              <li key={link.href}>
                <Link
                  href={link.href}
                  className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                    isActive(link.href)
                      ? "bg-primary/10 text-primary"
                      : "text-muted hover:text-foreground"
                  }`}
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>

          <div className="ml-4 flex items-center gap-2">
            {loggedIn ? (
              <>
                <span className="text-sm text-muted">{nickname}</span>
                <button
                  onClick={handleLogout}
                  className="rounded-md px-3 py-1.5 text-sm font-medium text-muted transition-colors hover:text-foreground"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <button
                onClick={handleLogin}
                className="rounded-lg bg-primary px-4 py-1.5 text-sm font-semibold text-zinc-900 transition-colors hover:bg-primary-hover"
              >
                로그인
              </button>
            )}
          </div>
        </div>

        {/* Mobile hamburger */}
        <button
          className="flex h-8 w-8 items-center justify-center rounded-md text-muted hover:text-foreground sm:hidden"
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="메뉴"
        >
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            {menuOpen ? (
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            ) : (
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
            )}
          </svg>
        </button>
      </nav>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="border-t border-border px-4 pb-3 sm:hidden">
          <ul>
            {NAV_LINKS.map((link) => (
              <li key={link.href}>
                <Link
                  href={link.href}
                  onClick={() => setMenuOpen(false)}
                  className={`block rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    isActive(link.href)
                      ? "bg-primary/10 text-primary"
                      : "text-muted hover:text-foreground"
                  }`}
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
          <div className="mt-2 border-t border-border pt-2">
            {loggedIn ? (
              <div className="flex items-center justify-between px-3 py-2">
                <span className="text-sm text-muted">{nickname}</span>
                <button
                  onClick={() => { handleLogout(); setMenuOpen(false); }}
                  className="text-sm text-muted hover:text-foreground"
                >
                  로그아웃
                </button>
              </div>
            ) : (
              <button
                onClick={() => { handleLogin(); setMenuOpen(false); }}
                className="block w-full rounded-lg bg-primary px-4 py-2 text-center text-sm font-semibold text-zinc-900 transition-colors hover:bg-primary-hover"
              >
                로그인
              </button>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
