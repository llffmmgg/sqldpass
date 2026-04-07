"use client";

import { useEffect, useMemo, useState } from "react";
import {
  getAdminMockExams,
  createMockExam,
  deleteMockExam,
  type AdminMockExam,
  type CreateMockExamType,
} from "@/lib/adminApi";

/**
 * 자격증 레지스트리 — 새 자격증 추가 시 이 배열에만 항목을 추가하면
 * 탭/생성 버튼/배지가 자동 확장됨.
 */
type CertConfig = {
  id: CreateMockExamType;
  label: string;
  shortLabel: string;
  totalQuestions: number;
  accent: {
    badge: string; // 배지 배경 + 텍스트
    button: string; // 생성 버튼
    tabActive: string; // 활성 탭 underline
  };
};

const CERTS: CertConfig[] = [
  {
    id: "SQLD",
    label: "SQLD",
    shortLabel: "SQLD",
    totalQuestions: 50,
    accent: {
      badge: "border-amber-500/30 bg-amber-500/10 text-amber-300",
      button:
        "bg-primary text-zinc-900 hover:bg-primary-hover disabled:opacity-50",
      tabActive: "border-amber-400 text-amber-300",
    },
  },
  {
    id: "ENGINEER_PRACTICAL",
    label: "정보처리기사 실기",
    shortLabel: "정처기 실기",
    totalQuestions: 20,
    accent: {
      badge: "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
      button:
        "border border-emerald-500/40 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20 disabled:opacity-50",
      tabActive: "border-emerald-400 text-emerald-300",
    },
  },
  // + 새 자격증 추가 시 여기에 항목 추가
];

type TabId = "all" | CreateMockExamType;

export default function AdminMockExamsPage() {
  const [exams, setExams] = useState<AdminMockExam[] | null>(null);
  const [creating, setCreating] = useState<CreateMockExamType | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("all");

  async function load(): Promise<AdminMockExam[] | null> {
    try {
      const data = await getAdminMockExams();
      setExams(data);
      return data;
    } catch (e) {
      setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다.");
      return null;
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCreate(examType: CreateMockExamType) {
    setCreating(examType);
    setError(null);
    setInfo(null);

    // 생성 직전 목록의 최대 sequence 기록 (오탐 복구 판정용)
    const prevMaxSeq =
      (exams ?? [])
        .filter((e) => e.examType === examType)
        .reduce((max, e) => Math.max(max, e.sequence), 0);

    try {
      await createMockExam(examType);
      await load();
    } catch (e) {
      const originalMessage =
        e instanceof Error ? e.message : "생성에 실패했습니다.";

      // 네트워크 타임아웃이어도 백엔드가 실제로 저장했을 가능성 → 목록 재조회로 확인
      const refreshed = await load();
      if (refreshed) {
        const recent = refreshed
          .filter((e) => e.examType === examType)
          .find(
            (e) =>
              e.sequence > prevMaxSeq &&
              Date.now() - new Date(e.createdAt).getTime() < 180_000,
          );
        if (recent) {
          // 실제로는 생성 성공한 케이스
          setInfo(
            `네트워크 응답이 지연되었지만 모의고사는 정상 생성되었습니다 (${recent.name}).`,
          );
          return;
        }
      }
      // 진짜 실패
      setError(
        `${originalMessage} — 응답이 지연된 경우 잠시 후 목록을 새로고침해 확인해주세요.`,
      );
    } finally {
      setCreating(null);
    }
  }

  async function handleDelete(id: number, name: string) {
    if (!confirm(`${name}을(를) 삭제하시겠습니까?`)) return;
    try {
      await deleteMockExam(id);
      await load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "삭제 실패");
    }
  }

  // 탭별 카운트 + 필터링
  const countsByCert = useMemo(() => {
    const map = new Map<CreateMockExamType, number>();
    (exams ?? []).forEach((e) => {
      map.set(e.examType, (map.get(e.examType) ?? 0) + 1);
    });
    return map;
  }, [exams]);

  const filteredExams = useMemo(() => {
    if (!exams) return null;
    if (activeTab === "all") return exams;
    return exams.filter((e) => e.examType === activeTab);
  }, [exams, activeTab]);

  // 현재 탭에서 노출할 생성 버튼 목록
  const createButtons: CertConfig[] =
    activeTab === "all" ? CERTS : CERTS.filter((c) => c.id === activeTab);

  function certOf(id: CreateMockExamType) {
    return CERTS.find((c) => c.id === id);
  }

  return (
    <div>
      {/* Header */}
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">모의고사 관리</h1>
        <p className="text-sm text-muted">
          자격증별로 모의고사를 생성/삭제합니다. 생성 시 해당 자격증의 분포
          템플릿에 따라 문제가 자동 편성됩니다.
        </p>
      </div>

      {/* Tabs */}
      <div className="mt-6 border-b border-border">
        <nav className="flex gap-1 overflow-x-auto" role="tablist">
          <TabButton
            label="전체"
            count={exams?.length ?? 0}
            active={activeTab === "all"}
            onClick={() => setActiveTab("all")}
            activeClass="border-foreground text-foreground"
          />
          {CERTS.map((cert) => (
            <TabButton
              key={cert.id}
              label={cert.shortLabel}
              count={countsByCert.get(cert.id) ?? 0}
              active={activeTab === cert.id}
              onClick={() => setActiveTab(cert.id)}
              activeClass={cert.accent.tabActive}
            />
          ))}
        </nav>
      </div>

      {/* Create buttons + error */}
      <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="text-xs text-muted">
          {activeTab === "all"
            ? "원하는 자격증 유형으로 모의고사를 생성하세요."
            : `${certOf(activeTab)?.label} 모의고사는 ${
                certOf(activeTab)?.totalQuestions
              }문항으로 구성됩니다.`}
        </div>
        <div className="flex flex-wrap gap-2">
          {createButtons.map((cert) => (
            <button
              key={cert.id}
              onClick={() => handleCreate(cert.id)}
              disabled={creating !== null}
              className={`rounded-lg px-4 py-2 text-sm font-semibold transition-colors ${cert.accent.button}`}
            >
              {creating === cert.id
                ? "생성 중..."
                : `+ ${cert.shortLabel} 모의고사`}
            </button>
          ))}
        </div>
      </div>

      {info && (
        <div className="mt-4 rounded-lg border border-emerald-500/30 bg-emerald-500/5 p-3 text-sm text-emerald-300">
          {info}
        </div>
      )}

      {error && (
        <div className="mt-4 rounded-lg border border-red-500/30 bg-red-500/5 p-3 text-sm text-red-400">
          {error}
        </div>
      )}

      {/* Table */}
      <div className="mt-6">
        {filteredExams === null ? (
          <p className="text-muted">로딩 중...</p>
        ) : filteredExams.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border bg-surface/30 px-4 py-10 text-center">
            <p className="text-sm text-muted">
              {activeTab === "all"
                ? "생성된 모의고사가 없습니다."
                : `${certOf(activeTab)?.label} 모의고사가 없습니다.`}
            </p>
            <p className="mt-1 text-xs text-muted/60">
              위쪽 버튼으로 새 모의고사를 생성해 보세요.
            </p>
          </div>
        ) : (
          <div className="overflow-hidden rounded-xl border border-border">
            <table className="w-full text-sm">
              <thead className="bg-surface text-left text-muted">
                <tr>
                  <th className="px-4 py-3 w-12">#</th>
                  <th className="px-4 py-3 w-36">자격증</th>
                  <th className="px-4 py-3">이름</th>
                  <th className="px-4 py-3 w-20">문항</th>
                  <th className="px-4 py-3 w-24">난이도</th>
                  <th className="px-4 py-3 w-32">생성일</th>
                  <th className="px-4 py-3 w-20 text-right">관리</th>
                </tr>
              </thead>
              <tbody>
                {filteredExams.map((exam) => {
                  const cert = certOf(exam.examType);
                  return (
                    <tr
                      key={exam.id}
                      className="border-t border-border hover:bg-surface/50"
                    >
                      <td className="px-4 py-3 text-muted">{exam.sequence}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center rounded-md border px-2 py-0.5 text-[11px] font-medium ${
                            cert?.accent.badge ??
                            "border-border bg-surface text-muted"
                          }`}
                        >
                          {cert?.shortLabel ?? exam.examType}
                        </span>
                      </td>
                      <td className="px-4 py-3 font-medium">{exam.name}</td>
                      <td className="px-4 py-3">{exam.totalQuestions}</td>
                      <td className="px-4 py-3">
                        {exam.difficultyLabel ? (
                          <span
                            className={`inline-flex items-center rounded-md border px-2 py-0.5 text-[11px] font-medium ${difficultyTdClass(exam.difficultyLabel)}`}
                          >
                            {exam.difficultyLabel}
                          </span>
                        ) : (
                          <span className="text-xs text-muted/60">-</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {new Date(exam.createdAt).toLocaleDateString("ko-KR")}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => handleDelete(exam.id, exam.name)}
                          className="text-xs text-red-400 hover:text-red-300"
                        >
                          삭제
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

// === 하위 컴포넌트 ===

function difficultyTdClass(label: NonNullable<AdminMockExam["difficultyLabel"]>): string {
  switch (label) {
    case "쉬움":
      return "border-emerald-500/40 bg-emerald-500/10 text-emerald-300";
    case "보통":
      return "border-amber-500/40 bg-amber-500/10 text-amber-300";
    case "어려움":
      return "border-red-500/40 bg-red-500/10 text-red-300";
    case "혼합":
      return "border-violet-500/40 bg-violet-500/10 text-violet-300";
  }
}

function TabButton({
  label,
  count,
  active,
  onClick,
  activeClass,
}: {
  label: string;
  count: number;
  active: boolean;
  onClick: () => void;
  activeClass: string;
}) {
  return (
    <button
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className={`relative whitespace-nowrap border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
        active
          ? activeClass
          : "border-transparent text-muted hover:text-foreground"
      }`}
    >
      {label}
      <span
        className={`ml-1.5 inline-flex min-w-[1.25rem] items-center justify-center rounded-full px-1.5 text-[10px] ${
          active ? "bg-foreground/10" : "bg-surface text-muted"
        }`}
      >
        {count}
      </span>
    </button>
  );
}
