"use client";

import { useEffect, useState } from "react";
import {
  getAdminMockExams,
  createMockExam,
  deleteMockExam,
  type AdminMockExam,
} from "@/lib/adminApi";

export default function AdminMockExamsPage() {
  const [exams, setExams] = useState<AdminMockExam[] | null>(null);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      const data = await getAdminMockExams();
      setExams(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다.");
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCreate() {
    setCreating(true);
    setError(null);
    try {
      await createMockExam();
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "생성에 실패했습니다.");
    } finally {
      setCreating(false);
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

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">모의고사 관리</h1>
        <button
          onClick={handleCreate}
          disabled={creating}
          className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-zinc-900 hover:bg-primary-hover disabled:opacity-50"
        >
          {creating ? "생성 중..." : "+ 새 모의고사 생성"}
        </button>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-500/30 bg-red-500/5 p-3 text-sm text-red-400">
          {error}
        </div>
      )}

      <div className="mt-6">
        {exams === null ? (
          <p className="text-muted">로딩 중...</p>
        ) : exams.length === 0 ? (
          <p className="text-muted">생성된 모의고사가 없습니다.</p>
        ) : (
          <div className="overflow-hidden rounded-xl border border-border">
            <table className="w-full text-sm">
              <thead className="bg-surface text-left text-muted">
                <tr>
                  <th className="px-4 py-3">#</th>
                  <th className="px-4 py-3">이름</th>
                  <th className="px-4 py-3">문항 수</th>
                  <th className="px-4 py-3">생성일</th>
                  <th className="px-4 py-3 text-right">관리</th>
                </tr>
              </thead>
              <tbody>
                {exams.map((exam) => (
                  <tr
                    key={exam.id}
                    className="border-t border-border hover:bg-surface/50"
                  >
                    <td className="px-4 py-3">{exam.sequence}</td>
                    <td className="px-4 py-3 font-medium">{exam.name}</td>
                    <td className="px-4 py-3">{exam.totalQuestions}</td>
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
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
