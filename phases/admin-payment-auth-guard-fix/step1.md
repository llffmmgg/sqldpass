# Step 1 — AdminPaymentController 가드 제거 + 테스트 정리

## 배경

`/admin/refunds` 진입 후 결제 목록 조회 시 401 → adminFetch 가 토큰 clear + `/admin/login` 으로 강제 리다이렉트. admin 로그인은 정상인데도 무한 루프.

원인:
- `AdminAuthController.login` 이 토큰 발급 시 `jwtProvider.createToken(adminUsername)` — subject = "admin" 문자열
- `JwtProvider.extractMemberId` 는 `Long.valueOf(claims.getSubject())` — admin 토큰엔 NumberFormatException
- `AdminAuthInterceptor` 는 그래서 token validate 만 하고 `memberId` attribute 를 의도적으로 set 안 함
- 그런데 `AdminPaymentController.list/refund/reissue` 3개 메서드가 `if (request.getAttribute("memberId") == null) throw UNAUTHORIZED` 가드를 가짐 → 항상 401

`AdminSubscriptionController.grant/expire` 는 같은 attribute 받지만 가드 없이 `(Long) null` 캐스팅으로 actorAdminId=null 통과 → 정상 동작. `subscription_history.actor_admin_id BIGINT NULL` (V81) 이라 null 저장 안전.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/controller/admin/AdminPaymentController.java` | list/refund/reissue 의 `if (memberId == null) throw UNAUTHORIZED` 가드 3개 제거 + 클래스 주석에 인증 정책 의도 명시 |
| `backend/src/test/java/com/sqldpass/controller/admin/AdminPaymentControllerTest.java` | list/refund/reissue 미인증 401 케이스 3건 삭제 |

## AdminPaymentController 변경

클래스 javadoc:

```java
/**
 * 관리자 결제 — 환불 / 재발급 / 목록 조회.
 *
 * <p>인증 보호는 {@link com.sqldpass.config.AdminAuthInterceptor} 가 {@code /api/admin/**}
 * 경로에 일괄 적용. AdminAuthInterceptor 는 admin 토큰을 validate 만 하고 memberId attribute
 * 를 set 하지 않으므로(admin 토큰 subject 는 username 문자열) 본 컨트롤러의 actorAdminId 는
 * null 일 수 있다. subscription_history.actor_admin_id 가 nullable (V81) 이라 안전, 운영
 * 추적은 history.reason 텍스트로 보존.
 */
```

list/refund/reissue 의 가드 제거:

```diff
  public Page<AdminPaymentRow> list(..., HttpServletRequest request) {
-     if (request.getAttribute("memberId") == null) {
-         throw new SqldpassException(ErrorCode.UNAUTHORIZED);
-     }
      String nick = ...;

  public RefundResponse refund(..., HttpServletRequest request) {
      Long actorAdminId = (Long) request.getAttribute("memberId");
-     if (actorAdminId == null) {
-         throw new SqldpassException(ErrorCode.UNAUTHORIZED);
-     }
      paymentService.revokePortOnePayment(paymentId, body.reason(), actorAdminId);

  public ReissueResponse reissue(..., HttpServletRequest request) {
      Long actorAdminId = (Long) request.getAttribute("memberId");
-     if (actorAdminId == null) {
-         throw new SqldpassException(ErrorCode.UNAUTHORIZED);
-     }
      PaymentService.ReissueResult result = ...;
```

`actorAdminId` 변수는 그대로 (service 인자 시그니처라 유지).

## AdminPaymentControllerTest 변경

삭제 대상 3건:
- `refund_미인증_시_401`
- `reissue_미인증_시_401`
- `list_미인증_시_401`

이유: `@WebMvcTest` 는 인터셉터를 로드 안 함 → controller 단 가드 없이는 미인증 검증 불가. 인증 보호는 `AdminAuthInterceptor` integration test 영역.

남는 테스트 — `refund_정상_200`, `refund_빈_reason_400`, `refund_긴_reason_400`, `reissue_정상_200`, `reissue_service_INVALID_INPUT_400`, `list_기본_정상`, `list_status_필터_전달`, `list_nickname_paymentId_trim_및_빈문자열_null`, `list_size_상한_및_음수_page_보정` — 그대로 유지. `requestAttr("memberId", 7L)` 명시 주입은 정상 케이스에서 그대로 동작 (Long cast 통과).

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. AdminPaymentController 의 list/refund/reissue 에 `memberId == null` 가드가 없다.
2. 클래스 javadoc 에 인증 정책 의도 명시 (AdminAuthInterceptor 가 보호 + actorAdminId null 가능).
3. AdminPaymentControllerTest 의 미인증 401 케이스 3건 삭제.
4. 남은 테스트 모두 통과.
5. `gradlew test` 전체 통과 + `compileJava` 성공.

## 금지 사항

- AdminAuthInterceptor 에 `setAttribute("memberId", ...)` 추가하지 마라. **이유**: admin 토큰 subject 가 username 문자열이라 extractMemberId 가 NumberFormatException. 별도 admin id 발급 정책 변경 필요 — 본 plan 범위 밖.
- `actorAdminId` 변수 자체를 제거하지 마라. **이유**: service 시그니처 인자라 더 큰 변경.
- AdminSubscriptionController 패턴을 바꾸지 마라. **이유**: 이미 정상 동작. 일관성 회복이 본 plan 목적.
- `subscription_history.actor_admin_id` 컬럼을 NOT NULL 변경하지 마라. **이유**: admin 토큰 정책상 null 정상.
- 다른 admin controller (members/feedback/posts 등) 를 함께 수정하지 마라. **이유**: 본 버그는 actorAdminId + null 가드 조합이 있는 곳에서만. 그 외는 영향 없음.

## Status 규칙

- 성공: step 1 `completed`, summary "AdminPaymentController list/refund/reissue 의 memberId null 가드 3개 제거 + 클래스 주석 보강 + 테스트 미인증 401 케이스 3건 삭제, test+compile OK".
- 실패: 3회 재시도 후 `error`.
