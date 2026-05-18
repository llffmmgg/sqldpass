# Step 4 — All Pass 정책 변경 안내 모달

## 배경

Step 1~3 으로 All Pass 가 평생 → 6개월(180일) 로 전환되었다. 사용자가 가격 페이지에 들어왔을 때:

1. **신규 사용자**: 평생권 인줄 알고 들어왔다가 6개월권으로 바뀐 걸 모르고 결제하는 혼선 방지
2. **기존 평생 구매자**: 정책 변경 소식 들었을 때 "내 권리도 사라지는 거 아닌가" 불안 해소

이를 위해 `/plan`, `/checkout` 진입 시 안내 모달을 1회 노출. `/plan` 은 `/checkout` 으로 리다이렉트되므로 실질적으로 `/checkout` 한 곳에만 통합하면 둘 다 커버된다.

**사용자 결정 사항**:
- sessionStorage 로 세션당 1회 노출 (브라우저 닫고 다시 들어오면 재노출)
- 중앙 오버레이 모달
- /plan 과 /checkout 둘 다 동일 문구 (실제로는 redirect 라 /checkout 한 곳에서 처리)

## 작업 디렉터리

```
frontend/
```

## 신규/변경 파일

| 파일 | 역할 |
|------|------|
| `frontend/src/components/billing/AllPassPolicyNotice.tsx` (신규) | 모달 컴포넌트. sessionStorage 게이트 + dismiss 처리 |
| `frontend/src/app/checkout/CheckoutClient.tsx` (수정) | `<AllPassPolicyNotice />` 마운트 |

## 디자인 톤

- **금지**: `backdrop-blur`, drop-shadow glow, opacity pulse, `/5~/10` 옅은 배경. (메모리 — Supabase 단단한 톤 유지)
- 오버레이는 `bg-black/70` 단단한 색.
- 카드: `bg-surface` + `border-border` + `shadow-xl`.
- 강조 칩: `bg-primary/10 border-primary/40 text-primary` (primary 토큰).
- 본문 텍스트는 `text-text-muted`, 강조는 `<strong className="text-text">` 로 가독성.
- 닫기 버튼: 기존 `Button` 컴포넌트 `variant="primary"` 1개. ESC / 오버레이 클릭 / 버튼 클릭 모두 dismiss.

## sessionStorage 키

```
all-pass-6month-notice-dismissed-2026-05
```

날짜 suffix 로 향후 다른 정책 공지 추가 시 키 분리 가능.

## 모달 컴포넌트 (AllPassPolicyNotice.tsx)

```tsx
"use client";

import { useEffect, useState } from "react";

import { Button } from "@/components/ui";

const STORAGE_KEY = "all-pass-6month-notice-dismissed-2026-05";

/**
 * All Pass 플랜 정책 변경(평생→6개월) 안내 모달.
 * - PG사 제약으로 평생 상품 유지 불가 → 6개월(180일) 로 전환.
 * - 기존 평생 구매자는 권리 유지됨을 함께 안내.
 * - sessionStorage 로 세션당 1회만 노출.
 */
export default function AllPassPolicyNotice() {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    try {
      const dismissed = sessionStorage.getItem(STORAGE_KEY) === "1";
      if (!dismissed) setOpen(true);
    } catch {
      // sessionStorage 차단 환경(시크릿 + 일부 정책) 도 1회는 노출
      setOpen(true);
    }
  }, []);

  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") handleDismiss();
    }
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
    // handleDismiss 는 stable — open 만 의존
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function handleDismiss() {
    try {
      sessionStorage.setItem(STORAGE_KEY, "1");
    } catch {
      // sessionStorage 차단 시 무시 — 본 세션 안에서만 닫힘 유지
    }
    setOpen(false);
  }

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[60] bg-black/70"
      onClick={handleDismiss}
      role="dialog"
      aria-modal="true"
      aria-labelledby="all-pass-policy-title"
    >
      <div className="flex min-h-full items-center justify-center px-4 py-6 sm:py-8">
        <div
          className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 shadow-xl sm:p-7"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="mb-4 flex items-center gap-2.5">
            <span className="inline-flex items-center rounded border border-primary/40 bg-primary/10 px-2 py-0.5 text-[10.5px] font-bold tracking-wide text-primary">
              안내
            </span>
            <h2
              id="all-pass-policy-title"
              className="text-[17px] font-bold tracking-tight text-text"
            >
              All Pass 플랜 변경 안내
            </h2>
          </div>

          <div className="space-y-3 text-[13.5px] leading-[1.7] text-text-muted">
            <p>
              결제 대행사(PG) 정책상 <strong className="text-text">평생 이용권</strong> 상품을
              더 이상 유지할 수 없게 되었어요. 이에 따라 All Pass 플랜의 이용 기간이{" "}
              <strong className="text-text">6개월(180일)</strong> 로 변경되었습니다.
            </p>
            <p>
              <strong className="text-text">이미 평생 All Pass 를 구매하신 분들은</strong>{" "}
              그대로 평생 이용 권리가 유지되니 안심해주세요. 변경 사항은 신규 결제부터만
              적용됩니다.
            </p>
          </div>

          <div className="mt-6 flex justify-end">
            <Button variant="primary" size="md" onClick={handleDismiss}>
              확인했어요
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
```

## CheckoutClient.tsx 통합

`CheckoutClient` 의 `Suspense` 아래에 모달을 마운트 — sessionStorage gate 라 SSR 시점에는 보이지 않아야 하므로 `"use client"` 모달이 마운트되면 useEffect 에서 판단해서 열림.

```diff
 import BuyerInfoModal from "@/components/billing/BuyerInfoModal";
+import AllPassPolicyNotice from "@/components/billing/AllPassPolicyNotice";
 import NoAdsGuard from "@/components/NoAdsGuard";
```

`CheckoutClient` 함수 본문 내 `Suspense` 바깥 또는 `CheckoutContent` 의 최상단에 마운트. **CheckoutContent 최상단**으로 — `CheckoutLanding` 과 같은 흐름 내에 두면 자연스럽다.

```diff
 function CheckoutContent() {
   const router = useRouter();
   ...
   return (
     <>
+      <AllPassPolicyNotice />
       ...existing JSX...
     </>
   );
 }
```

(`CheckoutContent` 의 실제 return 구조는 읽고 정확한 위치 파악 후 삽입)

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 확인:
- 시크릿 창에서 `/checkout` 진입 → 모달 노출 → 확인 → 같은 탭에서 새로고침 → 모달 안 뜸 (sessionStorage 유지)
- 탭 닫고 새 탭에서 `/checkout` → 다시 노출
- `/plan` 진입 → `/checkout` 으로 redirect → 모달 노출
- ESC, 오버레이 클릭, 확인 버튼 — 모두 dismiss 동작

## Acceptance Criteria

1. `AllPassPolicyNotice.tsx` 가 생성되고 `/checkout` 에서 1회 노출됨 (세션당)
2. ESC / 오버레이 클릭 / 확인 버튼 모두 dismiss
3. dismiss 후 sessionStorage 에 키 저장 — 새로고침해도 다시 안 뜸
4. backdrop-blur 사용 안 함 (Supabase 톤 유지)
5. `npm run lint`, `npm run build` 통과

## 금지 사항

- `backdrop-blur-sm`, drop-shadow glow 사용하지 마라. **이유**: 메모리 — AI 스러운 흐릿한 효과 금지 (Supabase 단단한 톤).
- 정책 안내 본문에 PG사 이름(KG이니시스, 카카오페이 등) 명시하지 마라. **이유**: 운영 PG 변경 시 본문도 같이 손봐야 하는 잔재. "결제 대행사(PG)" 만으로 충분.
- localStorage 사용하지 마라. **이유**: 사용자 결정 — sessionStorage 만.
- 페이지 전체를 가리는 modal 외 다른 노출 형태(우상단 토스트, 인라인 배너) 를 함께 넣지 마라. **이유**: 사용자 결정 — 중앙 모달 단일.

## Status 규칙

- 성공: step 4 `completed`, summary "AllPassPolicyNotice 모달 신규 + CheckoutContent 통합, sessionStorage 1회 노출, lint+build OK".
- 실패: 3회 재시도 후 `error`.
