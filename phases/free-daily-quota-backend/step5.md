# Step 5 — 컨트롤러 가드 + quota 엔드포인트

## 배경

`DailyUsageService` 호출을 두 컨트롤러에 삽입. 추가로 클라이언트가 사전 표시("오늘 18/30")할 수 있게 quota 조회 엔드포인트를 제공.

가드 위치:

| 컨트롤러 | 메서드 | 가드 |
|---|---|---|
| `QuestionController.getQuestions` (`/api/questions`) | 진입 시점 | `consumeQuestion(memberId, size)` |
| `MockExamController.get(id)` (`/api/mock-exams/{id}`) | 정규+미니 진입점 | `consumeMockSession(memberId)` — 단 `MockExamKind.PAST_EXAM`(기출복원)은 면제 |

**기출복원 면제 처리**: `MockExamController.get(id)` 가 정규/미니/기출을 모두 처리하는 단일 엔드포인트라 `MockExamEntity.getKind()` 로 분기. `MockExamKind.PAST_EXAM` 일 때 `consumeMockSession` 호출하지 마라.

## 작업 디렉터리

```
backend/
```

## 변경 대상

기존 파일 수정 2개:

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/controller/question/QuestionController.java` | `getQuestions` 진입 시 `consumeQuestion(memberId, size)` |
| `backend/src/main/java/com/sqldpass/controller/mockexam/MockExamController.java` | `get(id)` 에서 MockExam 조회 후 PAST_EXAM 아니면 `consumeMockSession(memberId)` |

신규 파일 2개:

| 파일 | 목적 |
|------|------|
| `backend/src/main/java/com/sqldpass/controller/usage/QuotaController.java` | `GET /api/quota` — 현재 사용량 조회 |
| `backend/src/test/java/com/sqldpass/controller/usage/QuotaIntegrationTest.java` | 통합 테스트 |

## QuestionController 수정 가이드

`QuestionController.getQuestions` 의 첫 줄(memberId 추출 후, questionService 호출 전):

```java
dailyUsageService.consumeQuestion(memberId, size);
```

`DailyUsageService` 를 `@RequiredArgsConstructor` 필드로 추가. 단순 한 줄 삽입.

## MockExamController 수정 가이드

`get(id)` 메서드 안에서 MockExam 을 먼저 조회 → `MockExamKind` 확인 → PAST_EXAM 이 아니면 가드 호출. **현재 코드는 `mockExamService.getForUser(id, memberId)` 가 본 객체를 가져옴**. 그 후 가드. 또는 더 깔끔하게 service 안에서 처리할 수도 있으나 컨트롤러 가드 일관성을 위해 컨트롤러 레벨에서:

```java
@GetMapping("/{id}")
public MockExamDetailResponse get(@PathVariable Long id, HttpServletRequest request) {
    Long memberId = (Long) request.getAttribute("memberId");
    var detail = mockExamService.getForUser(id, memberId);
    // 기출복원(PAST_EXAM) 은 무료 무제한 — 가드 면제
    if (detail.getKind() != MockExamKind.PAST_EXAM) {
        dailyUsageService.consumeMockSession(memberId);
    }
    return MockExamDetailResponse.from(detail);
}
```

(실제 도메인 메서드 시그니처 확인 후 적용. `getKind()` 또는 enum 접근 경로가 다르면 조정.)

**중요**: `/api/mock-exams` 목록 GET, `/api/mock-exams/mini` 목록 GET, `/api/mock-exams/best-scores`, `/api/mock-exams/pdf/eligibility`, `/api/mock-exams/{id}/pdf/download` 에는 **가드 호출 추가 금지**. 이유: 풀이 시작이 아닌 목록/메타 조회이며, 가드를 추가하면 목록 보기만 해도 카운트가 증가한다.

## QuotaController 작성

```java
@RestController
@RequiredArgsConstructor
@Tag(name = "사용량", description = "무료 일일 한도 조회")
public class QuotaController {

    private final DailyUsageService dailyUsageService;

    @GetMapping("/api/quota")
    public DailyUsageService.Quota getMyQuota(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        return dailyUsageService.getQuota(memberId);
    }
}
```

**WebMvcConfig 등록 확인**: `/api/quota/**` 가 memberAuthInterceptor 의 `addPathPatterns` 에 포함되도록 추가. 현재 등록된 `/api/mock-exams/**`, `/api/questions/**` 와 같은 줄에 `/api/quota/**` 추가.

```java
// backend/src/main/java/com/sqldpass/config/WebMvcConfig.java
registry.addInterceptor(memberAuthInterceptor)
        .addPathPatterns(
                /* 기존 경로들 */,
                "/api/quota/**"
        );
```

## 통합 테스트 (필수)

`QuotaIntegrationTest` — `@SpringBootTest` + `@AutoConfigureMockMvc`.

시나리오:

1. 무료 회원, `GET /api/questions?subjectId=1&size=20` → 200, 다시 호출 (총 40) → **두 번째 호출에서 402** + body `error=DAILY_QUESTION_LIMIT, used=30, limit=30` (실제로는 누적 카운트가 40이 되려다 멈추므로 used 값은 구현에 따라 30 또는 40 — 정책상 30 으로 보이는 게 자연스럽지만 step 3 의 롤백 결과 30 이 맞음)
2. 무료 회원, `GET /api/mock-exams/{정규모의ID}` → 200, 다시 다른 모의 → **두 번째 402** + `DAILY_MOCK_LIMIT`
3. 무료 회원, `GET /api/mock-exams/{기출ID}` → 1회·2회·3회 모두 200 (기출 면제)
4. 무료 회원, `GET /api/mock-exams/{미니ID}` 후 `GET /api/mock-exams/{정규모의ID}` → 두 번째 402 (미니+모의 합산)
5. 활성 구독자, 위 모든 호출 무제한 200
6. `GET /api/quota` (무료) → `{ questionUsed: 18, questionLimit: 30, mockUsed: 0, mockLimit: 1, resetAt: "..." }`
7. `GET /api/quota` (구독자) → `{ questionUsed: 0, questionLimit: null, mockUsed: 0, mockLimit: null, resetAt: "..." }`

테스트 데이터 셋업이 무거우면 핵심 1·2·3·5번만이라도 통과시켜라.

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. QuestionController, MockExamController 가드 삽입 (단 기출복원/목록/메타는 면제).
2. QuotaController 신규 + WebMvcConfig 에 `/api/quota/**` 인터셉터 등록.
3. 통합 테스트 최소 4개(1·2·3·5번) 통과.
4. `gradlew.bat test` 전체 통과.

## 금지 사항

- 목록 엔드포인트(`/api/mock-exams`, `/api/mock-exams/mini`) 에 가드 추가 금지. 이유: 풀이 시작이 아니라 메타 조회이며 가드 추가 시 목록만 봐도 카운트 증가.
- `/api/mock-exams/{id}/pdf/download` 에 가드 추가 금지. 이유: PDF 는 별도 결제 권한 체계.
- WebMvcConfig 의 기존 `addPathPatterns` 줄을 통째로 재작성하지 마라. 이유: 다른 경로 누락 위험. 새 entry 만 추가.
- QuestionDetail(`/api/questions/{id}`) 에 가드 추가 금지. 이유: 풀이 후 해설 조회는 카운트 대상 아님.

## Status 규칙

- 성공: step 5 `completed` + summary. phase 전체 완료 표시.
- 실패: 3회 후 `error`.
- blocked: WebMvcConfig 등 인프라 변경에 대한 사용자 결정 필요 시 `blocked`.
