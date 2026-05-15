const KEY = "seen_solve_tutorial_v1";

export function hasSeenSolveTutorial(): boolean {
  if (typeof window === "undefined") return true;
  try {
    return localStorage.getItem(KEY) === "1";
  } catch {
    return true;
  }
}

export function markSolveTutorialSeen(): void {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(KEY, "1");
  } catch {
    // localStorage 차단 환경에서는 매 진입 시 다시 노출되어도 안전한 동작.
  }
}
