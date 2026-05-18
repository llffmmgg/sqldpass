# Step 2 — Android HistoryScreen (풀이기록) 신설

## 작업 디렉터리
`mobile/`

## 배경 / Why
- iOS `HistoryView.swift` + `HistoryViewModel.swift` 가 풀이기록을 표시한다 (Solve 목록 + 점수 + 일자).
- Android 는 `ProfileTab.kt:194-196` 에서 "풀이 기록 화면은 곧 출시됩니다" 토스트만. 페어리티 갭 (P1-1).
- 백엔드: `GET /api/solves` (내 풀이 목록), `GET /api/solves/{id}` (상세), `GET /api/solves/me/daily?days=14` (이미 사용 중).
- step1 (Bookmarks) 의 패턴을 그대로 재사용.

## 변경 대상

### 1. ApiModels.kt
- iOS `Solve.swift` 와 동일한 DTO mirror 가 Android 에 있는지 확인. 없으면 추가:
  - `SolveSummary` (id, mockExamId?, mockExamName?, subjectId?, score, submittedAt) — `/api/solves` list 응답.
  - `SolveDetail` — `/api/solves/{id}` 응답.
- 실 백엔드 응답 JSON 형상은 `backend/.../controller/solve/dto/` 에서 확인.

### 2. SqldpassApi.kt
- `@GET("api/solves") suspend fun getMySolves(): List<SolveSummary>` 가 있는지 확인. 없으면 추가.
- `getSolve(id)` 는 이미 있음 (line 56).

### 3. AppViewModel state + 메서드
- `AppUiState` 에:
  - `val history: List<SolveSummary> = emptyList()`
  - `val historyLoading: Boolean = false`
  - `val historyError: String? = null`
- `fun loadHistory()` — 로그인 가드 후 API 호출.

### 4. 새 화면 (`mobile/app/src/main/java/com/sqldpass/app/ui/history/HistoryScreen.kt`)
- iOS HistoryView 정보 구조 따라:
  - TopAppBar 뒤로가기 + "풀이 기록"
  - LazyColumn → 카드 항목
  - 항목: 회차/주제 + 점수(맞은 개수/총개수) + 날짜
  - 탭 시 상세 진입 — **본 step 에서는 상세 화면 신설 안 함**. 우선 목록만. 탭 동작은 비활성/`Toast` 로 두거나 step3 (별도 phase) 로 미룸. README 에 명시.
- 로딩/empty/error AppStateView.

### 5. NavGraph
- `SqldpassRoute.History : SqldpassRoute("history")` 추가.
- `MainActivity.kt` NavHost 에 composable 추가.

### 6. ProfileTab 연결
- "풀이 기록" 행의 onClick 을 placeholder → `onOpenHistory()` 로.
- `ProfileTab` signature 에 `onOpenHistory: () -> Unit` 추가.
- MainActivity 에서 `onOpenHistory = { navController.navigate(SqldpassRoute.History.route) }`.

## 작업 절차
1. step1 (Bookmarks) 변경 패턴 그대로 따라가기.
2. Solve 응답 DTO 가 Android 에 있나 확인 후 필요 시 추가.
3. HistoryScreen 신설.
4. NavGraph + ProfileTab 배선.
5. `:app:assembleDebug` 통과.

## 검증
```powershell
cd C:\\Users\\admin\\desktop\\sqldpass\\sqldpass\\mobile
.\\gradlew.bat :app:assembleDebug
```

## 금지사항
- 상세 화면(SolveDetail) 신설 금지. 이유: 본 phase 의 스코프는 목록만. 상세는 follow-up.
- iOS HistoryView 의 시각 디자인을 그대로 베끼지 말 것. 이유: Android Inked OMR 디자인 시스템(LocalSqldpassPalette + AppCard) 기준으로 자연스럽게 구성.
- backend 변경 금지.

## 산출물
- 신규/수정 파일 목록 + 핵심 로직 요약.
- `:app:assembleDebug` 결과 마지막 5줄.
