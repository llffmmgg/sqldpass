# Step 3 — PaymentFailureRecorder 신설 + markFailed 트랜잭션 분리

## 배경

P1-3: `PaymentService.verify` (`backend/src/main/java/com/sqldpass/service/payment/PaymentService.java:179, 186`) 에서 PortOne 검증 실패 시 `entity.markFailed(...)` 후 `throw` 한다. `@Transactional` 메서드가 throw 로 끝나면 Spring 이 롤백 → markFailed 의 status 변경이 flush 되지 않는다. 결과: 실패 결제 row 가 PENDING 으로 영구 잔존, 운영 추적 불가.

해결: `@Transactional(propagation = REQUIRES_NEW)` 헬퍼로 별도 트랜잭션에서 status=FAILED 를 flush한 뒤 throw.

**중요**: PaymentService 내부에 헬퍼 메서드를 만들고 `this.markFailedInNewTx(...)` 로 호출하면 Spring AOP proxy 를 거치지 않아 propagation 속성이 무시된다 — 별도 `@Component` 컴포넌트로 분리해야 한다 (`GenerationLockService` 도 같은 이유로 별 컴포넌트 분리).

## 작업 디렉터리

```
backend/
```

## 변경 대상

신규 1개:

| 파일 | 역할 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/payment/PaymentFailureRecorder.java` | REQUIRES_NEW 로 markFailed flush |

수정 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | 생성자에 PaymentFailureRecorder 주입 (이번 step 에서는 wiring 만 — 사용은 step 4 에서) |

## PaymentFailureRecorder 구조

```java
@Component
@RequiredArgsConstructor
public class PaymentFailureRecorder {
    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedInNewTx(Long paymentEntityId, String pgResponse) {
        paymentRepository.findById(paymentEntityId)
                .ifPresent(p -> p.markFailed(pgResponse));
    }
}
```

- 입력: `Long paymentEntityId` (DB row id) — String paymentId 가 아닌 DB pk 를 받는 이유: 호출부가 이미 entity 를 가지고 있으니 id 만 넘기는 게 fetch 비용이 적고 명확.
- entity 가 사라진 케이스(이론상 동시 삭제) 는 ifPresent 로 swallow.

## PaymentService 수정 (이번 step 한정)

`PaymentService` 의 생성자(@RequiredArgsConstructor 라 자동) 에 `PaymentFailureRecorder failureRecorder` 필드를 추가한다. 본 step 에서는 호출부 변경 없음 — step 4 에서 verify 안의 두 throw 경로에 호출 삽입.

테스트는 step 4 에서 함께. 본 step 은 wiring + 컴파일만 검증.

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
.\gradlew.bat test
```

기존 PaymentServiceTest 가 `new PaymentService(properties, portOneClient, paymentRepository, subscriptionRepository, memberRepository, playBillingClient, playBillingProperties)` 로 인스턴스를 만든다 — 새 파라미터 추가에 따라 테스트의 setUp() 도 같이 수정해야 한다 (`PaymentFailureRecorder` mock 추가).

## Acceptance Criteria

1. `PaymentFailureRecorder` 가 `@Component` + `@Transactional(propagation = Propagation.REQUIRES_NEW)` 로 정의된다.
2. `markFailedInNewTx(Long, String)` 시그니처 (entity id 기반).
3. `PaymentService` 의 final 필드 + 생성자에 `failureRecorder` 가 추가된다 (Lombok @RequiredArgsConstructor 라 필드 추가만으로 OK).
4. `PaymentServiceTest.setUp()` 에 `@Mock PaymentFailureRecorder failureRecorder` 가 추가되고, `new PaymentService(..., failureRecorder)` 에 인자가 들어간다.
5. `gradlew.bat compileJava` + `gradlew.bat test` 통과 (기존 테스트 회귀 없음).

## 금지 사항

- PaymentService 내부에 `@Transactional(propagation = REQUIRES_NEW)` private 메서드를 만들지 마라. 이유: 자기 호출 시 proxy 우회 → propagation 무력화.
- markFailedInNewTx 를 verify 호출부에서 본 step 에 사용하지 마라. 이유: step 4 의 가드 로직과 함께 변경하면 diff 가독성이 떨어진다 — 본 step 은 wiring 만.
- PaymentRepository 에 새 메서드를 추가하지 마라. 이유: `findById` 만으로 충분, 추가 메서드는 사용처가 없다.

## Status 규칙

- 성공: step 3 `completed`, summary 에 "PaymentFailureRecorder REQUIRES_NEW 컴포넌트 신설, PaymentService wiring + 기존 테스트 setUp 보강 OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: 트랜잭션 매니저 설정에 사용자 결정 필요 시 `blocked`.
