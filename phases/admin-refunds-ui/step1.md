# Step 1 — 백엔드: 결제 목록 endpoint + reason @NotBlank

## 배경

어드민 환불 UI 가 결제 목록을 조회해 환불 대상을 선택할 수 있도록 `GET /api/admin/payments` 신설. 동시에 환불 사유 입력 누락을 막기 위해 기존 `RefundRequest.reason` 을 `@NotBlank` 강제.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `controller/admin/AdminPaymentRow.java` (신규) | 결제 목록 응답 record — id/paymentId/memberId/nickname/plan/amount/baseAmount/prorateDiscount/status/provider/buyer3/paidAt/createdAt |
| `persistent/payment/PaymentRepository.java` | `findAdminPage` 커스텀 쿼리 — Member LEFT JOIN, status/provider/nickname/paymentId LIKE 필터, paidAt DESC |
| `controller/admin/AdminPaymentController.java` | `GET /` 신규 endpoint (page/size/status/provider/nickname/paymentId 파라미터, size Math.min(100)). `RefundRequest.reason` 에 `@NotBlank @Size(max=200)` + refund 메서드에 `@Valid` |
| `AdminPaymentControllerTest` (보강) | list 기본 페이지 / status 필터 / provider 필터 / nickname LIKE 필터 / refund reason 빈문자열 400 검증 |

## findAdminPage JPQL

```java
@Query("""
        SELECT new com.sqldpass.controller.admin.AdminPaymentRow(
                p.id, p.paymentId, p.memberId, m.nickname,
                p.plan, p.amount, p.baseAmount, p.prorateDiscount,
                p.status, p.provider,
                p.buyerName, p.buyerEmail, p.buyerPhoneNumber,
                p.paidAt, p.createdAt
        )
        FROM PaymentEntity p LEFT JOIN MemberEntity m ON m.id = p.memberId
        WHERE (:status IS NULL OR p.status = :status)
          AND (:provider IS NULL OR p.provider = :provider)
          AND (:nickname IS NULL OR m.nickname LIKE %:nickname%)
          AND (:paymentIdLike IS NULL OR p.paymentId LIKE %:paymentIdLike%)
        ORDER BY p.paidAt DESC, p.id DESC
        """)
Page<AdminPaymentRow> findAdminPage(@Param("status") PaymentStatus status,
                                    @Param("provider") PaymentProvider provider,
                                    @Param("nickname") String nickname,
                                    @Param("paymentIdLike") String paymentIdLike,
                                    Pageable pageable);
```

MySQL 은 `DESC` 정렬에서 NULL 을 마지막에 두므로 `NULLS LAST` 명시 불필요.

## RefundRequest 변경

```diff
- public record RefundRequest(String reason) {}
+ public record RefundRequest(
+         @NotBlank(message = "환불 사유는 필수입니다.")
+         @Size(max = 200, message = "환불 사유는 200자 이내로 입력해주세요.")
+         String reason
+ ) {}
```

`refund` 메서드 시그니처에 `@Valid` 추가. `body` 가 null 이면 `@NotBlank` 가 동작 못 하므로 body=null 가드 유지하되 reason=null 가드는 Bean Validation 으로 위임.

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `GET /api/admin/payments` 가 `Page<AdminPaymentRow>` 반환, 4종 필터 동작.
2. Member 없는 결제는 nickname=null 로 반환 (LEFT JOIN).
3. `RefundRequest.reason` 이 `@NotBlank @Size(max=200)` 검증, `refund` 에 `@Valid` 적용.
4. 신규 테스트 5건 통과.
5. `gradlew test` 전체 통과.

## 금지 사항

- 결제 목록 조회를 `PaymentService` 에 추가하지 마라. **이유**: 단순 read-only 페이지네이션 — Repository → Controller 직결로 충분.
- `refund` 메서드의 path/body 시그니처를 바꾸지 마라. **이유**: 기존 호출 경로 회귀 차단.
- buyer 정보를 마스킹하지 마라. **이유**: 어드민 화면 전용 + CS 시 원본 필요. 마스킹은 별도 phase.
- `Math.min(size, 100)` 가드를 빼지 마라. **이유**: 대용량 페이지 요청 시 메모리/렌더 부담.

## Status 규칙

- 성공: step 1 `completed`, summary "AdminPaymentRow + findAdminPage + GET /api/admin/payments + RefundRequest @NotBlank + 테스트 5건, test+compile OK".
- 실패: 3회 재시도 후 `error`.
