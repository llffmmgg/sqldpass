# Step 3 — AppRepository.drainPendingSolves 단위 테스트

## 작업 디렉터리
`mobile/`

## 배경 / Why
- 오프라인 큐 동작은 풀이 데이터 유실/중복 방지의 핵심 — clientSubmissionId 기반 idempotency, FIFO 순서, 일부 실패 continue 동작 모두 회귀 위험 큼.

## 변경 대상

### 1. `mobile/app/src/test/java/com/sqldpass/app/data/AppRepositoryTest.kt`
테스트 시나리오:

1. **로그아웃 상태 (tokenStore.token == null) → 0 반환, API 호출 안 됨**
   - dao.unsyncedSolves() 가 3 row 반환해도 API 호출 verify { 0 * any() }.

2. **3 row 모두 성공 → API 3번 호출, 모두 markSolveSynced, return 3**
   - dao 가 mockExamId=1,2,3 인 row 3개 (createdAtMillis ASC) 반환.
   - api.submitSolve 가 모두 정상 응답.
   - dao.markSolveSynced 가 정확히 3번 호출되고, 호출 순서가 createdAt 순.

3. **중간 row 실패 → 다음 row 계속, return 성공 카운트**
   - row 2 의 submit 이 throw.
   - row 1, 3 은 정상 → markSolveSynced 2번, return 2.

4. **solo 풀이 (mockExamId == SOLO_PENDING_MOCK_EXAM_SENTINEL) → SoloPendingPayload 디코드 + subjectId 사용**
   - mockExamId = -1L, answersJson 이 SoloPendingPayload JSON.
   - api.submitSolve 호출 시 request.subjectId 채워지고 mockExamId null.

5. **잘못된 JSON → 해당 row skip, 나머지 처리, 카운트는 성공 개수만**
   - SoloPendingPayload 디코드 실패 row 1개 + 정상 row 2개 → return 2.

### 2. Mocking 전략
- `SqldpassApi` — mockk
- `SqldpassDatabase.solveDao()` 또는 동등 DAO — mockk. unsyncedSolves() 가 Flow 가 아니라 suspend list 라면 단순.
- `TokenStore` — mockk

### 3. test runner
- `runBlocking` 으로 suspend 메서드 직접 호출. coroutines-test 의 TestScope 도 가능하지만 본 케이스는 단순.

## 작업 절차
1. AppRepository 의 drainPendingSolves 호출에 필요한 모든 의존성 식별.
2. AppRepositoryTest.kt 작성 — 5 시나리오.
3. 테스트 통과.

## 검증
```powershell
cd C:\\Users\\admin\\desktop\\sqldpass\\sqldpass\\mobile
.\\gradlew.bat :app:testDebugUnitTest --tests "com.sqldpass.app.data.AppRepositoryTest"
```

## 금지사항
- 실제 Room 인스턴스 만들기 금지. 이유: unit test 영역이라 SQLiteDatabase 못 만듦. DAO mock 만 사용.
- drainPendingSolves 외 다른 메서드 테스트 추가하지 말 것. 이유: 본 step 의 스코프 한정.

## 산출물
- 신규 test 파일 + 시나리오 수.
- 전체 mobile 테스트 한 번에 실행 결과 (`gradlew :app:testDebugUnitTest`) 마지막 10줄.
