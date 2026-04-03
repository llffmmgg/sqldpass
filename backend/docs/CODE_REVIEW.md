# 코드 리뷰 (2026-04-03)

## Critical

| 위치 | 문제 | 해결책 |
|------|------|--------|
| SolveController.java:51 | **IDOR 취약점** — `getSolve(id)`에 memberId 검증 없음. 아무나 다른 사용자의 풀이 기록을 조회 가능 | `@RequestHeader("X-Member-Id")` 추가 후 서비스에서 소유자 검증 |
| AdminAuthController.java:39 | **타이밍 공격 취약점** — `.equals()`로 비밀번호 비교 시 문자열 길이에 따라 응답 시간이 달라져 비밀번호 유추 가능 | `MessageDigest.isEqual(a.getBytes(), b.getBytes())` 사용 |
| SolveController.java:36,43 | **인증 부재** — `X-Member-Id` 헤더를 클라이언트가 임의로 설정 가능. 다른 사용자로 위장하여 답안 제출/조회 가능 | OAuth 토큰 기반 인증으로 memberId를 서버에서 추출 |

## Major

| 위치 | 문제 | 해결책 |
|------|------|--------|
| QuestionRepository.java:14 | **성능** — `ORDER BY RAND()`는 MySQL에서 전체 테이블 스캔 후 정렬 (O(n)). 데이터 증가 시 심각한 성능 저하 | 랜덤 offset 방식 또는 미리 생성된 랜덤 ID 테이블 활용 |
| QuestionGenerationService.java:85 | **N+1 쿼리** — 토픽별 `countBySubjectIdAndTopic()`을 루프 내에서 호출. 토픽 수만큼 쿼리 발생 | `GROUP BY topic` 단일 쿼리로 전체 카운트 조회 |
| QuestionGenerationService.java:58 | **문자 깨짐** — `"結果 저장 실패"` 중 일본어/중국어 혼입 | `"결과 저장 실패"`로 수정 |
| GenerationLockService.java:35,40,45 | **레이스 컨디션** — `acquire()`는 `findForUpdate()`로 잠금 획득하지만, `complete()`/`fail()`/`reset()`은 잠금 없이 `findById()` 사용 | `complete`/`fail`/`reset`에도 `findForUpdate()` 적용 |
| AdminGenerationController.java:43 | **입력 검증 부재** — `count` 파라미터에 음수, 0, 극단적 큰 값 전달 가능 | `@Min(1) @Max(10)` 등 Bean Validation 추가 |

## Minor

| 위치 | 문제 | 해결책 |
|------|------|--------|
| AiProvider.java:86-95 | **취약한 JSON 파싱** — 단순 `indexOf`로 JSON 추출. 중첩 구조나 문자열 내 `{`/`}` 시 오동작 가능 | 정규식 또는 AI 응답 포맷을 structured output으로 변경 |
| AiProvider.java:56 | **NPE 위험** — `root.get("approved")`가 null 반환 시 `.asBoolean()` 호출에서 NPE | `root.path("approved").asBoolean(false)` 사용 |
| JwtProvider.java:23 | **charset 미지정** — `secret.getBytes()` 기본 charset 의존. 환경마다 다를 수 있음 | `secret.getBytes(StandardCharsets.UTF_8)` 명시 |
| AdminAuthInterceptor.java:28,33 | **응답 본문 없음** — 401 상태만 반환하고 JSON body 없음. 클라이언트 디버깅 어려움 | `response.getWriter().write(...)` 로 에러 JSON 반환 |
| GenerationLockService.java:35,40,45,50 | **매직 넘버** — ID `1` 하드코딩. 의도 파악 어렵고 변경에 취약 | 상수 `LOCK_ID = 1` 추출 |
| QuestionGenerationService.java:125 | **Thread.sleep(10000)** — 10초 고정 대기. 가상 스레드에서도 비효율적 | 설정 가능한 백오프 전략 또는 제거 |
| QuestionGenerationService.java:52 | **ObjectMapper 매번 생성** — `new ObjectMapper()` 반복 호출 | 클래스 필드로 주입 또는 static final 상수화 |

## 총평

전반적으로 레이어 분리, 도메인 모델 불변성, JOIN FETCH 활용 등 아키텍처는 잘 설계되어 있음.
가장 시급한 문제는 `X-Member-Id` 헤더 기반 인증 방식으로, 클라이언트가 임의의 사용자로 위장할 수 있어 IDOR 취약점이 존재함.
성능 측면에서는 `RAND()` 쿼리와 N+1 문제를 우선 해결 권장.
