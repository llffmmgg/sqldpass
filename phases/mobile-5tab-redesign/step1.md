# Step 1 — 5탭 재구성 (nav-restructure)

## 배경

현재 BOTTOM_TABS 는 `Home/MockExam/PastExam/Solve/Dashboard` 5개. 학습 회귀 사이클(오답·인사이트·계정) 진입이 Dashboard 한 곳에 섞여 있어 발견성이 낮음. iOS 5탭(`Dashboard/MockExams/WrongAnswers/Insights/Profile`)과도 비대칭.

목표 5탭:

```
Home  ┃  MockExam  ┃  WrongAnswers (NEW)  ┃  Insights (NEW)  ┃  Profile (NEW)
```

PastExam·Solve·Dashboard 는 풀스크린 또는 라우트로만 강등 (다른 step 에서 정리). 본 step 은 nav 컴파일 통과만 책임진다.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app`

## 변경 대상

- `nav/SqldpassNav.kt` — `SqldpassRoute` 에 `WrongAnswers`/`Insights`/`Profile` 추가. `BOTTOM_TABS` 5개 교체. PastExam·Solve·Dashboard route 는 enum 으로 남겨 두되 BOTTOM_TABS 에서 제거.
- `ui/wronganswer/WrongAnswerTab.kt` — 신설, 빈 스텁(`PlaceholderTab(title = "오답노트")`).
- `ui/insights/InsightsTab.kt` — 신설, 빈 스텁.
- `ui/profile/ProfileTab.kt` — 신설, 빈 스텁.
- `ui/common/PlaceholderTab.kt` (신설) — 후속 step 에서 채울 빈 화면 헬퍼.
- `MainActivity.kt` — NavHost 에 새 라우트 3개 추가 (스텁 호출), `BOTTOM_TABS` 아이콘/라벨 검토.

## 아이콘 매핑 (Material Icons.Outlined)

| 탭 | 아이콘 | 라벨 |
|---|---|---|
| Home | `Home` | 홈 |
| MockExam | `Quiz` | 모의고사 |
| WrongAnswers | `ReplayCircleFilled` 또는 `History` | 오답노트 |
| Insights | `BarChart` | 인사이트 |
| Profile | `PersonOutline` | 마이 |

`ReplayCircleFilled` 가 없으면 `Replay`. import 가능한 것 우선.

## Acceptance Criteria

1. `.\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL.
2. 5개 탭이 하단에 보이고, 각 탭 진입 시 자기 화면(또는 스텁) 표시.
3. 기존 MockExam/PastExam/Solve/Dashboard 풀이 진입 경로(Runner 전환)는 깨지지 않음 — MockExamTab 만 탭으로 노출되고, PastExam/Solve/Dashboard 는 라우트로 남아 있어 호출 가능.

## 금지 사항

- ViewModel 시그니처/AppUiState 필드 변경 금지. 이유: 후속 step 에서 사용되며 본 step 은 nav-only.
- 기존 Tab 컴포넌트(MockExamTab/PastExamTab/SolveTab/DashboardTab) 본문 변경 금지. 이유: step 2~5 에서 점진적으로 흡수/대체.
- 새 스텁 화면에 비즈니스 로직 추가 금지. 이유: 본 step 은 nav 회귀만 검증.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

## Status 규칙

- 성공: index.json step 1 `completed`, summary 한 줄.
- 실패: 3회 시도 후 `error` + `error_message`.
