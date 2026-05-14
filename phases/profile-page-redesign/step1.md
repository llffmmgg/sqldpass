# Step 1 — `/profile` 시각 톤 통일 + 정체성 헤더 + 위험구역 details

## 배경

`frontend/src/app/profile/page.tsx` (252줄, `"use client"`) 는 기능적으로는 동작하지만 다음 시각/UX 부채가 누적되어 있다:

1. **토큰 비표준 사용** — `bg-background` / `text-foreground` / `text-muted` / `text-red-400` / `text-green-400` / `border-rose-500/20` / `bg-rose-500/5` / `text-rose-300` 처럼 직접 Tailwind 색상이나 비표준 토큰을 사용. `docs/UI_GUIDE.md` 토큰 표준(`bg-bg`, `text-text`, `text-text-muted`, `text-danger`, `text-success`) 과 불일치하고, 특히 라이트 모드에서 위험구역 rose 톤이 어색하다.
2. **정체성 부재** — 누구의 프로필인지(닉네임/제공자/가입일) 한눈에 보이지 않는다. 회원정보 카드에 정보가 묶여 있고 닉네임 폼이 항상 펼쳐져 있어 입력 의도가 모호하다.
3. **위험구역이 항상 펼쳐짐** — `rose-500/20` 보더 + 안내문 + 버튼이 시각적 무게가 과해 정상 동선을 방해한다. `history/[id]` 의 `<details>` 디스클로저 패턴을 차용해 접힘 기본으로 한다.
4. **너비 협소** — `max-w-xl` 로 묶여 있어 KPI 카드/빠른 링크 row 등 step 2 에서 들어올 정보를 담기 부족. `Container size="narrow"` (max-w-3xl) 로 확장.

본 step 은 시각 톤 + 구조 리팩터만 다루고, 새 API/데이터 페치는 **건드리지 않는다**. 학습 스냅샷·빠른 링크 카드는 step 2 에서 추가한다.

레퍼런스 시각 톤: `frontend/src/app/history/[id]/page.tsx` (최근 Supabase 스타일 리디자인).

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/app/profile/page.tsx` | 정체성 헤더 + 닉네임 인라인 편집 + 위험구역 `<details>` + Container 너비 확장 + 색상 토큰 1:1 치환 (SubscriptionCard/FreePlanCard/FeatureChip 포함, 같은 파일 내) |

다른 파일은 수정하지 않는다.

## 구현

### A. 색상 토큰 1:1 치환 (CRITICAL — 색 계열 변경 금지)

`profile/page.tsx` 전체에서 다음 비표준/직접 Tailwind 토큰을 표준 CSS 토큰으로 치환한다. **색 계열은 바꾸지 않는다** (rose→danger 는 모두 빨강 계열, green-400→success 는 모두 녹색 계열).

| 현재 | → 변경 |
|---|---|
| `bg-background` | `bg-bg` |
| `text-foreground` | `text-text` |
| `text-foreground/60` | `text-text-muted` |
| `text-muted` | `text-text-muted` |
| `text-red-400` (에러) | `text-danger` |
| `text-green-400` (성공) | `text-success` |
| `text-rose-300` | `text-danger` |
| `text-rose-200` | `text-danger` |
| `text-rose-400` | `text-danger` |
| `border-rose-500/20`, `border-rose-500/40` | `border-danger/30`, `border-danger/40` |
| `bg-rose-500/5`, `bg-rose-500/10`, `bg-rose-500/20` | `bg-danger/[0.05]`, `bg-danger/10`, `bg-danger/20` |
| `bg-rose-500`, `hover:bg-rose-600` (탈퇴 확정 버튼) | `bg-danger`, `hover:bg-danger/90` |
| `focus:border-rose-500/60`, `focus:ring-rose-500/30` | `focus:border-danger/60`, `focus:ring-danger/30` |
| `text-primary-fg` (이미 토큰) | 그대로 |

primary/success/warning 토큰을 쓰는 SubscriptionCard 의 `bg-success/15 text-success`, `border-warning/40 bg-warning/[0.08] text-warning`, `border-primary/40 bg-primary/[0.08] text-primary` 등은 **그대로 유지**(이미 토큰화됨).

`text-text-subtle` 가 필요하면 FreePlanCard 의 `line-through text-text-subtle` 처럼 사용.

### B. 페이지 외곽

- 현재 `<main className="min-h-screen bg-background text-foreground">` + 내부 `<div className="mx-auto max-w-xl px-4 py-12">` 을 다음으로 교체:
  ```tsx
  <main className="min-h-screen bg-bg text-text">
    <Container size="narrow" className="py-10 md:py-12">
      {/* sections */}
    </Container>
  </main>
  ```
- `Container` 는 `@/components/ui/Container` 에서 default export. `size="narrow"` = `max-w-3xl`.
- `useRouter` 는 회원 탈퇴 성공 후 `router.replace("/")` 에서 필요하므로 import 유지. 하단 "← 돌아가기" 의 `router.back()` 은 명시적 `Link href="/dashboard"` 로 교체하고 `useRouter` 호출은 그대로.

### C. ① 정체성 헤더 (page 최상단, 기존 `<h1>프로필</h1>` 대체)

기존 `<h1 className="text-2xl font-bold">프로필</h1>` 한 줄을 다음 헤더 블록으로 교체:

```tsx
<header className="flex items-start justify-between gap-4">
  <div className="min-w-0">
    <p className="t-label text-text-subtle">프로필</p>
    <h1 className="mt-1 t-h1 truncate text-text">{me?.nickname ?? "—"}</h1>
    <p className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-text-muted">
      <span className="inline-flex items-center gap-1 rounded-md border border-border bg-bg-elevated px-1.5 py-0.5 font-medium">
        {me?.provider ?? ""}
      </span>
      {me?.createdAt && (
        <span>
          가입일 {new Date(me.createdAt).toLocaleDateString("ko-KR")}
        </span>
      )}
    </p>
  </div>
</header>
```

`me` 가 null 이면 헤더는 닉네임/제공자/가입일 칸을 표시하지 않거나 `—` 로. 기존 `if (!authChecked) loading` / `if (!isLoggedIn()) LoginRequired` 가드는 변경하지 않는다.

### D. ② 회원 정보 카드 → 닉네임 인라인 편집

기존 회원정보 카드(`mt-8 rounded-xl border border-border bg-surface p-6` + 내부 제공자/가입일 텍스트 + 폼) 를 다음으로 교체:

- 헤더는 step C 로 빠졌으므로 **제공자/가입일 줄은 카드에서 제거**(중복 방지).
- 카드 헤더 띠 추가 (`history/[id]` 패턴):
  - `border-b border-border bg-bg-elevated px-5 py-2.5` + 좌측 "닉네임" 라벨 (`t-label text-text-subtle`) + 우측 편집 토글 버튼.
- 본문(`px-5 py-4`) 에서 `editing` 토글:
  - `editing === false`: 닉네임 큰 글씨(`text-base font-medium text-text`) + 우측 "변경" 버튼(`<button>` → `setEditing(true); setInput(me.nickname)`).
  - `editing === true`: 입력 + 🎲 랜덤 + 저장 + 취소. 저장 성공 시 `setEditing(false)`. 폼 로직(`handleSubmit`, `handleRandomize`, error/success state) 은 그대로 재사용.
- `editing` state 새로 추가:
  ```ts
  const [editing, setEditing] = useState(false);
  ```
- 입력 필드와 버튼 클래스도 토큰화:
  - input: `flex-1 rounded-md border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30`
  - 🎲 버튼: `rounded-md border border-border bg-bg px-3 py-2 text-sm text-text-muted transition-colors hover:border-border-strong hover:text-text`
  - 저장 버튼: `rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-fg transition-colors hover:bg-primary-hover disabled:opacity-50`
  - 취소 버튼: `rounded-md border border-border bg-bg px-3 py-2 text-sm text-text-muted hover:text-text`
- 안내문 `2~30자, 다른 사용자와 중복 불가` 와 에러/성공 메시지는 유지하되 색만 토큰화 (`text-danger`, `text-success`).

### E. ③ 구독 카드 — 색상 토큰화만

`SubscriptionCard` / `FreePlanCard` / `FeatureChip` 함수는 **구조 변경 없음**. 다만 컨테이너 안에서 사용된 다음 토큰만 정리:

- `mt-8` 마진은 유지(섹션 간 간격). 부모가 `Container` 로 바뀌었어도 동일하게 동작.
- `bg-gradient-to-br from-primary/[0.08] via-bg to-bg` — `via-bg` `to-bg` 가 이미 토큰. 그대로.
- `border border-border bg-surface` — 그대로.
- `text-muted` → `text-text-muted` (FreePlanCard 안내문 / 만료일 텍스트).
- `bg-surface-hover` → 그대로(토큰).
- `text-text-subtle line-through` → 그대로.
- gradient CTA(`from-primary to-[#5ee0a5]`) 는 유지. `#5ee0a5` 는 amber 가 아니라 emerald 보조 톤이므로 색 계열 변경 아님.

### F. ④ 위험구역 → `<details>` 디스클로저

기존 박스(`mt-8 rounded-xl border border-rose-500/20 bg-rose-500/5 p-6` + h2 + p + 버튼) 를 다음으로 교체:

```tsx
<details className="group mt-8 overflow-hidden rounded-lg border border-border bg-surface transition-colors open:border-danger/30 open:bg-danger/[0.03]">
  <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-5 py-3 text-sm font-medium text-text-muted transition-colors hover:bg-surface-hover group-open:text-danger">
    <span className="inline-flex items-center gap-2">
      <span className="font-mono text-xs transition-transform duration-150 group-open:rotate-90">▸</span>
      계정 관리
    </span>
    <span className="t-label text-text-subtle group-open:hidden">위험 작업</span>
  </summary>
  <div className="border-t border-danger/20 bg-danger/[0.02] px-5 py-4">
    <h2 className="text-sm font-semibold text-danger">회원 탈퇴</h2>
    <p className="mt-1 text-xs text-text-muted">
      탈퇴 시 풀이 기록과 받은 알림이 모두 삭제되며 복구할 수 없습니다.
      작성한 건의사항은 운영을 위해 익명으로 보존됩니다.
    </p>
    <button
      type="button"
      onClick={() => { setWithdrawOpen(true); setWithdrawConfirm(""); setWithdrawError(null); }}
      className="mt-4 rounded-md border border-danger/40 bg-danger/10 px-4 py-2 text-sm font-medium text-danger transition-colors hover:bg-danger/20"
    >
      회원 탈퇴
    </button>
  </div>
</details>
```

탈퇴 모달(현 210~253줄) 은 구조 변경 없음. 모달 내 색상만 토큰화:

- `bg-black/70 backdrop-blur-sm` → 그대로 (모달 백드롭 표준).
- 모달 컨테이너 `bg-background` → `bg-bg`.
- `text-foreground` → `text-text`.
- `text-muted` → `text-text-muted`.
- `font-semibold text-rose-300` (확인 문구 강조) → `font-semibold text-danger`.
- input focus 토큰: `focus:border-rose-500/60 focus:ring-rose-500/30` → `focus:border-danger/60 focus:ring-danger/30`.
- 에러 메시지 `text-rose-400` → `text-danger`.
- 취소 버튼: `border-border bg-surface text-muted hover:text-foreground` → `border-border bg-surface text-text-muted hover:text-text`.
- 탈퇴 확정 버튼: `bg-rose-500 text-white hover:bg-rose-600` → `bg-danger text-white hover:bg-danger/90`.

### G. 하단 네비

기존 `router.back()` "← 돌아가기" 버튼을 `<Link href="/dashboard">` 로 교체:

```tsx
<div className="mt-8">
  <Link href="/dashboard" className="text-sm text-text-muted transition-colors hover:text-text">
    ← 대시보드로
  </Link>
</div>
```

`useRouter` 는 `router.replace("/")` 가 탈퇴 성공 핸들러에서 여전히 필요하므로 import/호출 유지.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 검증(`npm run dev`):

1. **비로그인** → `/profile` → `LoginRequired` 출력 (회귀 없음).
2. **로그인 + 무료 플랜** →
   - 헤더에 닉네임 / 제공자 칩(`Google` 등) / 가입일 표시.
   - 닉네임 카드는 "변경" 버튼만 보이고, 클릭 시 입력 + 🎲 + 저장 + 취소 노출. 저장 성공 시 헤더 닉네임 즉시 반영, 편집 모드 자동 해제.
   - `FreePlanCard` 정상 표시(이용권 보러가기 CTA).
   - 위험구역은 접힌 상태(`계정 관리` 한 줄)로 시작. 펼치면 안내 + 탈퇴 버튼.
3. **로그인 + 유료 활성** →
   - 활성 SubscriptionCard 정상, 기능 chip · 만료일.
4. **탈퇴 모달** → 펼친 상태에서 탈퇴 버튼 → 모달 → "탈퇴합니다" 입력 → POST 후 `/` 리다이렉트 (회귀 없음).
5. **라이트/다크 토글** → 모든 색상이 자연스럽게 따라옴 (history 카드와 톤 일치).
6. **반응형** 375 / 768 / 1280 폭 모두에서 헤더 잘림/카드 깨짐 없음.

## Acceptance Criteria

1. `profile/page.tsx` 내에 `bg-background`, `text-foreground`, `text-foreground/60`, `text-muted`, `text-red-400`, `text-green-400`, `text-rose-300`, `text-rose-200`, `text-rose-400`, `border-rose-500/*`, `bg-rose-500/*`, `focus:border-rose-500/*`, `focus:ring-rose-500/*`, `bg-rose-500`, `hover:bg-rose-600` 가 **하나도 남아있지 않다** (정규식 검색 0건).
2. 페이지 외곽이 `<Container size="narrow">` 로 감싸져 있다 (`max-w-3xl`).
3. 페이지 상단에 닉네임 + 제공자 칩 + 가입일을 표시하는 정체성 헤더가 있다.
4. 회원정보 카드는 닉네임 인라인 편집 패턴(보기 모드 / 편집 모드 토글) 으로 동작하고, "편집" 클릭 → 입력+버튼 노출, "취소" 클릭 → 보기 모드 복귀, 저장 성공 시 자동으로 보기 모드 복귀.
5. 회원정보 카드 안에서 제공자/가입일 텍스트가 더 이상 렌더되지 않는다(헤더로 이전).
6. 위험구역이 `<details>` 패턴으로 기본 접힘, summary 클릭 시 펼침, 펼친 상태에서 보더가 `danger/30` 으로 강조된다.
7. 하단 네비가 `router.back()` 버튼 대신 `<Link href="/dashboard">` 로 변경되었다.
8. 회원 탈퇴 모달의 흐름(WITHDRAW_PHRASE 입력 강제 / `handleWithdraw` / `withdrawMember()` 호출 / `clearAuth()` / `router.replace("/")`) 이 변경되지 않았다.
9. `useSubscription` 훅 사용 방식과 `SubscriptionCard` / `FreePlanCard` / `FeatureChip` 함수의 props 시그니처가 변경되지 않았다.
10. `npm run lint` errors 0 (기존 warning 외 신규 회귀 없음).
11. `npm run build` ✓ Compiled successfully.

## 금지 사항

- amber / rose / red / green / emerald 색 계열 자체를 다른 계열로 바꾸지 마라. 이유: 사용자가 `feedback_color_token_changes.md` 에 명시 — 색 계열 변경은 금지, opacity/shade 만 조정 허용. 본 step 의 모든 변경은 "직접 Tailwind 색 → 동일 의미의 CSS 토큰" 1:1 치환이다.
- `getMe`, `updateNickname`, `withdrawMember`, `useSubscription` API/훅 시그니처를 바꾸지 마라. 이유: 다른 페이지(NavBar, BuyerInfoModal 등) 가 동일 모듈을 import 중이며, 본 step 은 UI 리팩터.
- `WITHDRAW_PHRASE` "탈퇴합니다" 입력 강제 패턴을 우회/제거하지 마라. 이유: 회원 탈퇴는 비가역적 안전장치가 필수. 검증된 2단계 확인 UX.
- 새 데이터 페치(`getSolves`, `getWrongAnswerStats` 등) 를 추가하지 마라. 이유: 본 step 범위는 시각 톤 + 구조. 데이터 페치는 step 2 범위.
- `Container` 외 새 primitive 컴포넌트(`Card`, `Section`) 를 도입하지 마라. 이유: history/[id] 도 page.tsx 내 로컬 마크업으로 카드 패턴을 구성. 본 페이지도 같은 결로 유지해 비교 가능성을 확보.
- `globals.css`, `tailwind.config.*`, `frontend/src/components/ui/*` 를 수정하지 마라. 이유: 디자인 토큰/primitive 변경은 별도 작업. 본 step 은 페이지 단일 파일.
- `StreakBox` 컴포넌트 자체를 수정하지 마라. 이유: 다른 페이지에서도 사용 가능. 단, profile 페이지에서 사용을 step 2 에서 제거할 수 있음 — 그 결정은 step 2 범위.
- 마스코트/이모지를 헤더에 추가하지 마라. 이유: `docs/UI_GUIDE.md` 는 학습 도구 톤을 우선하며, 본 페이지 정체성 헤더는 텍스트만으로 깔끔하게 정돈한다.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "profile 시각 톤 통일 + 정체성 헤더 + 닉네임 인라인 편집 + 위험구역 details + Container 너비 확장, lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: lint/build 통과 못 할 경우.
