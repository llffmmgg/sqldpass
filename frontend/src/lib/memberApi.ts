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
