# Step 1 — PastExam grade 응답에 milestoneReached 추가

## 배경

기출복원(`/past-exams/{id}`) 결과 화면을 모의고사와 동일한 frame 으로 통일하면서 마일스톤 토스트("🎉 N일 연속 학습!")를 양쪽에서 동일하게 띄우려고 한다.

현재 `PastExamPublicService.grade` 는 로그인 사용자에 대해 이미 `solveService.solve()` 를 호출해 solve 테이블에 적재하고 `solveId` 만 캡처한다(`saved.solve().getId()`, `backend/src/main/java/com/sqldpass/service/publicapi/PastExamPublicService.java:228-229`). `solveService.solve()` 는 `SolveWithStreak(Solve, StreakUpdateResult)` 를 반환하고 그 안의 `StreakUpdateResult.milestoneReached()` 가 모의고사 응답(`SolveResponse.from(solve, currentStreak, milestone)`, `SolveController.java:42-44`)에서 사용된다. 기출복원 응답에는 이 값이 빠져 있다.

이 step 에서는 `PastExamGradeResponse` 에 `Integer milestoneReached` 를 추가하고, 비로그인/저장 실패 시 null 로 내려보낸다.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/controller/publicapi/dto/PastExamPublicDtos.java` | `PastExamGradeResponse` record 에 `Integer milestoneReached` 필드 추가 |
| `backend/src/main/java/com/sqldpass/service/publicapi/PastExamPublicService.java` | `grade()` 가 `solveService.solve()` 반환의 `streakUpdate().milestoneReached()` 캡처 후 응답에 포함 |

## 구현

### A. `PastExamPublicDtos.PastExamGradeResponse`

현 record(112~125줄) 끝에 nullable 필드 추가. record 컴포넌트 순서 변경 없음 — 기존 컴포넌트 뒤에 append.

```java
public record PastExamGradeResponse(
        int totalCount,
        int correctCount,
        int score,
        List<GradedItem> items,
        Long solveId,
        List<SubjectScore> subjectScores,
        boolean passed,
        String passReason,
        /** 학습 연속일 마일스톤 도달 일수 — 도달 안 했으면 null, 비로그인이면 null */
        Integer milestoneReached
) {
}
```

### B. `PastExamPublicService.grade`

`solveService.solve()` 호출부(228줄)를 다음과 같이 보강:

1. `var saved = solveService.solve(...)` 결과에서 `saved.streakUpdate()` 가 null 이 아니면 `milestoneReached = saved.streakUpdate().milestoneReached()` 캡처
2. 비로그인 또는 저장 실패 catch 경로에서는 `milestoneReached = null` 그대로 유지
3. 응답 빌드(`new PastExamGradeResponse(...)`, 242~250줄)에 마지막 인자로 `milestoneReached` 전달

`Long solveId` 와 동일 스코프에서 `Integer milestoneReached = null` 선언 후 try 안에서 갱신.

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
.\gradlew.bat test
```

테스트:
- `PastExamPublicService` 단위 테스트가 있다면 통과 유지. 신규 마일스톤 필드는 nullable 이라 기존 단정문은 영향 없음.
- 기존 `gradePastExam` 컨트롤러 통합 테스트(있다면) 도 호환.

## Acceptance Criteria

1. `PastExamGradeResponse` record 의 컴포넌트가 8 → 9 개로 늘어났고 마지막이 `Integer milestoneReached` 이다.
2. `PastExamPublicService.grade` 가 로그인 사용자 + `solve` 저장 성공 시 `streakUpdate.milestoneReached()` 를 응답에 그대로 전달한다. (마일스톤 미도달 시 null)
3. 비로그인 호출(`memberId == null`) 시 `milestoneReached` 가 null 이다.
4. `solve` 저장 실패(try-catch ignored) 시에도 채점 결과 자체는 반환되며 `milestoneReached` 는 null 이다.
5. `./gradlew.bat compileJava` 통과.
6. `./gradlew.bat test` 통과(기존 테스트 회귀 없음).

## 금지 사항

- `PastExamGradeResponse` 의 기존 컴포넌트 순서를 바꾸지 마라. 이유: 프론트엔드 `pastExamApi.ts` 의 `PastExamGradeResponse` 타입은 필드 이름 기반(JSON) 으로 매핑되지만, 새 필드는 반드시 마지막에 추가해 record 생성자 인자 순서 회귀를 막는다.
- `solveService.solve()` 시그니처를 바꾸거나 새 오버로드를 만들지 마라. 이유: 모의고사 흐름과 동일 메서드를 그대로 재사용하는 것이 핵심. 별도 시그니처가 생기면 마일스톤 계산 로직이 갈라진다.
- `StreakService.updateOnSolve` 의 내부 동작을 수정하지 마라. 이유: 본 작업은 채점 응답 노출 범위 변경만. 마일스톤 계산 로직 자체는 모의고사·기출복원이 이미 공유한다.
- DB 마이그레이션을 추가하지 마라. 이유: 응답 DTO 필드만 추가하는 작업으로 스키마 변경이 없다.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "PastExamGradeResponse + grade() 마일스톤 전달, compileJava/test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: 기존 테스트가 마일스톤 필드 부재를 단정하고 있어 충돌이 발생할 경우 — 해당 단정문 보강 가능 여부 확인 후 진행.
