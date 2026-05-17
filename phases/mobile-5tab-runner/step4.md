# Step 4 — 모의고사 + 기출복원 탭

## 배경

두 탭 모두 "회차 카드 → 풀이 → 채점 결과" 흐름이고 차이는 채점 엔드포인트와 결과 모양(모의고사 = SolveResponse, 기출복원 = PastExamGradeResponse).

전제: Step 1·2·3 완료.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/ui/mockexam`, `ui/pastexam`

## 변경 대상

- 신규: `ui/mockexam/MockExamTab.kt`
- 신규: `ui/pastexam/PastExamTab.kt`
- 수정: `MainActivity.kt` (placeholder → 두 탭 컴포저블 연결)
- 수정: `ui/AppViewModel.kt` — 탭별 상태 슬라이스(`pastExams`, `selectedCertSlug`, `runner` sealed state) 추가, `loadPastExams`/`startMockExamRunner`/`startPastExamRunner`/`submitRunner` 인텐트 추가

## 변경 내용

`AppViewModel`:
- `AppUiState` 에 추가:
  - `pastExamsByCert: Map<String, List<PastExamSummary>>` (단순 fetch 결과 캐시)
  - `certSlugs: List<String>` — 응답 모음 또는 디폴트 `["SQLD","ADsP","ADP"]`
  - `runner: RunnerSession?` (`data class RunnerSession(mode, questions, originId, certSlug?)`)
  - `runnerResult: RunnerResult?`
- 인텐트:
  - `loadPastExams(certSlug)`
  - `startMockExamRunner(mockExamId)` — `repository.mockExam(id)` → `questions` 변환 → `runner = ...` set
  - `startPastExamRunner(pastExamId, certSlug)` — `repository.pastExam(id)` → `questions` 변환
  - `submitRunner(answers)` — `runner.mode` 분기 → `submitMockExam` 또는 `gradePastExam` → `runnerResult` set, `runner = null`
  - `dismissResult()` → `runnerResult = null`

`MockExamTab`:
- 기존 ExamCard 목록 유지(`state.mockExams`).
- 카드 클릭 → `onStart(exam.id)` → ViewModel `startMockExamRunner`.
- `state.runner` non-null 이고 `mode == MOCK_EXAM` 이면 `QuestionRunnerScreen` 표시.
- `state.runnerResult` 가 `SolveResult` 이면 `QuestionResultScreen` 표시.

`PastExamTab`:
- 자격증 슬러그 칩 행 (`state.certSlugs`). 첫 진입 시 `loadPastExams("SQLD")` 자동.
- 칩 탭 → 해당 슬러그로 `loadPastExams`.
- 회차 카드: 이름·총문항·`bestCorrectCount/bestTotalCount` 표시.
- 카드 탭 → `startPastExamRunner(id, slug)` → `QuestionRunnerScreen`.
- `state.runnerResult` 가 `PastExamResult` → `QuestionResultScreen`.

## Acceptance Criteria

1. `:app:assembleDebug` 통과.
2. 두 탭 모두 카드 → 풀이 → 제출 → 결과 → 닫기 동선이 실 기기에서 동작(수동 QA).
3. 모의고사 제출 실패 시 `clientSubmissionId` 큐잉(기존 `submitMockExam` 로직)이 깨지지 않음.

## 금지 사항

- 즐겨찾기·신고 버튼 추가 금지. 이유: 별도 백엔드 엔드포인트와 인증 분기 필요 — 본 phase 범위 밖.
- ViewModel 1개에 모든 탭 상태가 폭주하지 않도록, runner 는 한 번에 1개 세션만(`runner: RunnerSession?` 단일 필드). 이유: 단순 유지, 탭 전환 시 풀이 강제 종료가 자연스러움.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

## Status 규칙

- 성공: index.json step 4 `completed`.
- 실패: 3회 시도 후 `error` + `error_message`.
