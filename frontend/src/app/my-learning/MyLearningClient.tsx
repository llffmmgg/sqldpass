"use client";

/* eslint-disable react-hooks/set-state-in-effect -- localStorage 의 마지막 탭 동기화는 마운트 effect 안 setState 가 자연스러움 */

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";

import DashboardPage from "@/app/dashboard/page";
import WrongAnswersPage from "@/app/wrong-answers/page";
import Spinner from "@/components/Spinner";

type Tab = "dashboard" | "wrong-answers";

const STORAGE_KEY = "sqldpass:myLearningTab";

export default function MyLearningClient() {
  return (
    <Suspense
      fallback={
        <main className="flex min-h-[40vh] items-center justify-center">
          <Spinner message="불러오는 중..." />
        </main>
      }
    >
      <MyLearningContent />
    </Suspense>
  );
}

function MyLearningContent() {
  const searchParams = useSearchParams();
  // ?tab=wrong 또는 ?tab=dashboard 로 진입 가능. 그 외는 마지막 탭 또는 dashboard 기본.
  const queryTab = searchParams?.get("tab");
  const initialTab: Tab =
    queryTab === "wrong" ? "wrong-answers" : queryTab === "dashboard" ? "dashboard" : "dashboard";

  const [tab, setTab] = useState<Tab>(initialTab);

  // 마운트 시 localStorage 의 마지막 탭으로 복원 (query 우선).
  useEffect(() => {
    if (queryTab) return;
    try {
      const last = window.localStorage.getItem(STORAGE_KEY);
      if (last === "dashboard" || last === "wrong-answers") setTab(last);
    } catch {
      /* ignore */
    }
  }, [queryTab]);

  // 탭 변경 시 마지막 탭 저장.
  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, tab);
    } catch {
      /* ignore */
    }
  }, [tab]);

  return (
    <div className="min-h-screen bg-bg text-text">
      <TabBar tab={tab} onChange={setTab} />
      <div>
        {tab === "dashboard" ? <DashboardPage /> : <WrongAnswersPage />}
      </div>
    </div>
  );
}

function TabBar({ tab, onChange }: { tab: Tab; onChange: (t: Tab) => void }) {
  // sticky 로 두면 페이지 스크롤 시 탭이 따라옴. NavBar 가 sticky top-0 라 그 아래에 붙는다.
  return (
    <div className="sticky top-14 z-30 border-b border-border bg-bg/95 backdrop-blur-md">
      <div className="mx-auto flex max-w-5xl gap-1 px-4 py-2 sm:px-6 lg:px-8">
        <TabButton active={tab === "dashboard"} onClick={() => onChange("dashboard")}>
          대시보드
        </TabButton>
        <TabButton active={tab === "wrong-answers"} onClick={() => onChange("wrong-answers")}>
          오답노트
        </TabButton>
      </div>
    </div>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      aria-current={active ? "page" : undefined}
      className={`relative inline-flex items-center px-4 py-2 text-sm font-semibold transition-colors ${
        active
          ? "text-primary"
          : "text-text-muted hover:text-text"
      }`}
    >
      {children}
      {active && (
        <span className="absolute inset-x-3 -bottom-0.5 h-0.5 rounded-full bg-primary" />
      )}
    </button>
  );
}
