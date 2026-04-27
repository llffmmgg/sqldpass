import type { ClipboardEvent } from "react";

import { uploadImage } from "@/lib/api";

/**
 * textarea onPaste 헬퍼.
 * 클립보드에 이미지가 있으면 R2 에 업로드 후 insert(markdown) 호출.
 * 일반 텍스트 paste 는 그대로 통과시킨다 (preventDefault X).
 */
export async function handleImagePaste(
  e: ClipboardEvent<HTMLTextAreaElement>,
  insert: (markdown: string) => void,
  onError?: (msg: string) => void,
): Promise<boolean> {
  const items = e.clipboardData?.items;
  if (!items) return false;
  for (const item of items) {
    if (item.kind === "file" && item.type.startsWith("image/")) {
      e.preventDefault();
      const file = item.getAsFile();
      if (!file) return false;
      try {
        const url = await uploadImage(file);
        const alt = `pasted-${Date.now()}`;
        insert(`\n\n![${alt}](${url})\n\n`);
        return true;
      } catch (err) {
        onError?.(err instanceof Error ? err.message : "이미지 업로드 실패");
        return false;
      }
    }
  }
  return false;
}
