# Step 1 — Checkout PlanCard Supabase 스타일 + BuyerInfoModal 토큰 정합

## 배경

사용자 요청 2건:
1. `/checkout` PlanCard 의 디자인을 Supabase pricing UI(레퍼런스: `Desktop/1.png`) 톤으로 변경.
2. 카드 맨 위에 표시되는 단위 라벨("3일 / 30일 / 평생 / FREE") 제거.

현 카드는 첫 줄에 `tier.unit ?? "FREE"` 가 작은 모노스페이스 라벨로 떠 있어 위계가 어지럽고, hover 시 강한 amber 글로우/translate 가 `docs/UI_GUIDE.md` 의 AI 슬롭 안티패턴("과한 글로우 — 자격증 학습 서비스의 신뢰감을 해친다") 과도 충돌. Supabase 패턴인 "플랜명(+뱃지) → 가격 → CTA → 체크리스트" 순서로 재배치하고 글로우/이동 효과를 톤다운.

함께 사용자가 BuyerInfoModal 도 카드와 톤 통일을 원해 미세 일관성 조정 결정. 모달은 이미 토큰 기반이지만 일부 비표준 토큰(`text-foreground`, `text-muted`, `text-error`) 정합과 포커스 링 통일이 필요.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/components/billing/CheckoutLanding.tsx` | PlanCard 재정렬 (헤더/가격/CTA/features 순서, 단위 라벨·태그라인·Most Popular 핀 제거, hover 톤다운) |
| `frontend/src/components/billing/BuyerInfoModal.tsx` | 토큰 정합(`text-foreground`→`text-text`, `text-error`→`text-danger`, `text-muted`→`text-text-muted`) + 포커스 링 통일 + 헤더 무게 정돈 |

## A. CheckoutLanding.tsx (PlanCard 재정렬)

1. `PlanCard` 헤더에서 `tier.unit ?? "FREE"` span(현 line 254–260) 제거
2. `PlanCard` 헤더에서 `tier.tagline` p(현 line 263–265) 제거. `Tier` 타입의 `tagline` 필드는 데이터로 유지하되 렌더에서 빼냄
3. highlight 카드의 absolute `Most Popular` 핀(현 line 245–252) 제거하고 헤더 라인 인라인 뱃지로 이전
4. 헤더 라인: 플랜명을 `text-2xl font-bold tracking-tight` (highlight 시 amber 텍스트) + 우측 인라인 뱃지(`현재 플랜` 회색 / `가장 인기` amber)
5. 가격 → CTA 버튼 → 구분선 → features 순서로 재정렬
6. features `ul` 의 `flex-1` 제거(버튼이 더 이상 아래에 없음)
7. 카드 컨테이너 hover 효과 톤다운:
   - 일반 카드: hover translate/shadow 제거, border 색만 변경
   - highlight 카드: 정적 amber 그라데이션·약한 그림자만 유지, hover 글로우 강화 / translate 제거
8. 가격 라인의 `/ {tier.unit}` 표기는 유지(단위 정보가 헤더에서 빠진 만큼 가격 라인 가독성 강화)

## B. BuyerInfoModal.tsx (미세 일관성 조정)

1. 헤더 위계 정돈 — `text-lg` → `text-xl tracking-tight` 로 카드 헤더와 비슷한 무게감. subtitle 은 `text-xs text-text-muted` 유지.
2. 닫기 버튼 토큰 — `text-muted hover:text-foreground` → `text-text-subtle hover:text-text` 로 정합.
3. 안내 박스 amber 톤은 유지 — 카드 highlight(`가장 인기`) 액센트와 통일감.
4. 입력 필드 — `text-foreground` → `text-text`, focus 시 `border-primary` + `ring-1 ring-primary/30` 로 카드 CTA 톤과 일치.
5. 라벨 토큰 — `text-muted` → `text-text-muted` 로 통일.
6. 에러 메시지 토큰 — `text-error` → `text-danger` 로 통일(`docs/UI_GUIDE.md` 시맨틱 토큰).
7. 모달 컨테이너 `rounded-2xl border border-border bg-surface shadow-xl` 유지.
8. 버튼 영역 — 현재 outline 취소 + primary 결제 진행 구조 그대로(사용자는 "미세 조정" 만 선택, 구조 변경 X).

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

위 둘 통과만으로 자동 검증 완료. 시각 회귀는 사용자 수동 확인:

- `/checkout` 비로그인 → ComingSoonView 그대로
- `/checkout` 로그인 + 미구독 → 4개 카드 새 레이아웃, Free `현재 플랜` 인라인 뱃지, Pro `가장 인기` 인라인 뱃지
- `/checkout` 로그인 + Starter 활성 → Starter `✓ 이용 중`, Pro/Lifetime prorate 가격 표시
- hover 시 카드 떠오름·글로우 강화 없음
- 결제 버튼 클릭 → BuyerInfoModal 헤더/입력 필드 토큰 정합, focus 시 primary 보더+링, ESC/오버레이 닫기 회귀 없음

## Acceptance Criteria

1. `CheckoutLanding.tsx` PlanCard 가 위→아래 순서로 (헤더 라인 = 플랜명+인라인 뱃지) → (가격) → (CTA 버튼) → (구분선) → (features) 로 재정렬되어 있다.
2. 카드 첫 줄에 `tier.unit ?? "FREE"` 단위 라벨이 더 이상 렌더되지 않는다.
3. `tier.tagline` 텍스트가 카드 안에 렌더되지 않는다.
4. highlight 카드의 absolute `Most Popular` 핀(외부 떠 있는 형태)이 제거되고 인라인 뱃지로 통합되어 있다.
5. 일반 카드에 hover `-translate-y-*`, hover `shadow-*` 클래스가 없다.
6. highlight 카드의 hover 시 글로우 강화가 없다.
7. `BuyerInfoModal.tsx` 에 `text-foreground`, `text-muted`, `text-error` 토큰이 더 이상 등장하지 않고 각각 `text-text`, `text-text-muted`, `text-danger` 로 교체되어 있다.
8. BuyerInfoModal 입력 필드 focus 시 `border-primary` + `ring-1 ring-primary/30` 가 함께 적용된다.
9. `npm run lint` errors 0 (기존 warning 외 신규 회귀 없음).
10. `npm run build` ✓ Compiled successfully.
11. `planLabel` export, `BuyerInfoModal` 의 props 시그니처, `validateName/Email/Phone`, `handleSubmit`, ESC/scroll lock 동작 변경 없음.

## 금지 사항

- `planLabel` export 시그니처를 변경하지 마라. 이유: `BuyerInfoModal.tsx`, `profile/page.tsx` 에서 import 중.
- `CheckoutClient.tsx`, `payment.ts`, backend 결제 로직, `buyerStorage.ts` 를 건드리지 마라. 이유: 본 작업은 순수 UI 리디자인. 결제 처리는 직전 phase `payment-buyer-info-modal` 로 정리 완료.
- BuyerInfoModal 의 검증 로직(`validateName/Email/Phone`, `handleSubmit`, ESC/scroll lock, localStorage 자동 채움) 을 변경하지 마라. 이유: 사용자가 "미세 일관성 조정" 만 선택. 동작 변경은 범위 밖.
- BuyerInfoModal 안내 박스 amber 톤을 중립 톤으로 교체하지 마라. 이유: 사용자가 "모달 전반 재정리" 가 아닌 "미세 조정" 을 선택. amber 액센트는 카드 highlight 와 통일감을 만드는 핵심.
- amber 컬러 시스템을 emerald 등 다른 컬러로 교체하지 마라. 이유: amber 는 PASS+ 액센트(`--cert-sqld`) 로 일관 사용 중. 사용자는 글로우 톤다운만 요청.
- 페이지 상단 헤더/구독 상태 안내/결제수단 토글/하단 FAQ/약관 링크 영역은 수정하지 마라. 이유: 사용자 요청은 PlanCard + 모달 한정.
- `Tier` 타입에서 `tagline`, `unit` 필드를 삭제하지 마라. 이유: 렌더에서 제거하더라도 데이터 정의 보존 — 향후 재도입 또는 다른 컴포넌트에서 재사용 여지 유지.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "PlanCard 재정렬 + BuyerInfoModal 토큰 정합, lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: lint/build 가 통과 못 할 경우.
