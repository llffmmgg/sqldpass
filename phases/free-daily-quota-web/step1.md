# Step 1 — Fetch 인터셉터 + 페이월 모달

## 배경

백엔드는 무료 회원이 일일 한도를 넘기면 `HTTP 402 Payment Required` 와 함께 다음 body를 반환:

```json
{ "error": "DAILY_QUESTION_LIMIT", "used": 30, "limit": 30, "resetAt": "2026-05-22T00:00:00" }
```

웹 클라이언트는:
1. 공용 fetch wrapper 에서 402 를 잡고
2. body 의 `error` 코드에 따라 모달 문구 분기
3. CTA 로 결제 페이지(`/checkout`) 또는 Focus 7일권으로 이동

**카운팅 로직 없음.** 서버가 던지면 모달 띄우고, 아니면 무시. 백엔드 단일 가드를 신뢰.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

먼저 기존 fetch wrapper 가 있는지 확인:

```
frontend/src/lib/api.ts
frontend/src/lib/auth.ts  (참고)
```

`api.ts` 가 공용 fetch 클라이언트일 가능성. 거기에 `Response.status === 402` 분기 추가.

신규/수정 파일:

| 파일 | 변경 |
|------|------|
| `frontend/src/lib/api.ts` (있다면) | 402 응답 시 body 파싱 → `QuotaExceededError` throw + 전역 이벤트 발행 |
| 신규 `frontend/src/components/QuotaPaywallModal.tsx` | 페이월 모달 컴포넌트 |
| 신규 `frontend/src/lib/quotaEvents.ts` | 402 이벤트 발행/구독 (간단한 EventTarget 또는 React Context) |
| `frontend/src/app/layout.tsx` 또는 client 진입 layout | 모달 마운트 + 이벤트 구독 |

## 인터셉터 구현 가이드

`api.ts` 패턴:

```ts
export async function apiFetch(input: RequestInfo, init?: RequestInit) {
  const res = await fetch(input, init);
  if (res.status === 402) {
    const body = await res.json().catch(() => null);
    if (body?.error === "DAILY_QUESTION_LIMIT" || body?.error === "DAILY_MOCK_LIMIT") {
      window.dispatchEvent(new CustomEvent("quota-exceeded", { detail: body }));
    }
    throw new QuotaExceededError(body);
  }
  return res;
}
```

모달은 client component:

```tsx
"use client";
export function QuotaPaywallModal() {
  const [info, setInfo] = useState<QuotaInfo | null>(null);
  useEffect(() => {
    const handler = (e: CustomEvent) => setInfo(e.detail);
    window.addEventListener("quota-exceeded", handler as EventListener);
    return () => window.removeEventListener("quota-exceeded", handler as EventListener);
  }, []);
  if (!info) return null;
  // error 코드별 문구 분기 — 정해진 문구 사용
  ...
}
```

## 모달 문구 (확정)

| error | 제목 | 본문 | CTA |
|---|---|---|---|
| `DAILY_QUESTION_LIMIT` | 오늘의 30문제 완주! 🐙 | 내일 다시 만나거나, Focus 7일권으로 끝까지 가볼까요? | "Focus 7일권 보기" → /checkout |
| `DAILY_MOCK_LIMIT` | 오늘 모의고사 1회 완료 | Focus 7일권으로 매일 풀 수 있어요. | "Focus 7일권 보기" → /checkout |

부수 버튼 "내일 다시 오기" / "닫기" 도 함께. 메모리 `feedback_no_ai_blur_effects` 준수: backdrop-blur, drop-shadow glow, opacity pulse 금지. Supabase 단단한 톤.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

dev server 띄워서 무료 계정으로:
1. 문제 31개 풀이 시도 → 모달
2. 모의고사 2회 진입 시도 → 모달
3. 기출복원 다회 풀이 → 모달 안 뜸

## Acceptance Criteria

1. 공용 fetch wrapper 또는 동등한 위치에 402 분기 추가.
2. QuotaPaywallModal 컴포넌트 신규 + 글로벌 layout 에 마운트.
3. error 코드별 정확한 문구 출력.
4. `npm run lint`, `npm run build` 통과.
5. 모달 디자인이 메모리 `feedback_no_ai_blur_effects` 위반하지 않음 (blur, glow, opacity pulse, /5~/10 옅은 배경 금지).

## 금지 사항

- 자체 카운터 로직 만들지 마라. 이유: 백엔드 단일 가드. 디바이스 시간/재설치 우회 방지.
- backdrop-blur, drop-shadow-* (glow), opacity-* (pulse animation) 사용 금지. 이유: feedback_no_ai_blur_effects.
- 색 계열 변경 금지(메모리 feedback_color_token_changes). amber/zinc 등 기존 토큰 유지.
- 모달을 페이지 컴포넌트 안에 두지 마라. 이유: 한도는 어느 페이지에서든 발생 가능. 글로벌 layout 마운트.
- "유료 회원만 가능합니다" 같은 단정 문구 금지. 이유: 사용자 톤 가이드.

## Status 규칙

- 성공: `phases/free-daily-quota-web/index.json` step 1 `completed`.
- 실패: 3회 후 `error`.
