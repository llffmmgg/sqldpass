# Step 2 — 학습 스냅샷 카드 + 빠른 링크 카드 추가

## 배경

step 1 에서 `/profile` 의 시각 톤이 history 카드와 정합되고, 정체성 헤더 + 닉네임 인라인 편집 + 구독 카드 + 위험구역 disclosure 가 자리잡혔다.

남은 UX 부채는 **데드엔드** 와 **학습 컨텍스트 부재**:

- 프로필에 들어왔다가 다시 갈 곳이 "← 대시보드로" 한 줄뿐. 자주 가는 경로(오답 노트 / 대시보드 / 결제·청구) 빠른 링크가 없다.
- 사용자가 자신이 얼마나 학습했는지(총 풀이 · 정답률 · 연속일수) 한눈에 확인할 수 없다. 현재는 StreakBox 가 "연속일수" 만 표시. dashboard 까지 이동해야 전체가 보인다.

본 step 은 두 카드를 추가한다:

1. **학습 스냅샷 카드** — KPI 3열 (총 풀이 · 정답률 · 연속일수). dashboard 와 동일한 계산식 사용.
2. **빠른 링크 카드** — 오답 노트 / 대시보드 / 결제·청구 row 3개. 오답 row 우측에 미해결 오답 합계 배지.

데이터 페치 패턴은 dashboard 페이지(`frontend/src/app/dashboard/page.tsx`) 가 이미 사용 중인 동일한 API 와 동일한 계산식을 재사용한다. 백엔드 무변경, 새 API 추가 없음.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/app/profile/page.tsx` | useEffect 안에서 `getSolves()` + `getWrongAnswerStats()` + `getMyStreak()` 병렬 호출, 결과로 KPI 카드/빠른 링크 카드 추가. 기존 StreakBox 박스 제거(KPI 카드로 흡수). |

## 구현

### A. 데이터 페치

```ts
import { getSolves, getWrongAnswerStats } from "@/lib/api";
import { getMyStreak, type Streak } from "@/lib/streakApi";
```

state 추가 (다른 state 와 함께 컴포넌트 상단):

```ts
const [stats, setStats] = useState<{
  totalSolved: number;
  totalCorrect: number;
  overallRate: number; // 0~100, 풀이 0 이면 0
  wrongCount: number;
  streak: Streak | null;
} | null>(null);
const [statsLoading, setStatsLoading] = useState(true);
```

기존 `useEffect` (getMe 호출) 와는 **별도 useEffect** 로 분리해 통계 실패가 회원정보 로드에 영향 주지 않게 한다:

```ts
useEffect(() => {
  if (!isLoggedIn()) {
    setStatsLoading(false);
    return;
  }
  let cancelled = false;
  (async () => {
    try {
      const [solves, wrongStats, streak] = await Promise.all([
        getSolves(),
        getWrongAnswerStats(),
        getMyStreak().catch(() => null),
      ]);
      if (cancelled) return;
      const totalSolved = solves.reduce((acc, s) => acc + s.totalCount, 0);
      const totalCorrect = solves.reduce((acc, s) => acc + s.correctCount, 0);
      const overallRate = totalSolved > 0 ? Math.round((totalCorrect / totalSolved) * 100) : 0;
      const wrongCount = wrongStats.reduce((acc, w) => acc + (w.wrongCount ?? 0), 0);
      setStats({ totalSolved, totalCorrect, overallRate, wrongCount, streak });
    } catch {
      // 통계 실패 시 카드는 skeleton 으로 두고 무시 — 회원정보 로드는 영향 없음
    } finally {
      if (!cancelled) setStatsLoading(false);
    }
  })();
  return () => { cancelled = true; };
}, []);
```

`WrongAnswerStatsResponse` 의 필드 명이 `wrongCount` 와 다르면 실제 타입에 맞게 사용. 만약 `count` 등 다른 이름이면 그대로 합산.

### B. ② 학습 스냅샷 카드 — StreakBox 박스 대체

step 1 에서 정체성 헤더 다음에 위치하던 `<StreakBox />` 박스를 **삭제**하고, 그 자리에 다음 카드를 둔다:

```tsx
<section className="mt-6 overflow-hidden rounded-lg border border-border bg-surface">
  <div className="flex items-center justify-between gap-3 border-b border-border bg-bg-elevated px-5 py-2.5">
    <span className="t-label text-text-subtle">내 학습</span>
    <Link href="/dashboard" className="text-xs text-text-muted transition-colors hover:text-text">
      대시보드에서 자세히 →
    </Link>
  </div>
  <dl className="grid grid-cols-3 divide-x divide-border">
    <KpiCell label="총 풀이" value={stats?.totalSolved ?? null} suffix="문제" loading={statsLoading} />
    <KpiCell label="정답률" value={stats?.overallRate ?? null} suffix="%" loading={statsLoading} tone={rateTone(stats?.overallRate)} />
    <KpiCell label="연속 학습" value={stats?.streak?.currentStreak ?? null} suffix="일" loading={statsLoading} />
  </dl>
</section>
```

`KpiCell` 헬퍼 컴포넌트(page.tsx 하단에 로컬 함수로):

```tsx
function KpiCell({ label, value, suffix, loading, tone }: {
  label: string;
  value: number | null;
  suffix: string;
  loading: boolean;
  tone?: "success" | "warning" | "danger";
}) {
  const toneCls =
    tone === "success" ? "text-success"
    : tone === "warning" ? "text-warning"
    : tone === "danger" ? "text-danger"
    : "text-text";
  return (
    <div className="px-4 py-4 text-center">
      <p className="t-label text-text-subtle">{label}</p>
      <p className={`mt-1.5 font-mono text-2xl font-bold tabular-nums ${toneCls}`}>
        {loading ? "—" : value ?? 0}
        <span className="ml-0.5 text-xs font-medium text-text-muted">{suffix}</span>
      </p>
    </div>
  );
}

function rateTone(rate: number | null | undefined): "success" | "warning" | "danger" | undefined {
  if (rate == null) return undefined;
  if (rate >= 80) return "success";
  if (rate >= 60) return "warning";
  return "danger";
}
```

`StreakBox` import / 사용을 본 파일에서 제거한다 (다른 페이지에서는 그대로 유지).

### C. ④ 빠른 링크 카드 (SubscriptionCard 와 위험구역 사이)

```tsx
<section className="mt-8 overflow-hidden rounded-lg border border-border bg-surface">
  <div className="border-b border-border bg-bg-elevated px-5 py-2.5">
    <span className="t-label text-text-subtle">바로가기</span>
  </div>
  <ul className="divide-y divide-border">
    <QuickLinkRow
      href="/wrong-answers"
      label="오답 노트"
      hint="틀린 문제를 모아 복습"
      badge={stats?.wrongCount && stats.wrongCount > 0 ? `${stats.wrongCount}개` : undefined}
      badgeTone="danger"
    />
    <QuickLinkRow
      href="/dashboard"
      label="대시보드"
      hint="과목별 약점과 학습 추이"
    />
    <QuickLinkRow
      href="/checkout"
      label="결제·청구"
      hint="이용권 관리 및 영수증"
    />
  </ul>
</section>
```

`QuickLinkRow` 헬퍼 컴포넌트(page.tsx 하단):

```tsx
function QuickLinkRow({ href, label, hint, badge, badgeTone }: {
  href: string;
  label: string;
  hint: string;
  badge?: string;
  badgeTone?: "danger" | "primary";
}) {
  const badgeCls =
    badgeTone === "danger"
      ? "border-danger/30 bg-danger/10 text-danger"
      : "border-primary/30 bg-primary/10 text-primary";
  return (
    <li>
      <Link
        href={href}
        className="flex items-center justify-between gap-3 px-5 py-3 transition-colors hover:bg-surface-hover"
      >
        <div className="min-w-0">
          <p className="text-sm font-medium text-text">{label}</p>
          <p className="mt-0.5 text-xs text-text-muted">{hint}</p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          {badge && (
            <span className={`rounded-md border px-1.5 py-0.5 text-[10px] font-semibold ${badgeCls}`}>
              {badge}
            </span>
          )}
          <span className="text-text-subtle">›</span>
        </div>
      </Link>
    </li>
  );
}
```

### D. 페이지 섹션 순서 (최종)

```
Container size="narrow"
│
├─ 정체성 헤더 (step 1)
├─ 학습 스냅샷 카드 (step 2 — StreakBox 박스 대체)
├─ 닉네임 카드 (step 1 — 인라인 편집)
├─ 구독 카드 (step 1 — 색상 토큰화만)
├─ 빠른 링크 카드 (step 2 — 신규)
├─ 위험구역 details (step 1)
└─ 하단 "← 대시보드로" 링크 (step 1)
```

마진 일관성: 첫 섹션 `mt-6`, 이후 섹션 `mt-8`, 하단 네비 `mt-8`.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 검증(`npm run dev`):

1. **풀이 0 회원** → 학습 스냅샷 카드의 모든 KPI 가 0 으로 표시(스켈레톤 빠르게 사라짐). 정답률 색상은 기본(text-text).
2. **풀이 1+ 회원** → totalSolved, overallRate, currentStreak 가 올바르게 표시. 정답률에 따른 색상 분기(80+ success / 60+ warning / <60 danger) 가 적용된다.
3. **오답 보유** → 빠른 링크 "오답 노트" 우측에 `{wrongCount}개` 배지 (danger 톤) 표시. 오답 0개면 배지 없음.
4. **클릭** → 각 row 가 `/wrong-answers`, `/dashboard`, `/checkout` 로 정상 이동.
5. **API 실패** (네트워크 끊기) → 회원정보/구독은 정상이고, 학습 스냅샷만 `—` 로 fallback. 빠른 링크는 그대로 렌더(배지 미표시).
6. **라이트/다크 토글** → 색상 자연스럽게 전환.
7. **반응형** 375px 에서 KPI 3열 폭이 좁아도 깨지지 않는지(필요하면 `text-2xl` 대신 `text-xl`, 또는 `px-3` 으로 조정).

## Acceptance Criteria

1. `profile/page.tsx` 내에 `getSolves()`, `getWrongAnswerStats()`, `getMyStreak()` 가 호출되고, 결과로 `totalSolved`, `overallRate`, `wrongCount`, `streak.currentStreak` 값이 KPI 카드에 표시된다.
2. KPI 카드의 정답률 색상이 점수 구간(>=80 success / >=60 warning / <60 danger) 에 따라 시맨틱 토큰으로 분기된다.
3. KPI 카드 헤더에 "내 학습" 라벨 + "대시보드에서 자세히 →" 링크가 있다.
4. `<StreakBox />` 컴포넌트가 본 파일에서 import 되지 않고 사용되지 않는다 (다른 파일은 그대로).
5. 빠른 링크 카드에 오답 노트 / 대시보드 / 결제·청구 row 3개가 있다.
6. 오답 노트 row 우측에 `{wrongCount}개` 배지가 미해결 오답이 있을 때만 표시된다 (없으면 배지 없음).
7. 통계 데이터 페치 실패 시 회원정보/구독 UI 는 정상 동작하고 KPI 만 `—` fallback 으로 표시된다.
8. `useEffect` 가 dependency array `[]` 로 마운트 시 1회만 호출되고, cleanup 함수(`cancelled = true`) 가 unmount 시 race 를 차단한다.
9. `npm run lint` errors 0 (기존 warning 외 신규 회귀 없음).
10. `npm run build` ✓ Compiled successfully.

## 금지 사항

- 새 백엔드 API 를 만들거나 기존 API 시그니처를 바꾸지 마라. 이유: 본 step 은 프론트 전용. dashboard 가 이미 동일 API 로 동일 통계를 계산하므로 재사용한다.
- `StudyActivityChart`, dashboard 의 과목별 학습 카드 같은 무거운 컴포넌트를 profile 로 가져오지 마라. 이유: dashboard 와 profile 의 역할 분담 — profile 은 스냅샷 + 빠른 진입, 상세 분석은 dashboard. 중복 노출은 정보 위계를 흐린다.
- KPI 셀에 ring 게이지·차트·이모지를 추가하지 마라. 이유: `docs/UI_GUIDE.md` AI 슬롭 안티패턴 — "장식 시각 요소가 문제 풀이 집중도를 떨어뜨린다". 숫자 + 색상 분기만으로 충분.
- 빠른 링크 row 에 임의 신규 경로(`/mypage/feedback`, `/board` 등) 를 추가하지 마라. 이유: profile 의 자주 가는 경로 3개로 좁혔다(오답 노트 / 대시보드 / 결제·청구). 더 늘리면 데드엔드 해소 효과가 흐려진다. NavBar / FeedbackRail 이 이미 다른 진입을 제공.
- `getStreakDays` 같은 다른 통계 함수가 있더라도 dashboard 와 다른 식을 쓰지 마라. 이유: 두 화면의 KPI 가 다른 숫자를 보여주면 사용자 신뢰 손상.
- `getSolves()` / `getWrongAnswerStats()` 실패가 페이지 전체를 망가뜨리지 않도록 try/catch 로 감싸라. 이유: 회원정보 로드는 독립적이어야 하며, KPI 카드가 빈 fallback 으로 떨어져도 다른 카드는 정상 동작해야 한다.
- 색상 토큰을 step 1 에서 정리한 표준 외로 추가하지 마라. 이유: 일관성 유지. 새 시맨틱이 필요하면 디자인 토큰 작업으로 별도 처리.

## Status 규칙

- 성공: step 2 `completed`, summary 에 "학습 스냅샷 KPI 3열 + 빠른 링크 3-row 카드 추가(StreakBox 박스 대체), lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: `getWrongAnswerStats` 응답 필드명이 예상과 다르면 — 코드 확인 후 정확한 필드명으로 합산. 그래도 모호하면 사용자에게 확인.
