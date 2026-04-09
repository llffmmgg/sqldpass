import { fetchApi } from "@/lib/api";

export interface MemberMe {
  id: number;
  nickname: string;
  provider: string;
  createdAt: string;
}

export function getMe() {
  return fetchApi<MemberMe>("/members/me");
}

export function updateNickname(nickname: string) {
  return fetchApi<MemberMe>("/members/me/nickname", {
    method: "PATCH",
    body: JSON.stringify({ nickname }),
  });
}

/** 회원 탈퇴 (hard delete). 성공 시 204 — 호출자가 clearAuth + 라우팅 처리. */
export function withdrawMember() {
  return fetchApi<void>("/members/me", { method: "DELETE" });
}
