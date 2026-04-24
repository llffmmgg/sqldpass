"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  getAdminMockExams,
  changeMockExamVisibility,
  toggleExpertVerified,
  setPastExamMeta,
  type AdminMockExam,
  type MockExamVisibility,
} from "@/lib/adminApi";

const EXAM_LABELS: Record<AdminMockExam["examType"], string> = {
  SQLD: "SQLD",
  ENGINEER_PRACTICAL: "정처기 실기",
  ENGINEER_WRITTEN: "정처기 필기",
  COMPUTER_LITERACY_1: "컴활 1급",
  COMPUTER_LITERACY_2: "컴활 2급",
  ADSP: "ADsP",
};

const VISIBILITY_LABELS: Record<MockExamVisibility, string> = {
  DRAFT: "초안",
  PUBLISHED: "공개",
  PREMIUM: "프리미엄",
};

type MetaDraft = {
  examYear: string;
  examRound: string;
  examDate: string;
};

export default function AdminPastExamsPage() {
  const [items, setItems] = useState<AdminMockExam[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [editingMetaId, setEditingMetaId] = useState<number | null>(null);
  const [metaDraft, setMetaDraft] = useState<MetaDraft>({ examYear: "", examRound: "", examDate: "" });

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      const list = await getAdminMockExams();
      setItems(list);
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  const pastExams = useMemo(
    () => items.filter((m) => m.kind === "PAST_EXAM"),
    [items],
  );

  const grouped = useMemo(() => {
    const map = new Map<AdminMockExam["examType"], AdminMockExam[]>();
    for (const m of pastExams) {
      const arr = map.get(m.examType) ?? [];
      arr.push(m);
      map.set(m.examType, arr);
    }
    for (const arr of map.values()) {
      arr.sort((a, b) => {
        const ay = a.examYear ?? 0;
        const by = b.examYear ?? 0;
        if (ay !== by) return by - ay;
        const ar = a.examRound ?? 0;
        const br = b.examRound ?? 0;
        return br - ar;
      });
    }
    return map;
  }, [pastExams]);

  async function onToggleVerified(m: AdminMockExam) {
    setBusyId(m.id);
    try {
      await toggleExpertVerified(m.id);
      await refresh();
    } catch (e) {
      alert(e instanceof Error ? e.message : "토글 실패");
    } finally {
      setBusyId(null);
    }
  }

  async function onChangeVisibility(m: AdminMockExam, next: MockExamVisibility) {
    if (m.visibility === next) return;
    setBusyId(m.id);
    try {
      await changeMockExamVisibility(m.id, next);
      await refresh();
    } catch (e) {
      alert(e instanceof Error ? e.message : "공개 상태 변경 실패");
    } finally {
      setBusyId(null);
    }
  }

  function openMetaEditor(m: AdminMockExam) {
    setEditingMetaId(m.id);
    setMetaDraft({
      examYear: m.examYear != null ? String(m.examYear) : "",
      examRound: m.examRound != null ? String(m.examRound) : "",
      examDate: m.examDate ?? "",
    });
  }

  async function saveMeta(m: AdminMockExam) {
    setBusyId(m.id);
    try {
      const payload = {
        promote: true,
        examYear: metaDraft.examYear ? Number(metaDraft.examYear) : null,
        examRound: metaDraft.examRound ? Number(metaDraft.examRound) : null,
        examDate: metaDraft.examDate || null,
      };
      await setPastExamMeta(m.id, payload);
      setEditingMetaId(null);
      await refresh();
    } catch (e) {
      alert(e instanceof Error ? e.message : "회차 정보 저장 실패");
    } finally {
      setBusyId(null);
    }
  }

  async function demoteToAi(m: AdminMockExam) {
    if (!confirm(`${m.name} 을 AI 모의고사로 되돌릴까요? (kind=AI 로 변경, 연도/회차 제거)`)) return;
    setBusyId(m.id);
    try {
      await setPastExamMeta(m.id, { promote: false });
      await refresh();
    } catch (e) {
      alert(e instanceof Error ? e.message : "AI 모의고사로 되돌리기 실패");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">기출 복원</h1>
          <p className="mt-1 text-sm text-muted">
            kind=PAST_EXAM 회차만 노출합니다. 회차 정보 편집과 `/past-exams` 공개용 expert_verified 토글을 여기서 관리하세요.
          </p>
        </div>
        <button
          onClick={refresh}
          disabled={loading}
          className="rounded-md border border-border bg-surface px-3 py-1.5 text-sm text-muted hover:text-foreground disabled:opacity-50"
        >
          새로고침
        </button>
      </header>

      {error && (
        <div className="rounded-md border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-muted">불러오는 중...</p>
      ) : pastExams.length === 0 ? (
        <div className="rounded-lg border border-border bg-surface/50 p-6 text-sm text-muted">
          아직 기출 복원 회차가 없습니다. 모의고사 탭에서 생성 후 <strong>기출 메타</strong>를 설정하거나, Flyway 마이그레이션으로 투입할 수 있습니다.
        </div>
      ) : (
        Array.from(grouped.entries()).map(([type, list]) => (
          <section key={type} className="rounded-lg border border-border bg-surface/40">
            <header className="flex items-center justify-between border-b border-border px-4 py-3">
              <h2 className="text-base font-semibold">
                {EXAM_LABELS[type] ?? type}
                <span className="ml-2 text-xs font-normal text-muted">({list.length}회차)</span>
              </h2>
            </header>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs text-muted">
                    <th className="px-4 py-2 font-medium">회차</th>
                    <th className="px-4 py-2 font-medium">이름</th>
                    <th className="px-4 py-2 font-medium">시험일</th>
                    <th className="px-4 py-2 text-center font-medium">문항</th>
                    <th className="px-4 py-2 font-medium">공개</th>
                    <th className="px-4 py-2 font-medium">검수</th>
                    <th className="px-4 py-2 font-medium">액션</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((m) => {
                    const visiblePublicly = m.visibility === "PUBLISHED" && m.expertVerified;
                    const busy = busyId === m.id;
                    const isEditing = editingMetaId === m.id;
                    return (
                      <tr key={m.id} className="border-b border-border/60 align-top">
                        <td className="px-4 py-3 whitespace-nowrap">
                          {isEditing ? (
                            <div className="flex flex-col gap-1">
                              <input
                                type="number"
                                placeholder="연도"
                                value={metaDraft.examYear}
                                onChange={(e) => setMetaDraft((d) => ({ ...d, examYear: e.target.value }))}
                                className="w-24 rounded border border-border bg-background px-2 py-1 text-xs"
                              />
                              <input
                                type="number"
                                placeholder="회차"
                                value={metaDraft.examRound}
                                onChange={(e) => setMetaDraft((d) => ({ ...d, examRound: e.target.value }))}
                                className="w-24 rounded border border-border bg-background px-2 py-1 text-xs"
                              />
                            </div>
                          ) : (
                            <span className="font-mono text-xs text-muted">
                              {m.examYear ?? "—"}년 / 제{m.examRound ?? "—"}회
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <Link
                            href={`/admin/mock-exams/${m.id}`}
                            className="text-foreground hover:text-primary"
                          >
                            {m.name}
                          </Link>
                          <div className="mt-0.5 text-[11px] text-muted">#{m.id} · seq {m.sequence}</div>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-xs text-muted">
                          {isEditing ? (
                            <input
                              type="date"
                              value={metaDraft.examDate}
                              onChange={(e) => setMetaDraft((d) => ({ ...d, examDate: e.target.value }))}
                              className="rounded border border-border bg-background px-2 py-1 text-xs"
                            />
                          ) : (
                            m.examDate ?? "—"
                          )}
                        </td>
                        <td className="px-4 py-3 text-center text-xs tabular-nums">{m.totalQuestions}</td>
                        <td className="px-4 py-3">
                          <select
                            value={m.visibility}
                            onChange={(e) => onChangeVisibility(m, e.target.value as MockExamVisibility)}
                            disabled={busy}
                            className="rounded border border-border bg-background px-2 py-1 text-xs disabled:opacity-50"
                          >
                            {(Object.keys(VISIBILITY_LABELS) as MockExamVisibility[]).map((v) => (
                              <option key={v} value={v}>
                                {VISIBILITY_LABELS[v]}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td className="px-4 py-3">
                          <button
                            onClick={() => onToggleVerified(m)}
                            disabled={busy}
                            className={`rounded-md border px-2 py-1 text-xs font-medium transition-colors disabled:opacity-50 ${
                              m.expertVerified
                                ? "border-emerald-500/40 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20"
                                : "border-border text-muted hover:text-foreground"
                            }`}
                          >
                            {m.expertVerified ? "✓ 검수됨" : "미검수"}
                          </button>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap items-center gap-1.5">
                            {visiblePublicly && (
                              <span className="rounded-full border border-primary/40 bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                                공개 중
                              </span>
                            )}
                            {isEditing ? (
                              <>
                                <button
                                  onClick={() => saveMeta(m)}
                                  disabled={busy}
                                  className="rounded border border-primary/40 bg-primary/10 px-2 py-1 text-xs font-medium text-primary hover:bg-primary/20 disabled:opacity-50"
                                >
                                  저장
                                </button>
                                <button
                                  onClick={() => setEditingMetaId(null)}
                                  disabled={busy}
                                  className="rounded border border-border px-2 py-1 text-xs text-muted hover:text-foreground disabled:opacity-50"
                                >
                                  취소
                                </button>
                              </>
                            ) : (
                              <>
                                <button
                                  onClick={() => openMetaEditor(m)}
                                  disabled={busy}
                                  className="rounded border border-border px-2 py-1 text-xs text-muted hover:text-foreground disabled:opacity-50"
                                >
                                  회차 편집
                                </button>
                                <Link
                                  href={`/admin/mock-exams/${m.id}`}
                                  className="rounded border border-border px-2 py-1 text-xs text-muted hover:text-foreground"
                                >
                                  문항 편집
                                </Link>
                                <button
                                  onClick={() => demoteToAi(m)}
                                  disabled={busy}
                                  className="rounded border border-border px-2 py-1 text-xs text-muted hover:text-red-300 disabled:opacity-50"
                                >
                                  AI 로 되돌리기
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        ))
      )}
    </div>
  );
}
