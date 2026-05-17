# Step 1 — 데이터 레이어 확장

## 배경

`mobile/app` 의 `SqldpassApi` 는 현재 모의고사 + 모바일 스냅샷 + 구글 로그인 + Play Billing 검증만 제공. 신규 4탭(기출복원·문제풀기·대시보드)이 사용할 6개 엔드포인트와 대응 DTO·Repository 메서드가 모두 부재. 후속 step 들이 의존하는 토대.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/data`

## 변경 대상

- `data/remote/SqldpassApi.kt`
- `data/ApiModels.kt`
- `data/AppRepository.kt`

## 변경 내용

`SqldpassApi` 에 추가:

- `GET /api/public/past-exams?cert={slug}` → `List<PastExamSummary>`
- `GET /api/public/past-exams/{id}` → `PastExamDetail`
- `POST /api/public/past-exams/{id}/grade` → `PastExamGradeResponse`
- `GET /api/subjects` → `List<SubjectResponse>`
- `GET /api/questions?subjectId=&size=` → `List<QuestionResponse>`
- `GET /api/streak/me` → `StreakResponse`
- `GET /api/solves/stats/overall-avg` → `OverallAvgResponse`
- `GET /api/mock-exams/best-scores` → `Map<String, BestScoreEntry>` (백엔드가 mockExamId 키 맵 또는 List 중 어느 쪽이든 둘 다 시도, 변환 실패는 빈 응답 처리)

`ApiModels` 에 record 추가 (Kotlin data class):

- `PastExamSummary`, `PastExamDetail`, `PastExamQuestion`
- `PastExamGradeRequest`, `PastExamAnswer`, `PastExamGradeResponse`, `PastExamSubjectScore`, `PastExamGradedItem`
- `SubjectResponse` (id, name, parentId, parentName 정도 — 백엔드 응답 키 그대로)
- `QuestionResponse` (id, subjectId, content, questionType)
- `StreakResponse` (currentStreak, longestStreak, milestoneDays)
- `OverallAvgResponse` (overallAvg, myRecentAvg 등 응답 키 미상 시 nullable 로 받기)
- `BestScoreEntry` (mockExamId, correctCount, totalCount)

`AppRepository` 에 메서드 추가 (모두 suspend, 에러는 호출자에 throw — repository 캐싱은 이번 범위 밖):

- `pastExams(certSlug: String?): List<PastExamSummary>`
- `pastExam(id: Long): PastExamDetail`
- `gradePastExam(id: Long, answers: List<PastExamAnswer>): PastExamGradeResponse`
- `subjects(): List<SubjectResponse>`
- `randomQuestions(subjectId: Long, size: Int): List<QuestionResponse>`
- `streak(): StreakResponse`
- `overallAvg(): OverallAvgResponse`
- `bestScores(): List<BestScoreEntry>` (서버 응답 모양에 따라 분기 후 List 로 정규화)

## Acceptance Criteria

1. `:app:assembleDebug` 통과.
2. 새 record 는 모두 Moshi `data class` + nullable 안전. enum 은 `String` 으로 받음(QuestionType 등).
3. 기존 메서드 (`mockExams`, `mockExam`, `syncContent`, `submitMockExam`, `drainPendingSolves`) 시그니처 무변경.

## 금지 사항

- `kotlinx.serialization` 도입 금지. 이유: 기존 프로젝트가 Moshi 단일 스택 — 직렬화 라이브러리 추가는 본 step 범위 밖.
- Room schema 변경 금지. 이유: 신규 엔드포인트는 모두 온라인 호출 기반. 오프라인 캐시는 후속 phase.
- 백엔드 응답 모양을 추측해 결정 못한 필드를 추가하지 마라. 이유: 알 수 없는 필드는 Moshi `@Json(name=...)` 또는 nullable 로 받고, UI 에서 분기.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

## Status 규칙

- 성공: index.json step 1 `completed`, summary 한 줄.
- 실패: 3회 시도 후 `error` + `error_message`.
