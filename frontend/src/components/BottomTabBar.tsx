"use client";

/* eslint-disable react-hooks/set-state-in-effect -- localStorage 의 마지막 cert 를 마운트 시점에 한 번 읽어 학습 탭 href 에 반영하는 것은 정당한 동기화 effect */

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { certFromSlug, type CertKey } from "@/lib/cert-tokens";

const LAST_CERT_KEY = "sqldpass:lastCert";
const DEFAULT_CERT_KEY: CertKey = "SQLD";

type Tab = {
  href: string;
  label: string;
  match: (pathname: string) => boolean;
  Icon: (props: { active: boolean }) => React.JSX.Element;
};

function LearnIcon({ active }: { active: boolean }) {
  return (
    <svg className="h-5 w-5" fill={active ? "currentColor" : "none"} viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
    </svg>
  );
}

function MiniIcon({ active }: { active: boolean }) {
  // 빠른 5–10분 미니 한 세트를 상징하는 번개+격자 아이콘.
  return (
    <svg className="h-5 w-5" fill={active ? "currentColor" : "none"} viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M13 3 L4 14 H11 L10 21 L20 9 H13 Z" />
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

function MyLearningIcon({ active }: { active: boolean }) {
  // 학습 진행률을 상징하는 차트 + 책 아이콘 — 대시보드+오답노트 통합 의미.
  return (
    <svg className="h-5 w-5" fill={active ? "currentColor" : "none"} viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 19V6a2 2 0 012-2h6a2 2 0 012 2v13m-10 0h10m-10 0H5a2 2 0 01-2-2v-5a2 2 0 012-2h4v9z" />
    </svg>
  );
}

// 홈 / 마이 탭은 제거. 홈은 좌상단 로고로, 마이는 "내학습" 으로 통합.
const TABS: Tab[] = [
  {
    // href는 런타임에 마지막 본 cert로 덮어씀 (BottomTabBar 본체에서 처리)
    href: `/solve?cert=${DEFAULT_CERT_KEY}`,
    label: "문제",
    match: (p) => p.startsWith("/solve"),
    Icon: LearnIcon,
  },
  {
    href: "/mini-mock-exams",
    label: "미니",
    match: (p) => p.startsWith("/mini-mock-exams"),
    Icon: MiniIcon,
  },
  {
    href: "/mock-exams",
    label: "모의고사",
    match: (p) => p.startsWith("/mock-exams") || p.startsWith("/cbt-mock-exam"),
    Icon: ExamIcon,
  },
  {
    href: "/past-exams",
    label: "기출",
    match: (p) => p.startsWith("/past-exams"),
    Icon: PastExamIcon,
  },
  {
    href: "/my-learning",
    label: "내학습",
    match: (p) =>
      p.startsWith("/my-learning") ||
      p.startsWith("/dashboard") ||
      p.startsWith("/profile") ||
      p.startsWith("/mypage") ||
      p.startsWith("/wrong-answers") ||
      p.startsWith("/history"),
    Icon: MyLearningIcon,
  },
];

// 시험/풀이 진행 중인 페이지에서는 오탭 방지를 위해 숨김
// /solve 는 학습 탭 destination 이므로 숨기지 않는다.
const HIDE_PATTERNS: RegExp[] = [
  /^\/admin(\/|$)/,
  /^\/auth(\/|$)/,
  /^\/mock-exams\/\d+/,
  /^\/cbt-mock-exam(\/|$)/,
  /^\/past-exams\/\d+/,
];

export default function BottomTabBar() {
  const pathname = usePathname() ?? "/";
  const [learnHref, setLearnHref] = useState(`/solve?cert=${DEFAULT_CERT_KEY}`);

  useEffect(() => {
    try {
      const last = window.localStorage.getItem(LAST_CERT_KEY);
      const certKey = last ? certFromSlug(last) : null;
      if (certKey) setLearnHref(`/solve?cert=${certKey}`);
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
          const href = tab.label === "문제" ? learnHref : tab.href;
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
                prefetch={false}
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
