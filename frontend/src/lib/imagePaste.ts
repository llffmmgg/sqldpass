import type { ClipboardEvent } from "react";

import { uploadImage } from "@/lib/api";

/**
 * textarea onPaste 헬퍼.
 * 클립보드에 이미지가 있으면:
 *   1) paste 시점의 selection 위치에 placeholder 즉시 삽입
 *   2) R2 업로드 진행 (1~2초)
 *   3) 완료되면 placeholder 를 실제 URL markdown 으로 replace
 * 업로드 동안 사용자가 textarea 다른 데 입력해도 보존됨 (함수형 setter 사용).
 *
 * 일반 텍스트 paste 는 그대로 통과 (preventDefault X).
 */
export async function handleImagePaste(
  e: ClipboardEvent<HTMLTextAreaElement>,
  setContent: (updater: (prev: string) => string) => void,
  onError?: (msg: string) => void,
): Promise<boolean> {
  const items = e.clipboardData?.items;
  if (!items) return false;

  for (const item of items) {
    if (item.kind === "file" && item.type.startsWith("image/")) {
      e.preventDefault();
      const file = item.getAsFile();
      if (!file) return false;

      const ta = e.currentTarget;
      const start = ta.selectionStart ?? 0;
      const end = ta.selectionEnd ?? 0;
      // 고유한 placeholder — 동시에 여러 장 paste 해도 충돌 X
      const id = Math.random().toString(36).slice(2, 10);
      const placeholder = `![업로드 중...](uploading:${id})`;
      const inserted = `\n\n${placeholder}\n\n`;

      // 1) placeholder 즉시 삽입 (함수형 setter — stale closure 회피)
      setContent((prev) => prev.slice(0, start) + inserted + prev.slice(end));

      try {
        const url = await uploadImage(file);
        const real = `![pasted-${Date.now()}](${url})`;
        // 2) placeholder → 실제 URL 로 replace (사용자가 그동안 추가한 텍스트 보존)
        setContent((prev) => prev.replace(placeholder, real));
        return true;
      } catch (err) {
        // 실패 시 placeholder 제거
        setContent((prev) => prev.replace(inserted, ""));
        onError?.(err instanceof Error ? err.message : "이미지 업로드 실패");
        return false;
      }
    }
  }
  return false;
}
