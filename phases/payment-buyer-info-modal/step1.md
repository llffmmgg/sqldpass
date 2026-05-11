# Step 1 — 백엔드: 정리 + payment buyer 컬럼 + API 계약

## 배경

KG이니시스 PortOne V2 PC 일반결제 customer 필수 필드를 payment 테이블에 저장. 직전 phase 의 `member.email` 정책은 새 phase 안에서 정리(V83 DROP) 한다.

## 작업 디렉터리

```
backend/
```

## 변경 대상

### 정리 (이전 phase 롤백)
| 파일 | 변경 |
|------|------|
| `MemberEntity.java` | `email` 필드, `updateEmail()`, 4-인자 생성자 제거 |
| `GoogleOAuthClient.java` | `GoogleUserInfo(sub, name)` 회귀, `extractVerifiedEmail` 헬퍼 삭제, getUserInfo/verifyIdToken 단순화 |
| `AuthService.java` | upsert 신규 분기 — 3-인자 생성자, 기존 분기 — `member.updateEmail` 호출 제거 |
| `PaymentService.java` | `PreparePaymentResult.customerEmail` 제거, prepare 의 customerEmail 추출 로직 제거 |
| `MemberEntityEmailTest.java` | 삭제 |
| `AuthServiceEmailTest.java` | 삭제 |
| `PaymentServiceTest.java` | customerEmail 케이스 2건 삭제 |

### 신규 (buyer 추가)
| 파일 | 변경 |
|------|------|
| `V83__drop_member_email.sql` (신규) | `ALTER TABLE member DROP COLUMN email` |
| `V84__add_payment_buyer_info.sql` (신규) | `ALTER TABLE payment ADD COLUMN buyer_name, buyer_email, buyer_phone_number` (모두 nullable) |
| `PaymentEntity.java` | buyer 3 필드 + buyer 포함 생성자 (기존 8/10-인자 생성자 옆에 11-인자 또는 등가 추가) |
| `PaymentController.PrepareRequest` | plan 외에 buyerName/Email/PhoneNumber + Bean Validation |
| `PaymentService.prepare` | 시그니처 확장 — buyer 받아 PaymentEntity 에 저장, normalizePhone |
| 테스트 신규 | `prepareSavesBuyerInfo`, `prepareNormalizesPhone` |

## V83 마이그레이션

```sql
-- V83__drop_member_email.sql
-- V82 (member.email) 정리 — 회원 도메인에 결제용 PII 미저장 정책으로 회귀.
-- buyer 정보는 V84 에서 payment 테이블에 신규 저장.
ALTER TABLE member DROP COLUMN email;
```

## V84 마이그레이션

```sql
-- V84__add_payment_buyer_info.sql
-- KG이니시스 PortOne V2 PC 일반결제 customer 필수 필드 보관용.
-- 결제 시점에 사용자가 모달에 입력한 정보를 PaymentEntity 에 동봉 저장.
-- 기존 row 는 NULL 유지 (이전 결제 영향 없음).
ALTER TABLE payment
    ADD COLUMN buyer_name         VARCHAR(50)  NULL AFTER member_id,
    ADD COLUMN buyer_email        VARCHAR(255) NULL AFTER buyer_name,
    ADD COLUMN buyer_phone_number VARCHAR(20)  NULL AFTER buyer_email;
```

## PaymentEntity 변경

새 필드 (member_id 다음 위치 권장):
```java
@Column(name = "buyer_name", length = 50)
private String buyerName;

@Column(name = "buyer_email", length = 255)
private String buyerEmail;

@Column(name = "buyer_phone_number", length = 20)
private String buyerPhoneNumber;
```

생성자: 기존 8-인자 생성자에 buyer 3 인자 추가한 11-인자 생성자 오버로드. 기존 생성자는 호환성 유지.

## PrepareRequest 변경

```java
public record PrepareRequest(
        @NotNull SubscriptionPlan plan,
        @NotBlank @Size(max = 50) String buyerName,
        @NotBlank @Email @Size(max = 255) String buyerEmail,
        @NotBlank
        @Pattern(regexp = "^01[0-9][-\\s]?\\d{3,4}[-\\s]?\\d{4}$",
                 message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String buyerPhoneNumber
) {}
```

PaymentController prepare 메서드는 `@Valid` 추가 — `public PreparePaymentResult prepare(@Valid @RequestBody PrepareRequest body, ...)`.

## PaymentService.prepare 변경

```java
public PreparePaymentResult prepare(Long memberId, SubscriptionPlan plan,
                                    String buyerName, String buyerEmail, String buyerPhoneNumber) {
    // ... 기존 ensureReviewer, plan 검증, eval, finalAmount 계산
    PaymentEntity entity = new PaymentEntity(paymentId, memberId, null,
            productName, plan, finalAmount, baseAmount, eval.discount(),
            buyerName, buyerEmail, normalizePhone(buyerPhoneNumber));
    paymentRepository.save(entity);
    return new PreparePaymentResult(
            paymentId, finalAmount, productName, plan,
            properties.getPortone().getStoreId(),
            baseAmount, eval.discount());  // customerEmail 제거
}

private static String normalizePhone(String raw) {
    return raw == null ? null : raw.replaceAll("[-\\s]", "");
}
```

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. V83, V84 적용 후 `ddl-auto: validate` 통과.
2. MemberEntity 에 email 필드/메서드/4-인자 생성자가 없다.
3. GoogleUserInfo 가 (sub, name) 으로 회귀.
4. PaymentEntity 가 buyer 3 필드 + 11-인자 생성자를 가진다.
5. PrepareRequest 의 buyer 3 필드가 Validation 어노테이션으로 검증, @Valid 적용.
6. PaymentService.prepare 가 buyer 정보 받아 저장, 휴대폰 정규화.
7. PreparePaymentResult.customerEmail 제거됨.
8. `gradlew test` 전체 통과 (직전 phase 테스트 14건 삭제 후 신규 2건 추가).

## 금지 사항

- V82 마이그레이션 파일을 수정하지 마라. **이유**: Flyway 는 적용된 마이그레이션 수정 금지. 컬럼 DROP 은 새 V83 으로만.
- MemberEntity 에 다시 email 을 추가하지 마라. **이유**: 정책 회귀. 결제용 PII 는 payment 테이블만.
- buyer 컬럼을 NOT NULL 로 추가하지 마라. **이유**: 기존 payment 행이 NULL 이라 V84 가 실패.
- PortOne customer 정보를 백엔드 검증 없이 프론트만 받게 두지 마라. **이유**: Bean Validation + 영구 저장이 정합성·CS 의 근거.
- normalizePhone 을 view/DTO 단계에서 하지 마라. **이유**: 서비스 안에서 저장 직전 정규화 — 단일 진입점.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "V83/V84 + PaymentEntity buyer + PrepareRequest Validation + prepare 시그니처 확장 + 정리 + 테스트 2건, test+compile OK".
- 실패: 3회 재시도 후 `error`.
