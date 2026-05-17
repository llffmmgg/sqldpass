# Step 5 — 문제풀기 + 대시보드 탭

## 배경

마지막 두 탭. 문제풀기는 과목 칩 선택 → 10문제 세트 풀이 → `/api/solves` 제출. 대시보드는 회원 카드 + streak + overall-avg + 시험별 best-score + PASS+ 카드.

전제: Step 1·2·3·4 완료.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/ui/solve`, `ui/dashboard`

## 변경 대상

- 신규: `ui/solve/SolveTab.kt`
- 신규: `ui/dashboard/DashboardTab.kt`
- 수정: `MainActivity.kt` (placeholder → 두 탭 컴포저블 연결)
- 수정: `ui/AppViewModel.kt` — `subjects`, `selectedSubjectId`, `streak`, `overallAvg`, `bestScores` 추가; `loadSubjects`, `loadDashboard`, `startPractice(subjectId)` 인텐트

## 변경 내용

`AppViewModel` 추가 상태/인텐트:
- `subjects: List<SubjectResponse>` + `loadSubjects()` (첫 진입 시 자동)
- `dashboard: DashboardData?` + `loadDashboard()` — streak·overallAvg·bestScores 를 한 번에 fetch, 부분 실패는 null 로 두고 계속 진행
- `startPracticeRunner(subjectId)` — `repository.randomQuestions(subjectId, 10)` → `runner` set
- `submitRunner` 분기에 `RunnerMode.PRACTICE` 추가 → `repository.submitMockExam` 대신 직접 `POST /api/solves` 호출 경로 필요. 단순화: `AppRepository` 에 `submitPractice(subjectId, answers): SolveResponse` 추가 (Step 1 에서 같이 처리되었으면 OK, 아니면 본 step 에서 추가).

`SolveTab`:
- 과목 칩 그리드(부모 과목별로 그룹). 칩 탭 → `startPracticeRunner(id)`.
- `runner` 가 PRACTICE 모드면 `QuestionRunnerScreen` 표시.
- 결과는 `SolveResult` → `다음 세트` 버튼이 같은 subjectId 로 재시작.

`DashboardTab`:
- 회원 카드(상단): 로그인 안됨 → "Google 로 로그인" 버튼 / 로그인됨 → 닉네임 + 로그아웃 TextButton.
- streak 카드: `currentStreak` 큰 숫자 + "연속 학습 일수" 라벨.
- 평균 카드: `overallAvg` 값을 "전체 14일 평균 X문 / 일" 식으로 표시.
- best-score 그리드: 시험별 카드 LazyVerticalGrid(2칸) — 이름 또는 mockExamId(이름 매핑이 있으면 사용, 없으면 `#id`) + `best/total`.
- PASS+ 카드: 기존 ActionCard 패턴 재사용 → `onPurchase("iap_one_month")`.

## Acceptance Criteria

1. `:app:assembleDebug` 통과.
2. 문제풀기 한 세트 풀이 → 다음 세트 → 정상 동작(수동 QA 시 같은 subjectId 로 10문 새로 받아옴).
3. 대시보드 진입 시 streak/overall/best 중 일부가 401(비로그인) 이거나 500 이어도 다른 카드는 표시.

## 금지 사항

- 합격률 자체 계산 금지(클라이언트에서 합격 기준 흉내). 이유: 자격증별 과락 룰이 백엔드 PastExamGradeResponse 의 `subjectScores`/`passed` 에 있음. 클라 흉내는 후속 phase 에서도 하지 마라.
- 일별 풀이 추이 차트 금지. 이유: 백엔드에 일별 집계 엔드포인트 부재 — 별도 phase.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

## Status 규칙

- 성공: index.json step 5 `completed`.
- 실패: 3회 시도 후 `error` + `error_message`.
