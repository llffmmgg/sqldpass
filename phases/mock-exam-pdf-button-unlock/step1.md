# Step 1 — MockExamPdfButton eligibility 게이트 제거 + 비로그인 클릭 가드

## 배경

`MockExamPdfButton` 이 마운트 시 `/api/mock-exams/pdf/eligibility` 를 조회해 `eligible=false` 면 `null` 을 리턴하도록 막혀 있어, 비로그인/미구독 사용자에게 버튼 자체가 보이지 않는다. 결과적으로 결제 유도 동선이 깨지고 사용자가 PDF 다운로드 기능의 존재를 인지하지 못한다.

이번 변경의 의도: 버튼을 누구에게나 노출하고, 클릭 시점에만 분기 처리한다. 토스트/안내 문구는 **사이트의 기존 가드 패턴을 그대로 재사용**해 PDF 버튼만 다른 UX가 되지 않게 한다.

## 사이트 기존 가드 패턴 (재사용 대상)

- **버튼 클릭 시 비로그인 차단**: `frontend/src/components/BookmarkButton.tsx:56` 의 `alert("로그인 후 이용 가능합니다.")` 가 표준. 클릭 시점에 강제 리다이렉트하는 패턴은 사이트에 없다. `<LoginRequired />` 컴포넌트는 페이지 전체 가드용이라 부적합.
- **미구독 차단**: 백엔드 에러 코드(`PDF_REQUIRES_SUBSCRIPTION`) 를 토스트로 노출하는 방식이 표준. 이미 `MockExamPdfButton.tsx:49-53` 에 구현돼 있어 변경 없음.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/components/MockExamPdfButton.tsx` | eligibility 게이트 제거 + 클릭 핸들러에 비로그인 가드(alert) 추가 |

## 백엔드 상태

수정 없음. `/api/mock-exams/{id}/pdf/download` 의 `subscriptionService.allowsPdf` 권한 체크는 그대로 유지 — 실제 PDF 보호는 API 레벨이 책임진다.

## MockExamPdfButton.tsx 변경

**제거**
- `useEffect` 안의 `getPdfEligibility()` 호출, `eligible` state, `if (!eligible) return null;` (L26–L39)
- 더 이상 호출되지 않는 `getPdfEligibility` import

**유지**
- `isLoggedIn` import (클릭 핸들러에서 비로그인 분기용)
- `downloadMockExamPdfAsUser` / `PdfDownloadError` import
- 기존 `PDF_REQUIRES_SUBSCRIPTION` 토스트 분기 + 일반 에러 토스트

**추가**
- 클릭 핸들러 최상단에:
  ```ts
  if (!isLoggedIn()) {
    alert("로그인 후 이용 가능합니다.");
    return;
  }
  ```
  `BookmarkButton.tsx:56` 와 동일한 패턴/문구. `useRouter`/리다이렉트 추가 안 함.

**최종 분기 흐름**
1. 비로그인 클릭 → `alert("로그인 후 이용 가능합니다.")` (BookmarkButton 컨벤션)
2. 로그인 + 미구독 → 백엔드 `PDF_REQUIRES_SUBSCRIPTION` → 기존 토스트
3. 로그인 + 권한 있음 → 정상 다운로드
4. 기타 에러 → 기존 일반 에러 토스트

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 확인 (`npm run dev` 후 `/mock-exams/{id}` 진입):
1. 비로그인 → 풀이/결과/이전시도 3 위치 모두에서 버튼 노출 → 클릭 시 alert
2. 로그인 + 미구독 → 버튼 노출 → 클릭 시 기존 "Lifetime 플랜 전용..." 토스트
3. 로그인 + 구독자/화이트리스트 → 버튼 노출 → 클릭 시 PDF 다운로드 (한글 파일명)
4. DevTools Network 에서 비로그인 시 `/api/mock-exams/pdf/eligibility` 호출이 사라졌는지 확인

## Acceptance Criteria

1. `MockExamPdfButton` 이 마운트 시 eligibility 를 호출하지 않고 항상 버튼을 렌더링.
2. 비로그인 클릭 → `alert("로그인 후 이용 가능합니다.")` (BookmarkButton 문구와 정확히 일치).
3. 미구독 클릭 → 기존 `PDF_REQUIRES_SUBSCRIPTION` 토스트 그대로 동작.
4. `npm run lint`, `npm run build` 통과.

## 금지 사항

- `frontend/src/lib/payment.ts` 의 `getPdfEligibility` 함수 정의를 지우지 마라. **이유**: 백엔드 엔드포인트가 살아 있고 미래에 "다운로드 가능 여부 뱃지" 같은 용도로 재사용 여지가 있음. dead code 정리는 별도 작업.
- 백엔드 `/api/mock-exams/pdf/eligibility` 또는 `/pdf/download` 권한 정책을 변경하지 마라. **이유**: 실제 PDF 보호는 API 레벨이 담당. 본 작업은 클라이언트 노출만 푸는 것.
- 새 토스트 문구를 만들지 마라. **이유**: PDF 버튼만 다른 UX가 되면 사이트 일관성이 깨진다. 비로그인 alert 문구는 `BookmarkButton` 과 동일하게 유지.
- `useRouter` 로 비로그인 사용자를 `/login` 으로 강제 리다이렉트하지 마라. **이유**: 사이트의 다른 버튼 가드는 클릭 시점에 페이지 이동을 강제하지 않음. alert 만으로 사용자가 자율적으로 로그인하도록 둔다.
- 호출 측 `frontend/src/app/mock-exams/[id]/page.tsx` (L245/L314/L562) 를 수정하지 마라. **이유**: 컴포넌트 시그니처 변경 없음.

## Status 규칙

- 성공: step 1 `completed`, summary "MockExamPdfButton eligibility 게이트 제거 + 비로그인 클릭 alert 가드 추가, lint/build OK".
- 실패: 3회 재시도 후 `error`.
