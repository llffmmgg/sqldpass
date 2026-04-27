"use client";

import { useRef, useState } from "react";

import { uploadImage } from "@/lib/api";

/**
 * 이미지 업로드 버튼.
 * 파일 선택 → R2 직접 업로드 → public URL 을 markdown image 문법으로 반환.
 * 부모는 onInsert(markdown) 콜백으로 textarea 에 삽입한다.
 */
export default function ImageUploadButton({
  onInsert,
  disabled,
}: {
  onInsert: (markdown: string) => void;
  disabled?: boolean;
}) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError(null);
    setBusy(true);
    try {
      const url = await uploadImage(file);
      const alt = file.name.replace(/\.[^.]+$/, "");
      onInsert(`\n\n![${alt}](${url})\n\n`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "업로드 실패");
    } finally {
      setBusy(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  }

  return (
    <div className="flex items-center gap-2">
      <input
        ref={inputRef}
        type="file"
        accept="image/png,image/jpeg,image/webp,image/gif"
        className="hidden"
        onChange={handleChange}
        disabled={disabled || busy}
      />
      <button
        type="button"
        disabled={disabled || busy}
        onClick={() => inputRef.current?.click()}
        className="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-2.5 py-1 text-xs font-medium text-text-muted transition-colors hover:border-border-strong hover:text-text disabled:cursor-not-allowed disabled:opacity-50"
      >
        {busy ? (
          <>
            <svg className="h-3 w-3 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden>
              <circle cx="12" cy="12" r="10" stroke="currentColor" strokeOpacity="0.25" strokeWidth="3" />
              <path d="M22 12a10 10 0 0 1-10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
            </svg>
            업로드 중…
          </>
        ) : (
          <>🖼 이미지 첨부</>
        )}
      </button>
      {error && <span className="text-xs text-red-400">{error}</span>}
    </div>
  );
}
