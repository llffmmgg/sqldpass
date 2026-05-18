# Step 3 — 오답노트 탭 (WrongAnswerTab)

## 배경

회귀 학습의 핵심 가치. 현재는 Dashboard 의 "취약 과목" 카드에서 과목 단위로만 진입 가능. 사용자가 **개별 문제를 골라서 일괄 재시도** 할 수 있어야 한다.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/ui/wronganswer`

## 변경 대상

- `ui/wronganswer/WrongAnswerTab.kt` 신설.
- `ui/AppViewModel.kt` —
  - `loadWrongAnswers(subjectId: Long? = null)` 추가 (단순 fetch → `state.wrongAnswers`).
  - `AppUiState` 에 `wrongAnswers: List<WrongAnswerSummary> = emptyList()`, `wrongAnswersLoading: Boolean = false` 필드 추가.
  - `startWrongAnswersFromQuestions(items: List<WrongAnswerSummary>, title: String)` 추가 — 선택된 항목으로 `RunnerSession(mode = WRONG_ANSWERS)` 구성. `subjectId` 는 첫 항목 기준, 같은 과목 아니면 0.
- `MainActivity.kt` — WrongAnswers 라우트가 위 콜백을 받도록 호출부 확장.

## UX 구성

```
HeroHeader: "약점만 노려서"
├─ 필터 칩 LazyRow: [전체] [SQL 기본] [SQL 활용] [데이터모델링] ...
├─ 일괄 액션 카드: "선택된 N문제 / 전체 K문제"
│   [전체 다시풀기]  [선택 시작 (N)]   ← N 0 일 때 비활성
└─ LazyColumn 항목:
    ┌────────────────────────────────┐
    │ ☑  Q#37  데이터모델링  3회 틀림 │
    │    "다음 중 슈퍼타입/서브타입..." │
    │    [북마크]                    │
    └────────────────────────────────┘
```

세부:

- 진입 시 `loadWrongAnswers()` 1회. 토큰 없으면 로그인 유도 빈 상태(`MascotEmpty` 패턴 없으므로 `CtaCard` 재사용).
- 필터 칩: `wrongAnswers` 의 `subjectName` distinct + "전체". 선택 시 client-side filter, 추가 fetch 없음.
- 멀티선택은 항상 체크박스 노출(롱탭 모드 분리 없이 단순화). 항목 카드 탭 = 토글.
- "전체 다시풀기" → 현재 필터된 전체로 `startWrongAnswersFromQuestions`.
- "선택 시작" → 체크된 항목으로 `startWrongAnswersFromQuestions`.
- 시작 후 `navController.navigate(Runner)` (MainActivity 가 처리).
- "3회 틀림" 같은 `wrongCount` 뱃지는 우측 정렬.

## Acceptance Criteria

1. `.\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL.
2. 비로그인: 로그인 유도 CTA + 빈 리스트.
3. 로그인: 오답 리스트 표시, 필터 칩 동작, 멀티선택 후 "선택 시작" → Runner 진입.
4. Runner 결과 화면에서 dismiss 시 오답노트 탭으로 복귀.

## 금지 사항

- 백엔드 신규 endpoint 추가 금지. 이유: `/api/wrong-answers` 가 이미 있고 questionIds 필터는 client-side 로 처리 가능.
- 풀이 후 자동으로 해당 항목을 리스트에서 제거하는 로직 추가 금지. 이유: 서버 `wrongCount` 가 다음 fetch 에서 갱신되며, 본 step 은 화면만 책임.
- `WrongAnswersView` 라는 이름 사용 금지. 이유: 기존 컨벤션이 `*Tab` 이므로 `WrongAnswerTab` 으로 통일.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```
