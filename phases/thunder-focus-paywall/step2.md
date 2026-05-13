# Step 2 — 프론트엔드: Thunder/Focus 카드 + 오답노트 잠금 뷰 + 즐겨찾기 30개 배너

## 배경

Step 1 의 백엔드 변경(SubscriptionPlan.FOCUS / hasLibraryAccess / Thunder product-name / preview 엔드포인트 / 즐겨찾기 30 limit) 을 프론트엔드 UI 에 반영.

이전 step 산출물:
- `/api/wrong-answers/preview?limit=5` — 권한 무관, 본인 오답 상위 5개 (제목·과목만)
- `ActiveSubscription` 응답에 `hasLibraryAccess: boolean` 신규 필드
- product-name "문어CBT Thunder" / "문어CBT Focus"

## 작업 디렉터리

```
frontend/
```

## 변경 대상

### 1. 타입 + 라이브러리

`frontend/src/lib/payment.ts`:
- `SubscriptionPlan` 유니온 타입에 `"FOCUS"` 추가
- `ActiveSubscription` 인터페이스에 `hasLibraryAccess: boolean` 추가
- Play Billing SKU 매핑에 `FOCUS: "iap_focus"` 추가
- `planLabel()`: `"THREE_DAY" → "Thunder"` (기존 "Starter"), `"FOCUS" → "Focus"`

### 2. `/checkout` Thunder 리브랜드 + Focus 카드

`frontend/src/components/billing/CheckoutLanding.tsx`:
- 기존 THREE_DAY 카드 → Thunder 로 리브랜드:
  - `name: "Starter"` → `"Thunder"`
  - `tagline`: "벼락치기 3일 풀파워"
  - `features` 리스트에 광고 제거 + 오답노트 + 즐겨찾기 무제한 항목 추가
- TIERS 배열에 FOCUS 항목 (Thunder 와 Pro 사이):
  ```ts
  {
    key: "FOCUS",
    name: "Focus",
    tagline: "일상 학습 30일 집중",
    price: 2900,
    unit: "30일",
    features: [
      { text: "광고 제거" },
      { text: "오답노트 사용" },
      { text: "즐겨찾기 무제한" },
      { text: "PASS+ 모의고사는 Pro 이상" },
    ],
    cta: "Focus 시작",
  }
  ```
- 그리드 4 카드 — 데스크탑 4열 / 태블릿 2열 / 모바일 1열. 기존 Pro "가장 인기" 배지 유지.

`frontend/src/app/checkout/CheckoutClient.tsx`:
- `previews` state 초기값에 `FOCUS: null` 추가
- prefetch 루프 `["THREE_DAY", "ONE_MONTH", "UNLIMITED"]` 에 `"FOCUS"` 추가

### 3. 오답노트 페이지 잠금 뷰

`frontend/src/app/wrong-answers/page.tsx` (또는 client 컴포넌트):
- `useSubscription()` 으로 `hasLibraryAccess` 확인
- false 면 잠금 뷰 렌더:
  - **상단 안내**: "오답노트는 Thunder · Focus · Pro · Lifetime 에서 사용할 수 있어요" + 가치 카피
  - **가운데 블러 미리보기**: `/api/wrong-answers/preview?limit=5` 호출 → 본인 오답 상위 5개를 카드로 렌더 + CSS `filter: blur(8px) saturate(0.7); pointer-events: none;` + `user-select: none` 으로 상호작용 차단. 카드 내용은 제목/과목만.
  - 오답 0개: 미리보기 대신 "아직 풀이 기록이 없어요. 시험 한 회차 풀고 다시 오세요" 안내
  - **하단 CTA 두 개**: "Focus 2,900원으로 30일 시작" + "Thunder 3,900원으로 3일 풀파워" → `/checkout`
- true 면 기존 화면 그대로
- 본 엔드포인트가 403 (`WRONG_ANSWER_REQUIRES_SUBSCRIPTION`) 반환 시 같은 잠금 뷰로 폴백 (방어 코드)

`frontend/src/lib/api.ts` 또는 동등 위치:
- `getWrongAnswersPreview(limit?: number): Promise<...>` 함수 추가
- 응답 타입 `WrongAnswerPreview` 정의 (questionId, questionContent, subjectName)

### 4. 즐겨찾기 30개 잠금 배너

위치: 즐겨찾기 모음 화면 (grep 으로 확정 — 프로필 페이지 섹션일 가능성, 또는 별도 라우트).
- `useSubscription()` 의 `hasLibraryAccess` + 응답의 `limited` 메타로 분기
- `!hasLibraryAccess && limited` 일 때 리스트 하단에 배너:
  ```tsx
  <div className="rounded-xl border border-primary/30 bg-surface/70 p-4 text-sm">
    즐겨찾기는 무료로 최근 30개까지 보여요.
    <Link href="/checkout" className="ml-1 text-primary underline">
      Focus 2,900원으로 전체 보기 →
    </Link>
  </div>
  ```
- `BookmarkButton.tsx` 자체는 무변경 — 31번째 토글 추가 가능

### 5. 회귀 검토 위치

- NavBar / 프로필의 오답노트 진입 링크는 그대로 노출 — 잠금 뷰가 CTA 역할
- planLabel 이 노출되는 모든 위치 ("Starter" 텍스트 grep) 가 자동으로 "Thunder" 로 갱신되는지 확인. 하드코딩된 "Starter" 가 있으면 추가 보정.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 확인 (`npm run dev`):
1. `/checkout` 4 카드 노출 (Thunder · Focus · Pro · Lifetime) — 라벨/가격/features 정합
2. 무료 사용자가 `/wrong-answers` 진입 → 잠금 뷰 + 본인 오답 5개 블러 미리보기 + Focus·Thunder CTA
3. Focus 결제 → SPA 라우팅으로 `/mock-exams` 자동 이동 후 광고 즉시 사라짐
4. Focus 결제 후 `/wrong-answers` 진입 → 정상 오답노트
5. Thunder 결제 → 광고 사라짐 + 오답노트 사용 가능 + PASS+ 풀이 가능 + 즐겨찾기 무제한
6. 무료 사용자 즐겨찾기 31개 등록 후 모음 화면에서 30개만 + 배너
7. 결제 영수증/order_name 에 "문어CBT Thunder" / "문어CBT Focus" 표시

## Acceptance Criteria

1. `SubscriptionPlan` 타입에 `"FOCUS"` 추가 + `ActiveSubscription.hasLibraryAccess` 노출.
2. `/checkout` 에 Thunder 리브랜드 + Focus 카드 노출 (4 카드).
3. 오답노트 화면 무권한 사용자에게 블러 미리보기 + CTA 잠금 뷰.
4. 즐겨찾기 모음 화면 무료 사용자에게 30개 + 배너.
5. `npm run lint`, `npm run build` 통과.

## 금지 사항

- `MockExamPdfButton`, `useSubscription` 의 `invalidateSubscriptionCache()` 로직을 변경하지 마라. **이유**: 직전 phase 결과 그대로 재사용. Focus 결제 후 즉시 권한 반영 자동.
- `BookmarkButton` 토글 자체에 30 가드를 넣지 마라. **이유**: 백엔드도 31번째 추가 차단 안 하고 표시만 잘림. 사용자 데이터 보존 정책 위반.
- planLabel 의 "THREE_DAY" → "Thunder" 외에 enum 키 자체를 바꾸지 마라. **이유**: 백엔드 호환성.
- 오답노트 페이지에서 비로그인 사용자에게는 `LoginRequired` 같은 기존 페이지 가드로 처리 (잠금 뷰는 로그인 사용자 대상).

## Status 규칙

- 성공: step 2 `completed`, summary "Thunder 리브랜드 + Focus 카드 + 오답노트 블러 잠금 뷰 + 즐겨찾기 30개 배너, lint/build OK".
- 실패: 3회 재시도 후 `error`.
