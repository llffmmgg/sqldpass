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
