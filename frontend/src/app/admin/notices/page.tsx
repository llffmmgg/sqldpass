"use client";

import { useEffect, useMemo, useState } from "react";
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

function pickLive(items: AdminNotice[]): Record<NoticeDisplayType, number | undefined> {
  const pick = (t: NoticeDisplayType) =>
    items
      .filter((i) => i.displayType === t && i.active)
      .sort((a, b) => (a.updatedAt < b.updatedAt ? 1 : -1))[0]?.id;
  return { BANNER: pick("BANNER"), MODAL: pick("MODAL") };
}

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

  const liveMap = useMemo(() => pickLive(items ?? []), [items]);

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

  async function deactivateOthersIfNeeded(displayType: NoticeDisplayType, exceptId: number | null): Promise<boolean> {
    const others = (items ?? []).filter(
      (i) => i.displayType === displayType && i.active && i.id !== exceptId,
    );
    if (others.length === 0) return true;
    const preview = others[0].body.slice(0, 20);
    const ok = confirm(
      `기존 활성 ${TYPE_LABEL[displayType]} 공지 "${preview}${others[0].body.length > 20 ? "…" : ""}"를 비활성화하고 이 공지를 노출할까요?`,
    );
    if (!ok) return false;
    for (const o of others) {
      await setNoticeActive(o.id, false);
    }
    return true;
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
        if (payload.active) {
          const proceed = await deactivateOthersIfNeeded(payload.displayType, null);
          if (!proceed) return;
        }
        await createNotice(payload);
      } else if (typeof editingId === "number") {
        if (payload.active) {
          const proceed = await deactivateOthersIfNeeded(payload.displayType, editingId);
          if (!proceed) return;
        }
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
      if (!n.active) {
        const proceed = await deactivateOthersIfNeeded(n.displayType, n.id);
        if (!proceed) {
          setBusyId(null);
          return;
        }
      }
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
            상단 배너 / 진입 모달로 노출되는 공지를 등록·수정합니다. display_type별 활성 1건(최신 updatedAt 기준)이 실제 노출됩니다.
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

          <div className="mt-5">
            <div className="text-xs text-muted">미리보기</div>
            <div className="mt-2 overflow-hidden rounded-lg border border-border">
              {form.displayType === "BANNER" ? (
                <div className="flex items-center justify-center gap-3 border-b border-primary/30 bg-primary/10 px-4 py-2.5 text-center text-sm font-medium text-primary">
                  <span className="whitespace-pre-wrap break-words">
                    <span className="mr-1">📢</span>
                    {form.body || "본문을 입력하면 여기 표시됩니다"}
                  </span>
                </div>
              ) : (
                <div className="bg-background p-5">
                  {form.title && <p className="text-base font-semibold text-amber-300">{form.title}</p>}
                  <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed">
                    {form.body || "본문을 입력하면 여기 표시됩니다"}
                  </p>
                </div>
              )}
            </div>
          </div>

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
          {items.map((n) => {
            const isLive = liveMap[n.displayType] === n.id;
            return (
              <div key={n.id} className="rounded-xl border border-border bg-surface p-5">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs text-muted tabular-nums">#{n.id}</span>
                    <span className="inline-flex items-center rounded-full border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-[11px] font-bold text-amber-300">
                      {TYPE_LABEL[n.displayType]}
                    </span>
                    {isLive ? (
                      <span className="inline-flex items-center rounded-full border border-red-500/40 bg-red-500/10 px-2 py-0.5 text-[11px] font-bold text-red-300">
                        🔴 지금 노출 중
                      </span>
                    ) : n.active ? (
                      <span className="inline-flex items-center rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[11px] font-bold text-emerald-300">
                        활성(대기)
                      </span>
                    ) : (
                      <span className="inline-flex items-center rounded-full border border-zinc-500/40 bg-zinc-500/10 px-2 py-0.5 text-[11px] font-bold text-zinc-400">
                        비활성
                      </span>
                    )}
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
            );
          })}
        </div>
      )}
    </div>
  );
}
