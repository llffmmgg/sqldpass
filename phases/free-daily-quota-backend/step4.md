# Step 4 — QuotaExceededException → 402 ControllerAdvice

## 배경

`DailyUsageService` 가 throw 하는 `QuotaExceededException` 을 HTTP 402 Payment Required + 구조화된 body 로 변환. 클라이언트 3 플랫폼(웹/iOS/Android)이 이 응답 형태를 디코딩해 모달을 띄운다.

응답 형식 (확정):
```json
{
  "error": "DAILY_QUESTION_LIMIT",
  "used": 30,
  "limit": 30,
  "resetAt": "2026-05-22T00:00:00"
}
```

`resetAt` 은 KST naive `LocalDateTime` (메모리 `project_kst_naive_serialization`). 프론트가 `+09:00` 부착.

## 작업 디렉터리

```
backend/
```

## 변경 대상

기존 `@ControllerAdvice` 가 있는지 먼저 확인:

```powershell
Get-ChildItem backend/src/main/java -Recurse -Filter *.java | Select-String "@ControllerAdvice"
```

- **있으면**: 그 클래스에 `@ExceptionHandler(QuotaExceededException.class)` 메서드 추가
- **없으면**: 신규 `backend/src/main/java/com/sqldpass/controller/common/QuotaExceededAdvice.java` 또는 `GlobalExceptionHandler.java` 생성. 패키지는 기존 `controller/common/` 또는 `service/common/` 컨벤션 따름

## ExceptionHandler 작성 가이드

```java
@ExceptionHandler(QuotaExceededException.class)
public ResponseEntity<QuotaExceededBody> handleQuotaExceeded(QuotaExceededException e) {
    return ResponseEntity
            .status(HttpStatus.PAYMENT_REQUIRED)
            .body(new QuotaExceededBody(e.getCode(), e.getUsed(), e.getLimit(), e.getResetAt()));
}

public record QuotaExceededBody(
        String error,
        int used,
        int limit,
        LocalDateTime resetAt
) {}
```

CLAUDE.md 규칙: Response DTO 는 `controller/` 패키지, record 사용.

## 테스트

`QuotaExceededAdviceTest` — `@WebMvcTest` 슬라이스 또는 통합 테스트. DailyUsageService 를 mock 해서 `QuotaExceededException` throw 하도록 stub, MockMvc 로 호출, 응답 status 402 + body 필드 4개 검증.

기존 `@ControllerAdvice` 에 추가했다면 기존 테스트도 회귀 없는지 확인.

## 검증

```powershell
cd backend
.\gradlew.bat test
```

## Acceptance Criteria

1. `QuotaExceededException` 이 throw 되면 응답 status 402 + body `{ error, used, limit, resetAt }` 반환.
2. `resetAt` 은 KST naive ISO 형식으로 직렬화 (예: `2026-05-22T00:00:00`).
3. 기존 다른 예외 핸들러 회귀 없음.
4. `gradlew.bat test` 통과.

## 금지 사항

- HTTP 403(Forbidden) 또는 429(Too Many Requests) 로 변환하지 마라. 이유: 정책상 "결제 유도" 의도가 명확한 402 가 클라이언트 모달 분기 키로 사용됨.
- `resetAt` 을 UTC `Z` 표기로 직렬화하지 마라. 이유: 메모리 `project_kst_naive_serialization` — 프론트가 +09:00 부착하는 규약.
- 이 step 에서 컨트롤러 가드(`consumeQuestion` 호출) 까지 함께 만들지 마라. 이유: step 5 의 책임이며 통합 테스트는 거기서 함께.

## Status 규칙

- 성공: step 4 `completed`.
- 실패: 3회 후 `error`.
