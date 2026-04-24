/**
 * 햅틱 피드백 유틸 — navigator.vibrate 래퍼.
 *
 * - Android Chrome/Samsung Browser: 동작
 * - iOS Safari: 미지원 (무시됨, 에러 안 남)
 * - 데스크톱: 대부분 미지원 (무시)
 * - SSR 안전 (typeof window check)
 */

function vibrate(pattern: number | number[]) {
  if (typeof window === "undefined") return;
  if (typeof navigator === "undefined") return;
  if (typeof navigator.vibrate !== "function") return;
  try {
    navigator.vibrate(pattern);
  } catch {
    // noop
  }
}

/** 가벼운 탭 (버튼 클릭 등) */
export function hapticLight() {
  vibrate(10);
}

/** 정답 — 짧고 확실한 한 번 */
export function hapticSuccess() {
  vibrate(30);
}

/** 오답 — 두 번 투닥 */
export function hapticError() {
  vibrate([40, 30, 40]);
}
