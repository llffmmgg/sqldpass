# Step 1 — Toast 안정화 + CheckoutClient useRef 가드

## 배경

모바일 결제창 외부 앱 전환 후 `/checkout?paymentId=xyz` 복귀 시 토스트가 수십 번 표시되고 페이지가 흔들린다.

**Root cause (HIGH)**:
1. `Toast.tsx:42` `<ToastContext.Provider value={{ show }}>` — 객체 리터럴이 매 렌더 새로 생성
2. `Toast.tsx:25` `useToast` Provider 미존재 fallback `return { show: () => {} }` — 매 호출 새 객체
3. `CheckoutClient.tsx:110` `useEffect(..., [returnedPaymentId, router, toast])` — `toast` 가 unstable → 매 렌더 effect 재실행 → `verifyPaymentById` 반복 호출 → 토스트 N번 + `router.replace` N번

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/components/Toast.tsx` | Provider value `useMemo` memoize, useToast fallback 을 모듈 상수로 |
| `frontend/src/app/checkout/CheckoutClient.tsx` | useRef 처리 플래그 추가 + useEffect deps 를 `[returnedPaymentId]` 만으로 축소 (사유 주석 + eslint-disable-next-line) |

## Toast.tsx 변경

`useMemo` import 추가 + Provider 미존재 fallback 모듈 상수화 + Provider value memoize:

```diff
- import {
-   createContext,
-   useCallback,
-   useContext,
-   useEffect,
-   useState,
-   type ReactNode,
- } from "react";
+ import {
+   createContext,
+   useCallback,
+   useContext,
+   useEffect,
+   useMemo,
+   useState,
+   type ReactNode,
+ } from "react";

  ...

+ // Provider 미존재 시 useToast() 가 반환할 no-op — 모듈 단위 상수라 매 호출 같은 reference.
+ // (전엔 매 호출 새 객체였어서 useToast 결과를 useEffect deps 에 넣은 곳이 무한 재실행됨.)
+ const NOOP_TOAST: ToastContextValue = { show: () => {} };

  export function useToast(): ToastContextValue {
    const ctx = useContext(ToastContext);
-   if (!ctx) {
-     // Provider 없이 호출된 경우에도 크래시 없이 no-op
-     return { show: () => {} };
-   }
-   return ctx;
+   return ctx ?? NOOP_TOAST;
  }

  export function ToastProvider({ children }: { children: ReactNode }) {
    const [items, setItems] = useState<ToastItem[]>([]);

    const show = useCallback((message: string, kind: ToastKind = "info") => {
      const id = Date.now() + Math.random();
      setItems((prev) => [...prev, { id, message, kind }]);
      setTimeout(() => {
        setItems((prev) => prev.filter((it) => it.id !== id));
      }, 3000);
    }, []);

+   // Provider value 를 매 렌더 새 객체 리터럴로 만들면 useToast 결과가 unstable.
+   // useEffect deps 에 toast 가 들어간 곳(예: CheckoutClient redirectUrl 복귀 처리)이
+   // 무한 재실행되는 사고를 피하기 위해 memoize.
+   const value = useMemo<ToastContextValue>(() => ({ show }), [show]);

    return (
-     <ToastContext.Provider value={{ show }}>
+     <ToastContext.Provider value={value}>
        {children}
        ...
      </ToastContext.Provider>
    );
  }
```

## CheckoutClient.tsx 변경

`useRef` import + 처리 플래그 + deps 축소:

```diff
- import { Suspense, useEffect, useState } from "react";
+ import { Suspense, useEffect, useRef, useState } from "react";

  ...

  function CheckoutContent() {
    const router = useRouter();
    const toast = useToast();
    const searchParams = useSearchParams();
    const returnedPaymentId = searchParams.get("paymentId");
    ...

+   // 같은 paymentId 에 대해 verify 가 두 번 이상 안 돌아가게 한다.
+   // 모바일 redirectUrl 복귀 후 Toast/router 등 deps 가 흔들려도 1회만 처리.
+   const verifiedRef = useRef<string | null>(null);

    ...

    useEffect(() => {
      if (!returnedPaymentId) return;
+     if (verifiedRef.current === returnedPaymentId) return;
+     verifiedRef.current = returnedPaymentId;
      verifyPaymentById(returnedPaymentId)
        .then((result) => {
          toast.show(`${planLabel(result.plan)} 결제 완료`, "success");
          router.replace("/checkout");
          setTimeout(() => router.push("/mock-exams"), 800);
        })
        .catch((e) => {
          const message = e instanceof Error ? e.message : "";
          const cancelled = /취소|cancel/i.test(message);
          toast.show(
            cancelled ? "결제를 취소하셨습니다." : message || "결제 검증에 실패했습니다.",
            cancelled ? "info" : "error",
          );
          router.replace("/checkout");
          if (e instanceof Error) console.error("[checkout:return]", e);
        });
-   }, [returnedPaymentId, router, toast]);
+     // eslint-disable-next-line react-hooks/exhaustive-deps -- router/toast 는 stable hook 결과로
+     // 다뤄야 한다. deps 에 추가하면 매 렌더 effect 재실행 → verify 중복 호출 사고 재발.
+   }, [returnedPaymentId]);
```

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

### (옵션) 수동 검증

DevTools 모바일 모드 또는 실 모바일:
- /checkout → 신용카드 → 결제 → 외부 앱에서 취소 → **토스트 1회** ("결제를 취소하셨습니다" 또는 "결제 검증에 실패했습니다") + 페이지 안정.
- 동일 흐름으로 성공 → **토스트 1회** + /mock-exams 이동 1회.
- DevTools Network 탭: `/api/payment/verify` POST 1회.

데스크탑 회귀:
- 결제창 SDK Promise 흐름 그대로 → 토스트 1회.
- 카카오페이도 동일.

## Acceptance Criteria

1. `Toast.tsx` 의 Provider value 가 `useMemo` 로 memoize 됨.
2. `useToast()` 의 Provider 미존재 fallback 이 `NOOP_TOAST` 모듈 상수.
3. `CheckoutClient.tsx` 의 `verifiedRef` 가 신규 추가됨.
4. useEffect deps 가 `[returnedPaymentId]` 만이며 사유 주석 + `// eslint-disable-next-line react-hooks/exhaustive-deps` 포함.
5. `npm run lint` 0 errors.
6. `npm run build` 성공.

## 금지 사항

- `verifiedRef.current` 를 effect 안에서 `null` 로 되돌리지 마라. **이유**: 한 페이지 세션 안에서 같은 paymentId 를 재처리할 일이 없음. router.replace 가 쿼리를 정리하면 effect 가드 `if (!returnedPaymentId) return` 가 자연 차단.
- `router`, `toast` 를 deps 에 다시 넣지 마라. **이유**: Toast memoize 가 미래 PR 에서 깨지면 즉시 무한 루프 재발. ref 가드가 1차, deps 축소가 2차 방어.
- `verifyPaymentById` 호출을 `try/await` 로 바꾸지 마라. **이유**: useEffect 콜백을 async 로 만들면 cleanup 누락 위험. Promise chain 유지.
- AbortController 로 fetch 취소 도입하지 마라. **이유**: verify 는 멱등이라 abort 불필요. 변경 범위 최소화.
- Toast.tsx 의 ToastBubble/ToastIcon 등 다른 부분을 수정하지 마라. **이유**: 본 step 은 Provider value 안정화만.

## Status 규칙

- 성공: step 1 `completed`, summary "Toast Provider value useMemo + useToast fallback 모듈 상수 + CheckoutClient verifiedRef + deps 축소, lint/build OK".
- 실패: 3회 재시도 후 `error`.
