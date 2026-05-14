# Step 3 — 백엔드: Discord 결제 알림

## 배경

결제 완료 알림이 없어 운영자가 SQL 또는 어드민 새로고침으로 결제 발생을 추적해야 함. 기존 `DiscordNotifier`(이미 generation/signup/error/feedback 4채널 운영 중) 인프라를 그대로 확장 — `payment` 채널 1개 추가하고 `PaymentService.verify()` / `verifyPaymentById()` 결제 성공 후 비동기 발송.

카카오톡은 비즈채널/템플릿 승인 인프라 부재로 본 phase 제외.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `src/main/resources/application.yaml` | `sqldpass.discord.webhook.payment: ${DISCORD_WEBHOOK_PAYMENT:}` 추가 (기존 generation/signup/error/feedback 옆) |
| `.env.example` (있다면) | `DISCORD_WEBHOOK_PAYMENT=` 한 줄 추가 |
| `service/notification/DiscordNotifier.java` | `@Value("${sqldpass.discord.webhook.payment:}") private String paymentWebhook` + `notifyPaymentComplete(member, payment, sub)` 메서드 — 기존 signup 알림 코드 복제 후 필드만 교체 |
| `service/payment/PaymentService.java` | `verify(...)` 와 `verifyPaymentById(...)` 의 결제 성공 + subscription save 직후 `TransactionSynchronizationManager.registerSynchronization` 으로 커밋 후 `discordNotifier.notifyPaymentComplete(...)` 호출. try/catch 로 알림 실패 격리. |
| 신규 테스트 | `DiscordNotifierTest`: paymentWebhook 빈 문자열이면 send 호출 안 됨 (Mockito 검증). `PaymentServiceTest` 보강: verify 성공 시 notifyPaymentComplete 1회 호출(트랜잭션 커밋 시뮬레이션 없으면 단순 mock 검증으로 OK), 실패 시 비즈니스 결과 영향 없음. |

## application.yaml 추가

```yaml
sqldpass:
  discord:
    webhook:
      generation: ${DISCORD_WEBHOOK_GENERATION:}
      signup: ${DISCORD_WEBHOOK_SIGNUP:}
      error: ${DISCORD_WEBHOOK_ERROR:}
      feedback: ${DISCORD_WEBHOOK_FEEDBACK:}
      payment: ${DISCORD_WEBHOOK_PAYMENT:}    # ← 추가
```

## DiscordNotifier 메서드

```java
@Value("${sqldpass.discord.webhook.payment:}")
private String paymentWebhook;

public void notifyPaymentComplete(MemberEntity member, PaymentEntity payment, SubscriptionEntity sub) {
    if (paymentWebhook == null || paymentWebhook.isBlank()) return;

    String expiresLabel = sub.getExpiresAt() == null
        ? "무기한"
        : sub.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

    DiscordEmbed embed = DiscordEmbed.builder()
        .title("💳 결제 완료")
        .color(5763719) // 초록 0x57F287
        .field("회원", member.getNickname(), true)
        .field("플랜", payment.getPlan().name(), true)
        .field("금액", "₩" + String.format("%,d", payment.getAmount()), true)
        .field("결제수단", payment.getProvider().name(), true)
        .field("만료일", expiresLabel, true)
        .timestamp(Instant.now())
        .build();

    sendAsync(paymentWebhook, embed);
}
```

> `sendAsync` 가 기존 메서드(`CompletableFuture.runAsync` + try/catch + log) 이라고 가정. 같은 패턴 사용. `MemberEntity` 가 닉네임 없는 경우 "(이름 없음)" placeholder.

## PaymentService 훅

`verify(...)` 내부 결제 성공·subscription 저장 직후:

```java
// (subscription save 완료 후)
final MemberEntity memberSnapshot = memberRepo.findById(payment.getMemberId()).orElse(null);
final PaymentEntity paymentSnapshot = payment;
final SubscriptionEntity subSnapshot = sub; // 직전에 저장한 entity 참조

if (memberSnapshot != null && TransactionSynchronizationManager.isSynchronizationActive()) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            try {
                discordNotifier.notifyPaymentComplete(memberSnapshot, paymentSnapshot, subSnapshot);
            } catch (Exception e) {
                log.warn("결제 Discord 알림 발송 실패: paymentId={}", paymentSnapshot.getPaymentId(), e);
            }
        }
    });
} else if (memberSnapshot != null) {
    // 트랜잭션 외부(드문 케이스) — 즉시 발송. 실패는 격리.
    try {
        discordNotifier.notifyPaymentComplete(memberSnapshot, paymentSnapshot, subSnapshot);
    } catch (Exception e) {
        log.warn("결제 Discord 알림 발송 실패(no tx): paymentId={}", paymentSnapshot.getPaymentId(), e);
    }
}
```

`verifyPaymentById(...)` (모바일 PortOne redirectUrl 복귀 경로) 도 동일한 후처리.

> 트랜잭션 커밋 후 호출이 핵심. commit 전에 호출하면 rollback 시 "유령 결제 알림" 발생.

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `application.yaml` 에 `sqldpass.discord.webhook.payment` 항목 존재.
2. `DiscordNotifier.notifyPaymentComplete(...)` 구현, paymentWebhook blank → 즉시 return 검증.
3. `PaymentService.verify()` / `verifyPaymentById()` 성공 분기에서 notifier 호출 — 트랜잭션 커밋 후 (`afterCommit`).
4. 알림 발송 실패가 결제 결과/응답에 영향 없음 (try/catch + log only).
5. 신규 테스트 ≥ 2건 통과.
6. `gradlew test` 전체 통과.

## 금지 사항

- 트랜잭션 커밋 전에 알림을 발송하지 마라. **이유**: 결제 verify 가 마지막에 rollback 되면 결제 안 됐는데 Discord 만 도착 → 운영 혼란.
- 알림 실패를 결제 결과에 전파하지 마라. **이유**: Discord 다운 = 결제 실패라는 결합은 SPOF.
- 동기 HTTP 호출로 결제 응답을 지연시키지 마라. **이유**: 사용자 결제 latency 영향. 기존 `sendAsync` 비동기 패턴 그대로.
- `paymentWebhook` 이 비어도 예외 던지지 마라. **이유**: 로컬/CI 에서는 env 미설정이 정상. 조용히 스킵.
- 환불·실패 결제 알림은 본 step 에 추가하지 마라. **이유**: 결제 성공만 우선. 환불 알림은 후속 phase 에서 동일 패턴으로 확장.

## Status 규칙

- 성공: step 3 `completed`, summary "application.yaml payment webhook + DiscordNotifier.notifyPaymentComplete(blank 스킵) + PaymentService.verify/verifyPaymentById afterCommit 훅 + 테스트 2건+, test/compile OK".
- 실패: 3회 재시도 후 `error`.
