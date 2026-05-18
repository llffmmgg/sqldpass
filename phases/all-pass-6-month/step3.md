# Step 3 — 프론트엔드 카피 + CD 환경변수 정합

## 배경

Step 1·2 의 사용자 노출 톤(평생 → 6개월) 정합을 마무리한다. 프론트엔드 체크아웃 카드 카피와 plan-tokens 의 짧은 표기, 그리고 CD 워크플로의 `PAYMENT_UNLIMITED_NAME` 기본값을 정리한다. 가격·카드 라벨·SKU 는 손대지 않는다.

## 작업 디렉터리

```
frontend/
.github/workflows/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/components/billing/CheckoutLanding.tsx` (L103-120) | UNLIMITED 카드 `pitch` "평생 사용" 문구 |
| `frontend/src/lib/plan-tokens.ts` (L16) | `UNLIMITED.short` "무기한" → "6개월" |
| `.github/workflows/cd.yml` (L221) | `PAYMENT_UNLIMITED_NAME` 기본값 — application.yaml 와 일치 |

## CheckoutLanding.tsx 변경 diff

```diff
   {
     key: "UNLIMITED",
     name: "All Pass",
-    tagline: "한 번 결제로 계속 이용",
+    tagline: "6개월 풀 액세스",
     price: 29900,
     originalPrice: 39900,
-    pitch: "한 번 결제로 새 회차와 모의고사 PDF 다운로드까지 평생 사용하세요.",
+    pitch: "6개월 동안 새 회차와 모의고사 PDF 다운로드까지 무제한으로 사용하세요.",
     features: [
       { text: "PASS+ 회차 무제한" },
-      { text: "무제한 풀 엑세스" },
+      { text: "6개월 풀 액세스" },
       { text: "광고 제거" },
       { text: "오답노트 사용" },
       { text: "즐겨찾기 무제한" },
       { text: "PDF 다운로드" },
       ...FREE_BASELINE,
     ],
     cta: "All Pass 시작",
   },
```

- `tagline`, `pitch`, 두 번째 feature 의 "엑세스" 오타도 함께 "액세스" 로 정정 (사용자 노출 톤 일관성).
- `cta = "All Pass 시작"` 유지.

## plan-tokens.ts 변경 diff

```diff
-  UNLIMITED: { label: "All Pass", short: "무기한", bar: "bg-emerald-500", dot: "bg-emerald-500", text: "text-emerald-300" },
+  UNLIMITED: { label: "All Pass", short: "6개월", bar: "bg-emerald-500", dot: "bg-emerald-500", text: "text-emerald-300" },
```

- color 토큰(emerald-500/300) 은 그대로. **이유**: 사용자 명시 없이 색 계열 변경 금지.

## cd.yml 변경 diff

```diff
-              -e PAYMENT_UNLIMITED_NAME="${PAYMENT_UNLIMITED_NAME:-문어CBT 평생 무제한 이용권}" \
+              -e PAYMENT_UNLIMITED_NAME="${PAYMENT_UNLIMITED_NAME:-문어CBT All Pass}" \
```

application.yaml 의 `PAYMENT_UNLIMITED_NAME` 기본값(`문어CBT All Pass`) 과 일치시키는 잔재 정리. env 미설정 운영 환경에서 영수증 상품명이 통일된다.

본 step 에서 cd.yml 의 PAYMENT_3DAY_NAME / PAYMENT_1MONTH_NAME 의 기존 잔재(`문어CBT 3일 이용권` / `한달 이용권`) 는 application.yaml 와도 불일치하지만 **본 plan 범위 밖** 이므로 손대지 않음.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

## Acceptance Criteria

1. CheckoutLanding 의 UNLIMITED 카드 카피가 "6개월" 톤으로 일관 (`tagline`, `pitch`, feature)
2. `plan-tokens.ts` 의 `UNLIMITED.short = "6개월"`
3. `cd.yml` 의 `PAYMENT_UNLIMITED_NAME` 기본값 = `문어CBT All Pass`
4. `npm run lint`, `npm run build` 모두 통과

## 금지 사항

- 가격(29900) / `originalPrice` / `cta` / `name` 을 변경하지 마라. **이유**: 사용자 결정 — 가격·라벨 유지.
- color 클래스(`bg-emerald-500` 등) 의 계열을 변경하지 마라. **이유**: 색 계열 변경은 사용자 명시 시에만.
- CD 의 다른 PAYMENT_*_NAME 잔재(`3일 이용권`/`한달 이용권`) 를 같이 정리하지 마라. **이유**: 본 plan 범위 밖. 별도 phase 에서 통일.
- profile/page.tsx 의 `isLifetime = subscription.expiresAt === null` 분기를 변경하지 마라. **이유**: 이 분기는 *DB expires_at* 기반이라 기존 평생 구독자에게 자동으로 올바른 표시를 제공. 신규 6개월 구매자는 자연스럽게 만료일이 표시됨.

## Status 규칙

- 성공: step 3 `completed`, summary "CheckoutLanding/plan-tokens/cd.yml 6개월 톤 갱신, lint+build OK".
- 실패: 3회 재시도 후 `error`.
