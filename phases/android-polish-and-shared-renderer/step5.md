# Step 5 — Shell 폴리시 + Indication·햅틱 + 최종 검증

## 배경

Step 1~4 가 끝나면 남은 visible Material 잔재(NavigationSuiteScaffold / TopAppBar 색·shape) 미세 정리 + 촉감(Indication·햅틱·터치 타겟) 보강 + 전체 체크리스트 검증.

본 step 은 메인 agent 가 직접 진행 (sub-agent 없이) — 작은 변경 + 통합 검증.

Step 1~4 모두 의존.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/`

## 변경 대상

### Shell 시각 잔재

| 파일 | 변경 |
|---|---|
| `ui/common/TabScaffold.kt:42` | Material3 `TopAppBar` → 자체 `Row` + `Text` (statusBarsPadding, horizontal `SqldSpacing.lg`, `LocalSqldpassPalette.page` 배경, title `MaterialTheme.typography.titleLarge` + `textPrimary` 색). scroll behavior 는 본 step 미포함 — 별 phase. |
| `MainActivity.kt:414` (NavigationSuiteScaffold) | `containerColor = LocalSqldpassPalette.current.page` 명시. 탭 아이콘 색 selected=`accent`, unselected=`textMuted` 명시. NavigationSuiteScaffold 자체는 유지 (완전 교체는 별 phase) |
| `MainActivity.kt:408` (CircularProgressIndicator) | `color = LocalSqldpassPalette.current.accent` 명시 |

### Indication + 햅틱

| 위치 | 변경 |
|---|---|
| feature 화면의 카드·리스트 행 (`Modifier.clickable`) | `indication = SqldpassIndication.factory()` 명시 (이미 LocalIndication 으로 제공되더라도 명시) |
| 정답 채점 (SoloSolveScreen.submit, QuestionRunnerScreen.submit) | 정답 시 `HapticFeedbackType.Confirm`, 오답 시 `HapticFeedbackType.Reject` (Android 14+ API. 폴백 `LongPress`) |
| 옵션 더블탭 (AppOptionRow) | `HapticFeedbackType.LongPress` (이미 처리됐을 가능성) — 확인 |
| destructive 다이얼로그 confirm (종료/탈퇴) | `HapticFeedbackType.LongPress` |

### 48dp 터치 타겟

- IconButton 잔재 검증 — 48dp 미만이면 `sizeIn(minWidth = 48.dp, minHeight = 48.dp)` 적용
- AppListRow 의 onClick 영역 — 전체 행이 클릭 영역인지 검증

## Acceptance Criteria

1. TopAppBar 자체 헤더로 교체
2. NavigationSuiteScaffold containerColor / 아이콘 색이 LocalSqldpassPalette 토큰
3. 정답/오답 채점 시 햅틱 발동 (실기기 확인)
4. 모든 IconButton 잔재 48dp 보장
5. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL
6. `cd mobile; .\gradlew.bat :app:testDebugUnitTest` 통과

## 최종 체크리스트 검증

### Question Rendering
- [ ] 한글 긴 문장 readable
- [ ] Markdown 정상
- [ ] 코드블록 indent + 가로 스크롤
- [ ] SQL/C/Java/Python 코드 정상
- [ ] markdown table 정상
- [ ] HTML `<table>` 변환 정상
- [ ] inline SVG aspect ratio
- [ ] 이미지 aspect ratio
- [ ] **결과 화면 해설 markdown/code/table 정상 (Step 1 의 critical fix)**
- [ ] 옵션 정답/오답 시각이 readability 손상 X

### Material Removal
- [ ] 하단 nav 가 default Material 안 같음
- [ ] TopAppBar 가 default Material 안 같음
- [ ] Card/Button/Chip 직접 호출 사라짐
- [ ] Dialog/Sheet/Menu 가 App* primitive 거침
- [ ] `MaterialTheme.colorScheme` 직접 호출 feature UI 안에서 거의 0
- [ ] default Material ripple 이 주요 촉감 아님 (SqldpassIndication)

### Interaction
- [ ] 1차 터치 타겟 ≥ 48dp
- [ ] 옵션 선택 즉시 반응
- [ ] pressed state visible 하되 노이즈 X
- [ ] selected state 명확
- [ ] disabled/loading 시 중복 액션 방지
- [ ] 햅틱 의미 있는 곳만
- [ ] 하단 CTA 엄지 친화적
- [ ] motion fast + calm
- [ ] 긴 학습 세션 readable + low-fatigue

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

수동 시나리오 (실기기 또는 에뮬레이터):
1. 5탭 진입 → 각 탭 헤더 시각 점검 (TopAppBar 정리)
2. SoloSolve 진입 → 옵션 선택 (햅틱) → 정답 확인 (햅틱) → 정답 공개 시각
3. 모의고사 50문 응시 → 마지막 제출 → 결과 화면 해설 렌더 (Step 1 fix)
4. 종료 다이얼로그 → AppDialog 시각 + destructive 톤 + 햅틱
5. 자격증 카드 탭 → AppBottomSheet 시각
6. 운영 메뉴 → AppDropdown 시각

## 금지 사항

- 새 기능 추가 금지. 이유: 본 step 은 마무리 + 검증.
- 새 App* primitive 신설 금지. 이유: 본 phase 범위 외.
- 본 phase 의 step 1~4 산출물 변경 금지 (각 step 의 acceptance 가 통과한 상태). 이유: scope creep.
- iOS 측 어떤 변경도 금지. 이유: 본 phase 는 Android only.
- 백엔드·frontend 변경 금지. 이유: 본 phase 는 클라이언트(Android) UI only.

## Status 규칙

- 성공: step 5 `completed`, summary 에 빌드 결과 + 체크리스트 결과.
- 일부 체크리스트 실패: `blocked` + 어느 항목 미해결인지 명시.
- 빌드 실패: 3회 후 `error`.
