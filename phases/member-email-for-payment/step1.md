# Step 1 — 백엔드: email 컬럼 + OAuth + 결제 응답

## 배경

KG이니시스 신용카드 결제에 PortOne `customer.email` 필드가 필요하다. 현재 `MemberEntity` 에는 email 컬럼이 없어 (V10 에서 DROP), 다시 수집해야 한다.

정책 (사용자 확정):
- email_verified=true 일 때만 저장, false 면 NULL 유지
- email UNIQUE 제약 안 둠 (sub 가 유일 식별자)
- 기존 회원은 다음 로그인 시 자연 백필
- CARD 결제만 email 필수 (KAKAOPAY 는 영향 없음)

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `backend/src/main/resources/db/migration/V82__add_member_email.sql` (신규) | `ALTER TABLE member ADD COLUMN email VARCHAR(255) NULL AFTER nickname` |
| `backend/src/main/java/com/sqldpass/persistent/member/MemberEntity.java` | email 필드 + updateEmail 도메인 메서드 + 4-인자 생성자 |
| `backend/src/main/java/com/sqldpass/service/auth/GoogleOAuthClient.java` | GoogleUserInfo record 에 email 추가, getUserInfo / verifyIdToken 에서 email_verified=true 시에만 email 추출 |
| `backend/src/main/java/com/sqldpass/service/auth/AuthService.java` | upsert 신규 분기 — 4-인자 생성자 / 기존 분기 — updateEmail 호출 |
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | PreparePaymentResult 에 customerEmail 필드 추가 + prepare 가 회원 조회해서 채움 |

테스트:
- `MemberEntityEmailTest` (신규) — updateEmail 멱등성 (null/blank/동일 무동작) + 변경 시 반영
- `AuthServiceEmailTest` (신규) — 신규 가입 시 email 저장 / 재로그인 시 NULL → 채움 / 동일 → save 없음
- `GoogleOAuthClient` 직접 테스트는 외부 HTTP 의존이라 SKIP. 단위 테스트는 AuthService 에서 mock 으로 커버.
- `PaymentServiceTest` — prepare 응답에 customerEmail 포함 확인 (회원 email 있는 경우 / NULL 인 경우 양쪽)

## V82 마이그레이션

```sql
-- V82__add_member_email.sql
-- KG이니시스 신용카드 결제 customer.email 수집용. V10 에서 DROP 됐던 컬럼 재추가.
-- nullable + UNIQUE 없음 — 기존 회원은 다음 로그인 시 점진적 백필.
ALTER TABLE member ADD COLUMN email VARCHAR(255) NULL AFTER nickname;
```

## MemberEntity 변경

`nickname` 다음 필드:
```java
@Column(name = "email", length = 255)
private String email;
```

도메인 메서드 (changeNickname 옆):
```java
/**
 * email_verified=true 인 경우에만 호출 전제. null/blank/동일 시 무동작 →
 * JPA dirty checking 에 의한 UPDATE 쿼리 생략.
 */
public void updateEmail(String email) {
    if (email == null || email.isBlank()) return;
    if (email.equals(this.email)) return;
    this.email = email;
}
```

4-인자 생성자 (기존 3-인자도 유지):
```java
public MemberEntity(String provider, String providerId, String nickname, String email) {
    this(provider, providerId, nickname);
    this.email = email;  // null 허용
}
```

## GoogleOAuthClient 변경

`GoogleUserInfo` record 시그니처 확장:
```java
public record GoogleUserInfo(String sub, String name, String email) {}
```

`getUserInfo` 와 `verifyIdToken` 각각에 email 추출 헬퍼:
```java
private static String extractVerifiedEmail(JsonNode response) {
    if (response.has("email") && response.has("email_verified")
            && response.get("email_verified").asBoolean()) {
        return response.get("email").asText();
    }
    return null;
}
```

`getUserInfo` 끝부분:
```java
String email = extractVerifiedEmail(response);
return new GoogleUserInfo(sub, name, email);
```

`verifyIdToken` 끝부분도 동일.

⚠️ ID token (tokeninfo 엔드포인트) 의 `email_verified` 는 종종 문자열 `"true"` 로 옵니다. `asBoolean()` 이 문자열도 처리하는지 확인 — JsonNode.asBoolean() 은 "true" 문자열을 true 로 해석함. 안전하게 동작.

## AuthService 변경

`upsertMemberAndIssueToken`:
```java
MemberEntity member = memberRepository.findByProviderAndProviderId("google", userInfo.sub())
        .orElseGet(() -> {
            isNew[0] = true;
            String placeholder = "user_" + userInfo.sub().substring(0, Math.min(12, userInfo.sub().length()));
            return memberRepository.save(
                    new MemberEntity("google", userInfo.sub(), placeholder, userInfo.email()));
        });

// 기존 회원 — verified email 받았고 DB 와 다르면 갱신
if (!isNew[0] && userInfo.email() != null) {
    member.updateEmail(userInfo.email());
    // @Transactional + dirty checking → 트랜잭션 종료 시 자동 UPDATE
}
```

## PaymentService 변경

`PreparePaymentResult` 에 `customerEmail` 추가:
```java
public record PreparePaymentResult(String paymentId, int amount, String productName,
                                   SubscriptionPlan plan, String storeId,
                                   int baseAmount, int prorateDiscount,
                                   String customerEmail) {}
```

`prepare()` 메서드 안에서 회원 조회 → email 채움:
```java
// (paymentRepository.save 직후, log/return 직전)
String customerEmail = memberRepository.findById(memberId)
        .map(MemberEntity::getEmail)
        .orElse(null);

return new PreparePaymentResult(
        paymentId, finalAmount, productName, plan,
        properties.getPortone().getStoreId(),
        baseAmount, eval.discount(),
        customerEmail);
```

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `V82__add_member_email.sql` 가 추가되어 `ddl-auto: validate` 가 통과한다.
2. `MemberEntity.email` 필드와 `updateEmail(String)` 메서드, 4-인자 생성자가 존재한다.
3. `GoogleUserInfo` record 가 `(sub, name, email)` 3개 필드를 가진다.
4. `getUserInfo` 와 `verifyIdToken` 모두 `email_verified=true` 일 때만 email 을 채운다.
5. `AuthService.upsertMemberAndIssueToken` 신규 분기에서 email 이 같이 저장되고, 기존 분기에서 updateEmail 이 호출된다.
6. `PreparePaymentResult` 에 `customerEmail` 필드가 추가된다.
7. 단위 테스트 신규 추가분이 통과한다.
8. `.\gradlew.bat test` 와 `.\gradlew.bat compileJava` 모두 성공.

## 금지 사항

- email 컬럼에 UNIQUE 제약을 추가하지 마라. 이유: 사용자 결정. `sub` 가 유일 식별자이고, UNIQUE 두면 백필 시 충돌 위험.
- email NULL 시 prepare 를 차단(throw)하지 마라. 이유: 차단은 프론트에서 결제수단 별로 분기. 백엔드는 단순히 null 반환.
- `member.updateEmail()` 후 명시적 `save()` 를 호출하지 마라. 이유: `@Transactional` + JPA dirty checking 으로 자동 UPDATE. 명시 save 는 노이즈.
- `email_verified` 없이 그냥 email 만 저장하지 마라. 이유: 미인증 이메일은 다른 사람 소유 가능성. 영수증 오발송 리스크.
- 다른 기능(닉네임 변경, 결제 흐름 등)을 함께 수정하지 마라. 이유: 변경 범위 최소화, 회귀 위험 회피.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "V82 + MemberEntity.email + GoogleOAuthClient email_verified + AuthService 분기 + PreparePaymentResult customerEmail, test+compile OK".
- 실패: 3회 재시도 후 실패면 `error`.
