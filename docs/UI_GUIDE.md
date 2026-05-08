# UI 디자인 가이드

## 디자인 원칙

1. 학습 도구처럼 보여야 한다. 마케팅 페이지보다 문제 풀이·복습·상태 확인이 빠른 화면을 우선한다.
2. 정보 밀도를 유지하되, 문제·해설·오답 상태가 명확히 구분되어야 한다.
3. 모바일과 데스크톱 모두에서 풀이 흐름이 끊기지 않아야 한다.
4. 라이트·다크 모드 모두에서 WCAG AA(4.5:1) 대비를 만족한다.

## AI 슬롭 안티패턴

| 금지 사항 | 이유 |
|-----------|------|
| 의미 없는 gradient orb 또는 blur 장식 | 학습 화면의 정보 집중도를 떨어뜨린다 |
| 모든 섹션을 큰 카드로 감싸기 | 대시보드와 문제 풀이 화면의 밀도가 낮아진다 |
| 과한 글로우, 네온, 보라색 AI 팔레트 | 자격증 학습 서비스의 신뢰감을 해친다 |
| 사용법을 장황하게 설명하는 인앱 텍스트 | 반복 사용 흐름을 방해한다 |
| 버튼 텍스트가 모바일에서 줄 밖으로 넘침 | 문제 풀이 중 조작 실수를 만든다 |

## 디자인 토큰

`frontend/src/app/globals.css`의 CSS 변수 + `@theme inline` 매핑이 단일 진실. 다크 모드는 `:root.dark`로 분기되며 `lib/theme`이 토글한다. 인라인 `<script>`로 FOUC 방지.

### 표면 (Surface)

| 토큰 | 라이트 | 다크 | 용도 |
|------|--------|------|------|
| `--bg` | `#ffffff` | `#1a1a1a` | 페이지 기본 배경 |
| `--surface` | `#f7f8fa` | `#262626` | 카드 배경 |
| `--bg-elevated` | `#f0f1f3` | `#262626` | 카드 안의 강조 박스 |
| `--surface-hover` | `#eef0f3` | `#2d2d2d` | 카드 hover |
| `--border` | `#d0d3d9` | `#3e3e3e` | 또렷한 솔리드 보더 |
| `--border-strong` | `#a8acb6` | `#525252` | 호버·강조 보더 |

### 텍스트

| 토큰 | 라이트 | 다크 |
|------|--------|------|
| `--text` | `#181a1f` | `#eff1f6` |
| `--text-muted` | `#4a505a` | `#a1a1aa` |
| `--text-subtle` | `#5e636e` | `#8a8e98` |

### 브랜드 (Primary = Emerald)

| 토큰 | 라이트 | 다크 |
|------|--------|------|
| `--primary` | `#24b47e` | `#3ecf8e` |
| `--primary-hover` | `#1fa374` | `#4fd6a0` |
| `--primary-fg` | `#ffffff` | `#0a0a0a` |
| `--primary-soft` | `rgba(36,180,126,0.10)` | `rgba(62,207,142,0.12)` |
| `--primary-ring` | `rgba(36,180,126,0.35)` | `rgba(62,207,142,0.40)` |

### 시맨틱

| 토큰 | 라이트 | 다크 | 용도 |
|------|--------|------|------|
| `--danger` | `#ef4444` | `#f63737` | 오답·에러·삭제 |
| `--warning` | `#d97706` | `#ffb800` | 경고·주의 |
| `--success` | `#00997a` | `#00b8a3` | 정답·완료 |
| `--info` | `#0a84ff` | `#0a84ff` | 정보·진행 |

### 자격증 액센트 (라이트·다크 동일)

| 자격증 | 토큰 | 색 |
|--------|------|----|
| SQLD | `--cert-sqld` | `#f59e0b` (amber) |
| 정처기 실기 | `--cert-engineer-practical` | `#2dbb7a` (green) |
| 정처기 필기 | `--cert-engineer-written` | `#f43f5e` (rose) |
| 컴활 1급 | `--cert-cl1` | `#0ea5e9` (sky) |
| 컴활 2급 | `--cert-cl2` | `#6366f1` (indigo) |
| ADsP | `--cert-adsp` | `#14b8a6` (teal) |

라우트별 시그니처 색은 자격증 액센트로 통일. 자격증 분기 컴포넌트(예: `CertChips`, `CategoryHero`)는 이 토큰만 사용한다.

## 타이포그래피

### 폰트 스택

- 본문: **Noto Sans KR** (400 / 500 / 600 / 700) — `--font-sans-kr`
- 코드: **JetBrains Mono** — `--font-jetbrains-mono`
- 액센트/디스플레이: **Caveat** — `--font-caveat`

### 스케일 (모바일 → md 이상)

| 클래스 | 모바일 | md+ | 비고 |
|--------|--------|-----|------|
| `.t-display` | 2.5rem / 1.05 | 4rem / 1 | 랜딩·CTA 헤드라인, 700 weight |
| `.t-h1` | 1.875rem / 1.15 | 2.25rem | 페이지 제목, 700 weight |
| `.t-h2` | 1.5rem / 1.2 | 1.875rem | 섹션 제목, 600 weight |
| `.t-h3` | 1.125rem / 1.35 | 1.25rem | 600 weight |
| `.t-body-lg` | 1rem / 1.7 | 1.125rem | 긴 본문(블로그 등) |
| `.t-body` | 0.875rem / 1.6 | 1rem | 일반 본문 |
| `.t-caption` | 0.75rem | — | 메타·날짜 (text-muted 자동 적용) |
| `.t-label` | 11px / 600 / uppercase / 0.08em | — | 태그·라벨 |

## 컴포넌트 Primitive

위치: `frontend/src/components/ui/`

| 파일 | 역할 |
|------|------|
| `Button.tsx` | 기본 버튼 + `ButtonLink` variant |
| `Card.tsx` | header / body / footer anatomy |
| `Badge.tsx` | 상태·태그 칩 |
| `Container.tsx` | 3-size 그리드 컨테이너 |
| `Section.tsx` | 페이지 섹션 |
| `cn.ts` | `clsx` 머지 헬퍼 |

새 primitive를 추가할 때는 이 폴더에 둔다. 도메인 컴포넌트(`components/exam/`, `components/blog/`, `components/admin/` 등)는 primitive 위에 쌓는다.

### 카드

```text
rounded-lg border bg-surface
```

- 카드는 문제·요약·반복 리스트처럼 명확히 묶이는 단위에만 사용한다.
- 페이지 섹션 전체를 카드 안에 다시 넣지 않는다.

### 버튼

```text
Primary: 명확한 주요 행동 1개 (bg-primary text-primary-fg)
Secondary: 부가 행동 (border + text-text)
Icon: 반복 도구·닫기·이동·저장
```

- 문제 풀이 화면에서는 다음 문제·제출·해설 보기 같은 핵심 행동을 가장 빠르게 찾을 수 있어야 한다.
- 아이콘만 있는 버튼에는 접근 가능한 이름(`aria-label`)을 제공한다.

### 입력 필드

```text
rounded-md border border-border px-3 py-2 bg-bg
```

- 관리자·검색 화면에서는 라벨, 에러 메시지, 로딩 상태를 명확히 표시한다.

## 레이아웃

- 문제 풀이 화면은 본문 읽기 폭을 과하게 넓히지 않는다 (`Container` md 사이즈).
- 대시보드와 목록 화면은 스캔 가능한 표·리스트·탭·필터를 우선한다.
- 모바일에서는 `BottomTabBar`와 주요 행동 버튼이 화면 밖으로 밀리지 않도록 한다.

## 다크 모드

- `data-theme` 속성 + `:root.dark` CSS 변수 분기.
- 인라인 `<script>`로 첫 페인트 전 테마 적용 (`localStorage.theme` 또는 `prefers-color-scheme`).
- 컴포넌트는 토큰만 사용하고 직접 hex를 박지 않는다 — 다크 모드에서 자동으로 따라온다.

## 애니메이션

- 허용: 짧은 hover/focus 상태 변화, 로딩 스피너, 탭/아코디언 전환
- 금지: 문제 풀이 집중을 방해하는 장식 애니메이션

## 아이콘

- 기존 아이콘 라이브러리 또는 프로젝트 내 공통 패턴을 우선한다.
- 마스코트 컴포넌트(`MascotImage`, `MascotEmpty`, `MascotSpinner`)는 빈 상태·로딩에서 일관되게 사용한다.
- 반복 액션에는 텍스트 버튼보다 익숙한 아이콘과 툴팁을 쓴다.
