# Step 5 — iOS 5탭 재정의 + 기출복원 분리

## 배경

현재 iOS MainTabView: dashboard / mockExams / wrongAnswers / insights / profile.
신규: home / mockExams / pastExams / soloSolve / profile.

핵심 변경:
- WrongAnswers, Insights 탭 제거 (View 는 유지 — ProfileView 안 NavigationLink 으로 진입)
- PastExams 탭 신규 (현재 iOS 에 기출복원 진입이 명확히 정의돼 있지 않음 — MockExamsListView 와 별도로 신설)
- SoloSolve 탭 신규 (이번 phase 이전의 fullScreenCover 임시 진입점은 제거)

macOS 검증 대기 (Windows 환경에서 코드만 작성).

## 작업 디렉터리

`ios/`

macOS 셸에서만 빌드 검증. Windows 에서 xcodebuild 시도 금지.

## 변경 대상

| 파일 | 변경 |
|---|---|
| `App/MainTabView.swift` | `enum MainTab` 재정의 — `.home, .mockExams, .pastExams, .soloSolve, .profile`. TabView 5개 라벨/아이콘 갱신. 기존 wrongAnswers/insights 탭 제거. fullScreenCover SoloSolveContext 임시 진입 제거. |
| `Features/Home/HomeView.swift` (신규) | DashboardView 의 자리를 새 HomeView 가 차지(파일 신규). 본 step 은 빈 스텁 + `Text("홈")` 정도 — step 6 에서 본격 구현. |
| `Features/PastExams/PastExamsListView.swift` (신규) | MockExamsListView 패턴 미러 — 자격증 칩 + 회차 카드. PastExamService 가 없으면 기존 MockExamService 의 pastExam 메서드 또는 신규 — `frontend` 의 /api/public/past-exams 호출. 본 step 은 골격만 + 데이터 fetch placeholder. |
| `Features/Solo/SoloHubView.swift` (신규) | 실전 문제 탭 진입 화면 — Android SolveTab 동등. 자격증 칩 + 과목 카드 그리드, 카드 탭 → SoloSolveView push. 카드 데이터는 `/api/subjects` 호출. |
| `App/MainTabView.swift` | SoloSolveContext 임시 진입 제거. SoloSolve 탭에 SoloHubView 등록 — NavigationStack 안에. |
| `project.yml` | Features/Home/, Features/PastExams/, Features/Solo/ 폴더가 sources 패턴(`- path: Sqldpass`)에 포함되는지 확인 — createIntermediateGroups: true 라 자동 포함 예상. |

## Acceptance Criteria (macOS 환경에서 검증)

1. `cd ios && ~/bin/xcodegen generate && xcodebuild ... build` → BUILD SUCCEEDED.
2. 시뮬레이터 실행 시 하단 5탭: 홈/모의고사/기출복원/실전 문제/마이.
3. WrongAnswers, Insights 탭 아이콘 사라짐.
4. 기출복원 탭 진입 시 자격증 칩 + 회차 카드 (데이터 fetch 미구현이면 skeleton 또는 placeholder).
5. 실전 문제 탭 진입 시 자격증 칩 + 과목 카드 — 카드 탭 시 SoloSolveView push.
6. Home 탭 진입 시 일단 스텁 텍스트 (step 6 에서 본 구현).

## 검증

```bash
cd ios
~/bin/xcodegen generate
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

**Windows 환경에서 본 step 실행 불가** — 코드 작성만 Windows 에서 수행하고, 빌드 검증은 macOS 사용자.

## 금지 사항

- DashboardView.swift 파일을 삭제하지 마라. 이유: step 6 에서 HomeView 내용을 결정하는 동안 reference 로 둘 가능성. 다만 MainTabView 에서 호출은 끊는다.
- WrongAnswersView, InsightsView 화면 본체를 손대지 마라. 이유: ProfileView 안에서 NavigationLink 으로 진입 — step 7 에서.
- PastExamsListView 의 데이터 fetch 를 본 step 에서 완전 구현하려 하지 마라. 이유: 골격 + placeholder 까지가 본 step 범위. 본격 구현은 별 step 또는 후속 phase.
- SoloSolveView 본체를 손대지 마라. 이유: 이미 동작 중 (이전 phase). 본 step 은 진입 위치만 변경.

## Status 규칙

- macOS 빌드 통과: `completed` + summary.
- 코드만 작성 + 빌드 미검증(Windows): `blocked` + blocked_reason: "iOS 빌드 검증 macOS 필요".
- 실패: 3회 후 `error`.
