const TOKEN_KEY = "user_token";
const NICKNAME_KEY = "user_nickname";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setAuth(token: string, nickname: string) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(NICKNAME_KEY, nickname);
}

export function setNickname(nickname: string) {
  localStorage.setItem(NICKNAME_KEY, nickname);
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(NICKNAME_KEY);
}

export function isLoggedIn(): boolean {
  return getToken() !== null;
}

export function getNickname(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(NICKNAME_KEY);
}

/**
 * 안드로이드 Capacitor 앱 — 네이티브 Google Sign-In 으로 받은 ID 토큰을 백엔드에 전달.
 * 호출 전 mobile/ 워크스페이스에 Capacitor Google Auth 플러그인이 설치돼 있어야 한다.
 * (예: `@capacitor-firebase/authentication` 또는 동등한 Cap 7 호환 플러그인.)
 */
export async function loginWithGoogleIdToken(idToken: string): Promise<{
  token: string;
  nickname: string;
  isNew: boolean;
}> {
  const res = await fetch("/api/auth/login/google/idtoken", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ idToken }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: "로그인에 실패했어요." }));
    throw new Error(err.message ?? "로그인에 실패했어요.");
  }
  const body = (await res.json()) as { token: string; nickname: string; isNew: boolean };
  setAuth(body.token, body.nickname);
  return body;
}
