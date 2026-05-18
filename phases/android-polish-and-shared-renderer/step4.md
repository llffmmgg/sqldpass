# Step 4 — Card/Button/Chip/IconButton 치환 + 토큰 정합

## 배경

Material3 visible 컴포넌트의 대부분 — Card 41회, Button 12회, Chip 6회, IconButton 8회 등 — 을 App* primitive 로 교체 + `MaterialTheme.colorScheme.*` 직접 사용(90+) → `LocalSqldpassPalette.current.*` 치환 + 매직 dp → 토큰 치환.

본 step 은 **화면 그룹별 sub-agent 5개 병렬** 진행. 메인 agent 가 결과 통합.

Step 1 의존 (AppQuestionContent 사용). Step 3 와 병렬 가능.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/`

## 화면 그룹 (5 sub-agent 병렬)

### 그룹 A — Solve + Runner (가장 critical)
- `ui/solve/SoloSolveScreen.kt`
- `ui/solve/components/SoloExplanationCard.kt`
- `ui/solve/components/SoloBottomActionBar.kt` (있다면)
- `ui/runner/QuestionRunnerScreen.kt`
- `ui/runner/CodeBlockCard.kt`
- `ui/common/SkeletonCard.kt` → AppSkeletonCard 로 대체

### 그룹 B — 결과 화면
- `ui/runner/QuestionResultScreen.kt`
- `ui/runner/RunnerJumpGrid.kt` (이미 Step 3 에서 일부 처리)

### 그룹 C — Wrong Answer / Bookmark / History
- `ui/wronganswer/WrongAnswerTab.kt` (Card 4회, OutlinedButton 1회, Button 1회, FilterChip 1회, Checkbox 1회)

### 그룹 D — Home + Tabs (Mock/Past/Solve)
- `ui/home/HomeScreen.kt` (Card 2회, IconButton 2회)
- `ui/home/CertCarousel.kt` (Card 1회)
- `ui/home/ContinueLastCard.kt` (Card 1회)
- `ui/home/CertInfoSheet.kt` (Step 3 에서 일부)
- `ui/mockexam/MockExamTab.kt` (Card 2회, FilterChip 1회, TextButton 3회)
- `ui/pastexam/PastExamTab.kt` (있다면 FilterChip 등)
- `ui/solve/SolveTab.kt` (Card 2회, FilterChip 1회, AssistChip 1회)
- `ui/insights/InsightsTab.kt` (Card 2회) + `ui/dashboard/DailyChart.kt` (Card 1회)
- `ui/common/CtaCard.kt` — wrapper 자체를 AppCard 호출로 변경
- `ui/common/MenuListRow.kt` — AppListRow 로 통합 또는 wrapper 정리
- `ui/common/AccentCard.kt` — AppCard(accent=...) 호출로 변경 또는 deprecate

### 그룹 E — Profile + Paywall + Auth
- `ui/profile/ProfileTab.kt` (이미 일부 App* 사용 중 — Card/OutlinedButton/AssistChip 잔재 정리)
- `ui/profile/KpiGrid.kt`
- `ui/passplus/PassPlusCatalogScreen.kt` (Card 2회, Button 1회, IconButton 1회)
- `ui/dashboard/DashboardTab.kt` (Card 5회, OutlinedButton 2회, AssistChip 1회)
- `ui/dashboard/NicknameEditDialog.kt` (OutlinedTextField → AppTextField + AppButton)

## 치환 규칙 (모든 그룹 공통)

### 컴포넌트 매핑

| Material3 | App* |
|---|---|
| `Card(...)` + CardDefaults | `AppCard(surface=Card, accent=None)` |
| `Card(...)` + elevated 톤 | `AppCard(surface=Elevated)` |
| `Button(...)` | `AppButton(text=..., variant=Primary, size=Regular)` |
| `OutlinedButton(...)` | `AppButton(variant=Secondary)` |
| `TextButton(...)` | `AppButton(variant=Tertiary)` |
| `FilterChip(selected=...)` | `AppChip(label=..., selected=...)` |
| `AssistChip(...)` | `AppChip(label=..., selected=false)` |
| `IconButton(...)` (prominent) | 자체 `Box(modifier.clickable.size(48.dp)) { Icon(...) }` 또는 신규 AppIconButton 추가(필요 시) |
| `Checkbox(...)` | 본 step 에서 신규 AppCheckbox 추가하거나 Material3 wrap. 호출처 1회뿐이라 단순 wrap 권장 |
| `OutlinedTextField(...)` | `AppTextField(value=..., onValueChange=...)` |
| `CircularProgressIndicator` (prominent) | `AppStateView(state = AppViewState.Loading)` 또는 자체 작은 progress |

### 토큰 매핑 (`MaterialTheme.colorScheme.*` → `LocalSqldpassPalette.current.*`)

| Material | Palette |
|---|---|
| `surface` | `card` |
| `onSurface` | `textPrimary` |
| `onSurfaceVariant` | `textMuted` |
| `outline`, `outlineVariant` | `border` |
| `surfaceVariant` | `elevated` |
| `primary` | `accent` |
| `onPrimary` | `accentFg` |
| `primaryContainer` | `accentSoftBg` |
| `onPrimaryContainer` | `textPrimary` (또는 accent) |
| `error` | `danger` |
| `errorContainer` | `dangerSoftBg` |
| `onErrorContainer` | `danger` |

### 매직 dp 토큰화

| dp | 토큰 |
|---|---|
| 6 | `SqldRadius.sm` |
| 8 | `SqldSpacing.sm` 또는 `SqldRadius.md` |
| 10 | `SqldSpacing.md - 2.dp` 또는 그대로 |
| 12 | `SqldSpacing.md` 또는 `SqldRadius.md` (radius) |
| 14 | `SqldRadius.lg - 2.dp` 또는 그대로 (corner 만) |
| 16 | `SqldSpacing.base` 또는 `SqldRadius.lg` |
| 18 | 그대로 (의도된 값) — 또는 `SqldSpacing.base + 2.dp` |
| 20 | `SqldSpacing.lg - 4.dp` 또는 `SqldRadius.xxl` |
| 24 | `SqldSpacing.lg` |

각 파일의 `private val CardCorner = 14.dp` / `ButtonCorner = 12.dp` const 제거 — 호출부에서 `SqldRadius.lg` / `SqldRadius.md` 직접 사용.

## Sub-agent 위임 가이드 (메인 agent 용)

본 step 의 메인 agent (=프로젝트 관리 agent) 는 다음 5개 sub-agent 를 동시 호출:

1. Sub-agent A: 그룹 A (Solve + Runner)
2. Sub-agent B: 그룹 B (결과)
3. Sub-agent C: 그룹 C (Wrong Answer)
4. Sub-agent D: 그룹 D (Home + Tabs)
5. Sub-agent E: 그룹 E (Profile + Paywall)

각 sub-agent 에게 본 step.md + `docs/MOBILE_UX_SPEC.md` + 위 매핑 표를 spec 으로 제공. 작업 후 각 sub-agent 는 변경 파일·치환 항목 보고.

메인 agent 가 모든 sub-agent 결과 수신 후 `assembleDebug` 빌드 검증.

## Acceptance Criteria

1. visible Material3 Card/Button/Chip/IconButton 직접 호출이 위 그룹에서 사라짐
2. `MaterialTheme.colorScheme.*` 직접 호출 90+ → 거의 0 (theme 정의·infrastructure 제외)
3. 매직 dp 의 라디우스 const(CardCorner/ButtonCorner) 제거
4. 기능 동작 회귀 0 (시각만 변경)
5. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

수동 시나리오 — 5탭 진입 + 풀이 + 결과 화면 + 자격증 시트 + 닉네임 편집 전반 시각 점검. AppQuestionContent 가 결과 화면 해설 렌더 검증 (Step 1 의 결과물 의존).

## 금지 사항

- 화면의 정보 위계·동선 변경 금지. 이유: UX 결정은 `docs/MOBILE_UX_SPEC.md` 진실 원천. 본 step 은 시각만.
- AppButton variant Mapping 임의 변경 금지 (Primary/Secondary/Tertiary 의미 일관). 이유: 시각 일관성.
- `MaterialTheme.colorScheme` 호출을 다른 색 hex 로 치환 금지. 이유: 항상 LocalSqldpassPalette 토큰. 색 hex 변경은 별 phase.
- 새 App* primitive 신설 금지 (필요하면 본 step 전에 Step 2 확장). 이유: scope creep 방지.
- CardDefaults.cardElevation 잔재 사용 금지. 이유: AppCard 가 elevation 정책 통합.

## Status 규칙

- 성공: step 4 `completed`, summary 에 그룹별 변경 항목 카운트.
- 실패: 3회 후 `error`.
- 일부 그룹만 완료 시: `blocked` + blocked_reason 에 미완 그룹 명시.
