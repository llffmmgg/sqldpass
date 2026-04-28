"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";

const LAST_CERT_KEY = "sqldpass:lastCert";
const DEFAULT_CERT_SLUG = "sqld";

type Tab = {
  href: string;
  label: string;
  match: (pathname: string) => boolean;
  Icon: (props: { active: boolean }) => React.JSX.Element;
};

function HomeIcon({ active }: { active: boolean }) {
  return (
    <svg className="h-5 w-5" fill={active ? "currentColor" : "none"} viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
    </svg>
  );
}

function LearnIcon({ active }: { active: boolean }) {
  return (
    <svg className="h-5 w-5" fill={active ? "currentColor" : "none"} viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
    </svg>
  );
}

function ExamIcon({ active }: { active: boolean }) {
  return (
    <svg className="h-5 w-5" fill={active ? "currentColor" : "none"} viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
    </svg>
  );
}

function PastExamIcon({ active }: { active: boolean }) {
  return (
    <svg className="h-5 w-5" fill={active ? "currentColor" : "none"} viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  );
}

function MyIcon({ active }: { active: boolean }) {
  return (
    <svg className="h-5 w-5" fill={active ? "currentColor" : "none"} viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
    </svg>
  );
}

const TABS: Tab[] = [
  {
    href: "/",
    label: "홈",
    match: (p) => p === "/",
    Icon: HomeIcon,
  },
  {
    // href는 런타임에 마지막 본 cert로 덮어씀 (BottomTabBar 본체에서 처리)
    href: `/learn/${DEFAULT_CERT_SLUG}`,
    label: "학습",
    match: (p) => p.startsWith("/learn"),
    Icon: LearnIcon,
  },
  {
    href: "/mock-exams",
    label: "모의고사",
    match: (p) => p.startsWith("/mock-exams") || p.startsWith("/cbt-mock-exam"),
    Icon: ExamIcon,
  },
  {
    href: "/past-exams",
    label: "기출복원",
    match: (p) => p.startsWith("/past-exams"),
    Icon: PastExamIcon,
  },
  {
    href: "/dashboard",
    label: "마이",
    match: (p) =>
      p.startsWith("/dashboard") ||
      p.startsWith("/profile") ||
      p.startsWith("/mypage") ||
      p.startsWith("/wrong-answers"),
    Icon: MyIcon,
  },
];

// 시험/풀이 진행 중인 페이지에서는 오탭 방지를 위해 숨김
const HIDE_PATTERNS: RegExp[] = [
  /^\/admin(\/|$)/,
  /^\/auth(\/|$)/,
  /^\/solve\/?$/,
  /^\/mock-exams\/\d+/,
  /^\/cbt-mock-exam(\/|$)/,
  /^\/past-exams\/\d+/,
];

export default function BottomTabBar() {
  const pathname = usePathname() ?? "/";
  const [learnHref, setLearnHref] = useState(`/learn/${DEFAULT_CERT_SLUG}`);

  useEffect(() => {
    try {
      const last = window.localStorage.getItem(LAST_CERT_KEY);
      if (last) setLearnHref(`/learn/${last}`);
    } catch {
      /* ignore */
    }
  }, []);

  if (HIDE_PATTERNS.some((re) => re.test(pathname))) return null;

  return (
    <nav
      className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-bg/95 backdrop-blur-md lg:hidden"
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
      aria-label="하단 메인 메뉴"
    >
      <ul className="mx-auto grid max-w-md grid-cols-5">
        {TABS.map((tab) => {
          const active = tab.match(pathname);
          const href = tab.label === "학습" ? learnHref : tab.href;
          return (
            <li key={tab.label}>
              <Link
                href={href}
                aria-current={active ? "page" : undefined}
                className={`flex flex-col items-center justify-center gap-0.5 py-2 text-[10px] font-medium transition-colors ${
                  active
                    ? "text-primary"
                    : "text-text-muted hover:text-text"
                }`}
              >
                <tab.Icon active={active} />
                <span>{tab.label}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
