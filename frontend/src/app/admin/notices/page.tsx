"use client";

import { useEffect, useState } from "react";
import {
  listNotices,
  createNotice,
  updateNotice,
  setNoticeActive,
  deleteNotice,
  type AdminNotice,
  type NoticeDisplayType,
  type NoticePayload,
} from "@/lib/adminApi";
import { formatDate } from "@/lib/format";

const EMPTY_FORM: NoticePayload = {
  displayType: "MODAL",
  title: "",
  body: "",
  active: false,
};

const TYPE_LABEL: Record<NoticeDisplayType, string> = {
  BANNER: "🎀 상단 배너",
  MODAL: "💬 진입 모달",
};

export default function AdminNoticesPage() {
  const [items, setItems] = useState<AdminNotice[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | "new" | null>(null);
  const [form, setForm] = useState<NoticePayload>(EMPTY_FORM);

  function load() {
    setLoading(true);
    listNotices()
      .then(setItems)
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  function startNew() {
    setEditingId("new");
    setForm(EMPTY_FORM);
  }

  function startEdit(n: AdminNotice) {
    setEditingId(n.id);
    setForm({
      displayType: n.displayType,
      title: n.title ?? "",
      body: n.body,
      active: n.active,
    });
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function handleSave() {
    if (!form.body.trim()) {
      alert("본문을 입력해주세요.");
      return;
    }
    const payload: NoticePayload = {
      ...form,
      title: form.title?.trim() ? form.title.trim() : null,
    };
    try {
      if (editingId === "new") {
        await createNotice(payload);
      } else if (typeof editingId === "number") {
        await updateNotice(editingId, payload);
      }
      cancelEdit();
      load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "저장 실패");
    }
  }

  async function handleToggleActive(n: AdminNotice) {
    setBusyId(n.id);
    try {
      await setNoticeActive(n.id, !n.active);
      load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "변경 실패");
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(n: AdminNotice) {
    if (!confirm(`#${n.id} 공지를 삭제할까요?`)) return;
    setBusyId(n.id);
    try {
      await deleteNotice(n.id);
      load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "삭제 실패");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">공지사항 관리</h1>
          <p className="mt-1 text-sm text-muted">
            상단 배너 / 진입 모달로 노출되는 공지를 등록·수정합니다. display_type별 활성 1건이 최신 기준으로 노출됩니다.
          </p>
        </div>
        {editingId === null && (
          <button
            onClick={startNew}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-zinc-900 hover:bg-primary-hover"
          >
            + 새 공지
          </button>
        )}
      </div>

      {editingId !== null && (
        <div className="mt-6 rounded-xl border border-border bg-surface p-5">
          <h2 className="text-lg font-semibold">
            {editingId === "new" ? "새 공지 작성" : `#${editingId} 수정`}
          </h2>

          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <label className="text-sm">
              <span className="text-muted">노출 위치</span>
              <select
                value={form.displayType}
                onChange={(e) => setForm({ ...form, displayType: e.target.value as NoticeDisplayType })}
                className="mt-1 w-full rounded-md border border-border bg-background px-3 py-2"
              >
                <option value="MODAL">진입 모달</option>
                <option value="BANNER">상단 배너</option>
              </select>
            </label>
            <label className="flex items-end gap-2 text-sm">
              <input
                type="checkbox"
                checked={form.active}
                onChange={(e) => setForm({ ...form, active: e.target.checked })}
                className="h-4 w-4"
              />
              <span>지금 활성화</span>
            </label>
          </div>

          <label className="mt-4 block text-sm">
            <span className="text-muted">제목 (모달용, 선택)</span>
            <input
              type="text"
              value={form.title ?? ""}
              maxLength={200}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              placeholder="예: 📢 공지드려요!"
              className="mt-1 w-full rounded-md border border-border bg-background px-3 py-2"
            />
          </label>

          <label className="mt-4 block text-sm">
            <span className="text-muted">본문 (줄바꿈 가능)</span>
            <textarea
              value={form.body}
              onChange={(e) => setForm({ ...form, body: e.target.value })}
              rows={6}
              className="mt-1 w-full resize-y rounded-md border border-border bg-background px-3 py-2"
            />
          </label>

          <div className="mt-5 flex justify-end gap-2">
            <button
              onClick={cancelEdit}
              className="rounded-lg border border-border px-4 py-2 text-sm text-muted hover:text-foreground"
            >
              취소
            </button>
            <button
              onClick={handleSave}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-zinc-900 hover:bg-primary-hover"
            >
              저장
            </button>
          </div>
        </div>
      )}

      {loading && <p className="mt-6 text-muted">로딩 중...</p>}

      {items && items.length === 0 && (
        <p className="mt-12 text-center text-muted">등록된 공지가 없습니다.</p>
      )}

      {items && items.length > 0 && (
        <div className="mt-6 space-y-3">
          {items.map((n) => (
            <div key={n.id} className="rounded-xl border border-border bg-surface p-5">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="text-xs text-muted tabular-nums">#{n.id}</span>
                  <span className="inline-flex items-center rounded-full border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-[11px] font-bold text-amber-300">
                    {TYPE_LABEL[n.displayType]}
                  </span>
                  <span
                    className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-bold ${
                      n.active
                        ? "border-emerald-500/40 bg-emerald-500/10 text-emerald-300"
                        : "border-zinc-500/40 bg-zinc-500/10 text-zinc-400"
                    }`}
                  >
                    {n.active ? "활성" : "비활성"}
                  </span>
                  <span className="text-[11px] text-muted/70">v{n.version}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-muted/70">{formatDate(n.updatedAt)}</span>
                  <button
                    onClick={() => handleToggleActive(n)}
                    disabled={busyId === n.id}
                    className="rounded border border-border px-2 py-1 text-xs text-muted hover:text-foreground disabled:opacity-30"
                  >
                    {n.active ? "비활성화" : "활성화"}
                  </button>
                  <button
                    onClick={() => startEdit(n)}
                    className="rounded border border-border px-2 py-1 text-xs text-muted hover:text-foreground"
                  >
                    수정
                  </button>
                  <button
                    onClick={() => handleDelete(n)}
                    disabled={busyId === n.id}
                    className="rounded border border-red-500/30 px-2 py-1 text-xs text-red-400 hover:bg-red-500/10 disabled:opacity-30"
                  >
                    삭제
                  </button>
                </div>
              </div>
              {n.title && <p className="mt-3 text-sm font-semibold text-amber-300">{n.title}</p>}
              <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed">{n.body}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
