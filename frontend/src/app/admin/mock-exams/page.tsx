"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  getAdminMockExams,
  createMockExam,
  deleteMockExam,
  changeMockExamVisibility,
  startMockExamPdfBackfill,
  getMockExamPdfBackfillStatus,
  type AdminMockExam,
  type CreateMockExamType,
  type MockExamCreationDifficulty,
  type MockExamVisibility,
  type PdfBackfillStatus,
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
  {
    id: "COMPUTER_LITERACY_1",
    label: "컴퓨터활용능력 1급 필기",
    shortLabel: "컴활 1급",
    totalQuestions: 60,
    accent: {
      badge: "border-sky-500/30 bg-sky-500/10 text-sky-300",
      button:
        "border border-sky-500/40 bg-sky-500/10 text-sky-300 hover:bg-sky-500/20 disabled:opacity-50",
      tabActive: "border-sky-400 text-sky-300",
    },
  },
  {
    id: "COMPUTER_LITERACY_2",
    label: "컴퓨터활용능력 2급 필기",
    shortLabel: "컴활 2급",
    totalQuestions: 40,
    accent: {
      badge: "border-indigo-500/30 bg-indigo-500/10 text-indigo-300",
      button:
        "border border-indigo-500/40 bg-indigo-500/10 text-indigo-300 hover:bg-indigo-500/20 disabled:opacity-50",
      tabActive: "border-indigo-400 text-indigo-300",
    },
  },
  {
    id: "ENGINEER_WRITTEN",
    label: "정보처리기사 필기",
    shortLabel: "정처기 필기",
    totalQuestions: 100,
    accent: {
      badge: "border-rose-500/30 bg-rose-500/10 text-rose-300",
      button:
        "border border-rose-500/40 bg-rose-500/10 text-rose-300 hover:bg-rose-500/20 disabled:opacity-50",
      tabActive: "border-rose-400 text-rose-300",
    },
  },
  {
    id: "ADSP",
    label: "데이터분석 준전문가(ADsP)",
    shortLabel: "ADsP",
    totalQuestions: 50,
    accent: {
      badge: "border-teal-500/30 bg-teal-500/10 text-teal-300",
      button:
        "border border-teal-500/40 bg-teal-500/10 text-teal-300 hover:bg-teal-500/20 disabled:opacity-50",
      tabActive: "border-teal-400 text-teal-300",
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
  // 정처기 생성 시 사용할 평균 난이도 (SQLD는 무시)
  const [engineerDifficulty, setEngineerDifficulty] = useState<MockExamCreationDifficulty>("NORMAL");
  // (템플릿 제거됨 — 고정 분포 사용)

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
      await createMockExam(examType, engineerDifficulty, null);
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

  async function handleVisibility(id: number, visibility: MockExamVisibility) {
    try {
      await changeMockExamVisibility(id, visibility);
      await load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "공개 상태 변경 실패");
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
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold">모의고사 관리</h1>
          <p className="text-sm text-muted">
            자격증별로 모의고사를 생성/삭제합니다. 생성 시 해당 자격증의 분포
            템플릿에 따라 문제가 자동 편성됩니다.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <PdfBackfillButton />
          <Link
            href="/admin/mock-exams/manual"
            className="rounded-lg border border-violet-500/40 bg-violet-500/10 px-3 py-2 text-xs font-semibold text-violet-300 hover:bg-violet-500/20"
          >
            + JSON 으로 직접 등록
          </Link>
        </div>
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

      {/* 평균 난이도 선택 (모든 자격증 공통) */}
      <div className="mt-3 rounded-lg border border-emerald-500/20 bg-emerald-500/5 p-3">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="text-xs text-emerald-300">
              <span className="font-semibold">평균 난이도 (SQLD/정처기/컴활 공통):</span>{" "}
              <span className="text-emerald-300/70">
                {engineerDifficulty === "EASY" && "쉬움 위주"}
                {engineerDifficulty === "NORMAL" && "보통 위주"}
                {engineerDifficulty === "HARD" && "어려움 위주"}
                {engineerDifficulty === "VERY_HARD" && "매우 어려움 위주"}
              </span>
            </div>
            <div className="flex flex-wrap gap-2">
              {(["EASY", "NORMAL", "HARD", "VERY_HARD"] as const).map((d) => (
                <button
                  key={d}
                  onClick={() => setEngineerDifficulty(d)}
                  disabled={creating !== null}
                  className={`rounded px-3 py-1 text-xs font-medium transition-colors ${
                    engineerDifficulty === d
                      ? "bg-emerald-500/30 text-emerald-200 ring-1 ring-emerald-400/50"
                      : "border border-emerald-500/30 text-emerald-300/70 hover:bg-emerald-500/10"
                  }`}
                >
                  {d === "EASY"
                    ? "쉬움"
                    : d === "NORMAL"
                      ? "보통"
                      : d === "HARD"
                        ? "어려움"
                        : "매우 어려움"}
                </button>
              ))}
            </div>
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
                  <th className="px-4 py-3 w-44">공개 상태</th>
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
                      <td className="px-4 py-3 font-medium">
                        <div className="flex flex-wrap items-center gap-2">
                          <Link
                            href={`/admin/mock-exams/${exam.id}`}
                            className="hover:text-violet-300 transition-colors"
                          >
                            {exam.name}
                          </Link>
                          {exam.kind === "PAST_EXAM" && (
                            <span className="inline-flex items-center rounded-md border border-amber-500/50 bg-amber-500/15 px-2 py-0.5 text-[10px] font-bold text-amber-300">
                              기출{exam.examYear ? ` · ${exam.examYear}` : ""}
                              {exam.examRound ? `년 ${exam.examRound}회` : ""}
                            </span>
                          )}
                          {exam.expertVerified && (
                            <span className="inline-flex items-center rounded-md border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-bold text-emerald-300">
                              전문가 검수 완료
                            </span>
                          )}
                          {exam.templateLabel && (
                            <span className="inline-flex items-center rounded-md border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-medium text-emerald-300">
                              {exam.templateLabel}
                            </span>
                          )}
                        </div>
                      </td>
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
                      <td className="px-4 py-3">
                        <VisibilityControl
                          current={exam.visibility}
                          onChange={(v) => handleVisibility(exam.id, v)}
                        />
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {new Date(exam.createdAt).toLocaleDateString("ko-KR")}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-3">
                          <Link
                            href={`/admin/mock-exams/${exam.id}`}
                            className="text-xs text-violet-300 hover:text-violet-200"
                          >
                            수정
                          </Link>
                          <button
                            onClick={() => handleDelete(exam.id, exam.name)}
                            className="text-xs text-red-400 hover:text-red-300"
                          >
                            삭제
                          </button>
                        </div>
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

const VISIBILITY_OPTIONS: { value: MockExamVisibility; label: string; cls: string }[] = [
  {
    value: "DRAFT",
    label: "비공개",
    cls: "border-zinc-500/40 bg-zinc-500/10 text-zinc-300",
  },
  {
    value: "PUBLISHED",
    label: "공개",
    cls: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  },
  {
    value: "PREMIUM",
    label: "프리미엄",
    cls: "border-amber-500/40 bg-amber-500/10 text-amber-300",
  },
];

function VisibilityControl({
  current,
  onChange,
}: {
  current: MockExamVisibility;
  onChange: (v: MockExamVisibility) => void;
}) {
  return (
    <div className="flex flex-wrap gap-1">
      {VISIBILITY_OPTIONS.map((opt) => (
        <button
          key={opt.value}
          onClick={() => onChange(opt.value)}
          className={`rounded border px-2 py-0.5 text-[10px] font-medium transition-colors ${
            current === opt.value
              ? `${opt.cls} ring-1 ring-current`
              : "border-border text-muted hover:text-foreground"
          }`}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}

function difficultyTdClass(label: NonNullable<AdminMockExam["difficultyLabel"]>): string {
  switch (label) {
    case "쉬움":
      return "border-emerald-500/40 bg-emerald-500/10 text-emerald-300";
    case "보통":
      return "border-amber-500/40 bg-amber-500/10 text-amber-300";
    case "어려움":
      return "border-orange-500/40 bg-orange-500/10 text-orange-300";
    case "매우 어려움":
      return "border-red-500/40 bg-red-500/10 text-red-300";
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

/**
 * 사용자 노출 모의고사 전부의 PDF 를 R2 캐시에 미리 채워두는 백필 트리거.
 * 시작 후 status 폴링 (3초 주기). RUNNING/DONE 상태를 라벨로 표시.
 */
function PdfBackfillButton() {
  const [status, setStatus] = useState<PdfBackfillStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 페이지 진입 시 한 번 + RUNNING 일 때 3초 폴링
  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setInterval> | null = null;

    const tick = async () => {
      try {
        const s = await getMockExamPdfBackfillStatus();
        if (cancelled) return;
        setStatus(s);
        if (s.state !== "RUNNING" && timer) {
          clearInterval(timer);
          timer = null;
        }
      } catch {
        // 권한·네트워크 실패는 조용히 무시 (어드민이 아니거나 일시적 장애)
      }
    };

    tick();
    timer = setInterval(tick, 3000);

    return () => {
      cancelled = true;
      if (timer) clearInterval(timer);
    };
  }, [status?.state]);

  const handleClick = async () => {
    setError(null);
    try {
      const s = await startMockExamPdfBackfill();
      setStatus(s);
    } catch (e) {
      setError(e instanceof Error ? e.message : "백필 시작 실패");
    }
  };

  const isRunning = status?.state === "RUNNING";
  const label = (() => {
    if (isRunning) {
      return `PDF 백필 중… ${status.processed}/${status.total}`;
    }
    if (status?.state === "DONE") {
      return `PDF 백필 (직전: 신규 ${status.generated} · 캐시 ${status.cached}${status.failed ? ` · 실패 ${status.failed}` : ""})`;
    }
    return "PDF 백필 시작";
  })();

  return (
    <div className="flex flex-col items-end gap-1">
      <button
        onClick={handleClick}
        disabled={isRunning}
        className="rounded-lg border border-rose-500/40 bg-rose-500/10 px-3 py-2 text-xs font-semibold text-rose-300 transition hover:bg-rose-500/20 disabled:opacity-50"
        title="사용자 노출 모의고사(검수+공개) 의 PDF 를 백그라운드에서 일괄 생성해 R2 캐시에 채운다."
      >
        {label}
      </button>
      {error && <p className="text-[10px] text-rose-400">{error}</p>}
    </div>
  );
}
