# Step 1 — 백엔드: FOCUS 플랜 + Thunder 리브랜드 + 라이브러리 권한 게이트

## 배경

구매 유도 강화: 새 플랜 Focus(2,900/30일) 신설, 기존 Starter(3,900/3일)를 Thunder(벼락치기)로 리브랜드 + 라이브러리/광고 권한 강화, 오답노트 무료 차단 + 즐겨찾기 30개 제한.

권한 매트릭스 (변경 후):
| 플랜 | 광고 제거 | PASS+ | 오답노트 | 즐겨찾기 | PDF |
|------|---------|------|---------|----------|-----|
| 무료 | ❌ | ❌ | ❌ | 30개 | ❌ |
| Thunder (THREE_DAY) | ✅ | ✅ | ✅ | 무제한 | ❌ |
| Focus (FOCUS) | ✅ | ❌ | ✅ | 무제한 | ❌ |
| Pro (ONE_MONTH) | ✅ | ✅ | ✅ | 무제한 | ❌ |
| Lifetime (UNLIMITED) | ✅ | ✅ | ✅ | 무제한 | ✅ |

오답노트 + 즐겨찾기 무제한을 단일 권한 `hasLibraryAccess` 로 묶어 단순화 (정책상 항상 동일 분기).

## 작업 디렉터리

```
backend/
```

## 변경 대상

### 1. `SubscriptionPlan` enum 확장

`backend/src/main/java/com/sqldpass/persistent/payment/SubscriptionPlan.java`:
- `THREE_DAY` 의 `removesAds` `false → true` (Thunder 리브랜드)
- `FOCUS(30, true, false, true, ?)` 4번째 enum 값 추가
- 새 필드 `hasLibraryAccess` (boolean) 도입 → free=false 외 모두 true
- 생성자 시그니처 확장: `(Integer days, boolean removesAds, boolean allowsPdf, boolean hasLibraryAccess, int tier)`
- tier: enum 리넘버 (THREE_DAY=1, FOCUS=2, ONE_MONTH=3, UNLIMITED=4)
- `isHasLibraryAccess()` getter 추가
- enum 키 `THREE_DAY` 그대로 유지 — DB/API 호환성

### 2. `PaymentProperties`

`backend/src/main/java/com/sqldpass/service/payment/PaymentProperties.java`:
- `private PlanConfig focus = new PlanConfig(2900, "문어CBT Focus");` 필드 + getter/setter
- `configFor(SubscriptionPlan)` switch 에 `case FOCUS -> focus;` 추가

### 3. `application.yaml`

`backend/src/main/resources/application.yaml`:
- `sqldpass.payment.three-day.product-name` 기본값 `"문어CBT Starter"` → `"문어CBT Thunder"`
- `sqldpass.payment.focus.{amount, product-name}` 추가 (기본 2900 / "문어CBT Focus")
- `sqldpass.play-billing.product-id-mapping.FOCUS: ${PLAY_BILLING_SKU_FOCUS:iap_focus}`

### 4. `SubscriptionService` 권한 메서드

`backend/src/main/java/com/sqldpass/service/payment/SubscriptionService.java`:
- `hasLibraryAccess(Long memberId)` 메서드 추가 — `getActive(memberId).map(s -> s.plan().isHasLibraryAccess()).orElse(false)`
- `ActiveSubscription` record 에 `boolean hasLibraryAccess` 필드 추가 + `PaymentController` 응답 매핑 보강

### 5. `ErrorCode` 추가

`backend/src/main/java/com/sqldpass/service/common/ErrorCode.java`:
- `WRONG_ANSWER_REQUIRES_SUBSCRIPTION("WRONG_ANSWER_REQUIRES_SUBSCRIPTION", "오답노트는 Thunder · Focus · Pro · Lifetime 에서 사용할 수 있어요.", HttpStatus.FORBIDDEN)` 추가

### 6. 오답노트 전체 잠금 + preview 엔드포인트

`backend/src/main/java/com/sqldpass/service/wronganswer/WrongAnswerService.java`:
- `getWrongAnswers()`, `getStats()`, `retry()` 진입부에 권한 가드 추가:
  ```java
  if (!subscriptionService.hasLibraryAccess(memberId)) {
      throw new SqldpassException(ErrorCode.WRONG_ANSWER_REQUIRES_SUBSCRIPTION);
  }
  ```
- 미리보기용 별도 메서드 `getWrongAnswersPreview(memberId, limit)` — 권한 가드 X. 본인 오답 상위 limit 개 반환. 단 `correctOption/answer/keywords/explanation` 제외 (제목·과목만).

`backend/src/main/java/com/sqldpass/controller/wronganswer/WrongAnswerController.java`:
- `GET /api/wrong-answers/preview?limit=5` 엔드포인트 신설. 권한 가드 X (로그인은 필요 — memberId).
- 응답 DTO: `WrongAnswerPreviewResponse` (questionId, questionContent, subjectName 만)

### 7. 즐겨찾기 30개 제한

위치: `backend/src/main/java/com/sqldpass/service/bookmark/` 또는 동등 위치 (실행 시 grep). 예상 컨트롤러: `BookmarkController`.

- 회원별 즐겨찾기 조회 시 `subscriptionService.hasLibraryAccess(memberId)` 가 false 면 `LIMIT 30` 적용
- 응답에 `limited: boolean` + `totalCount`, `freeLimit: 30` 메타 추가
- 31번째 즐겨찾기 추가/토글은 막지 않음 — 백엔드 저장 그대로

### 8. 결제 흐름 분기 점검

`PaymentService` / `PaymentController` / `AdminSubscriptionService` / `PaymentServiceTest`:
- `SubscriptionPlan` switch / 매핑 / 응답 DTO 에 FOCUS 누락 없는지 grep
- `evaluateUpgrade()` 의 tier 비교가 정수 1~4 로 잘 동작하는지 검증

### 9. 테스트

- `SubscriptionPlanTest` (있다면): 4 enum 각각 `removesAds`/`allowsPdf`/`hasLibraryAccess` 검증
- `SubscriptionServiceTest`: `hasLibraryAccess()` 메서드, `ActiveSubscription` 응답에 새 필드
- `PaymentServiceTest`: 기존 `"문어CBT Starter"` 어설션을 `"문어CBT Thunder"` 로 보정 + `setUp()` 의 `PlanConfig` 초기값도 보정. FOCUS 결제 시나리오 1개 추가 (prepare→verify→구독 발급)
- `WrongAnswerServiceTest`: 무료/Thunder 권한 분기 (Thunder 는 통과, 무료는 throw). preview 메서드는 권한 무관 동작
- `WrongAnswerControllerTest`: `/api/wrong-answers/preview` 엔드포인트 테스트
- 즐겨찾기 테스트: 무료 회원 31개 등록 후 조회 시 30개만 + `limited=true`

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
.\gradlew.bat test
```

## Acceptance Criteria

1. `SubscriptionPlan.FOCUS` enum 값 추가 + `hasLibraryAccess` 필드 도입.
2. THREE_DAY `removesAds=true`, `hasLibraryAccess=true` 로 변경.
3. application.yaml product-name "Starter" → "Thunder", focus 항목 추가.
4. `SubscriptionService#hasLibraryAccess` 메서드 + `ActiveSubscription` 새 필드 노출.
5. `/api/wrong-answers` 본 엔드포인트들에 권한 가드 + 새 ErrorCode.
6. `/api/wrong-answers/preview` 엔드포인트 신설, 응답에 정답/해설 제외.
7. 즐겨찾기 조회 시 무료/무권한 회원에게 30 limit + `limited` 메타.
8. `gradlew test` 전체 통과.

## 금지 사항

- `SubscriptionPlan` enum 키 `THREE_DAY` 자체 이름을 바꾸지 마라. **이유**: DB `subscription.plan` 컬럼이 enum name 으로 저장되어 있어 이름 변경 시 기존 row 가 매핑 안 됨. 라벨만 프론트에서 "Thunder" 로.
- 31번째 즐겨찾기 추가를 백엔드에서 차단하지 마라. **이유**: 결제 후 데이터 그대로 보여줘야 신뢰. 표시만 잘림.
- 오답노트 row 를 DB 에서 삭제하지 마라. **이유**: 동일.
- 광고 표시 로직(`AdDisplay.tsx`) 을 변경하지 마라. **이유**: 본 step 은 백엔드만. Thunder/Focus 가 `removesAds=true` 라 기존 분기 자동 처리.

## Status 규칙

- 성공: step 1 `completed`, summary "FOCUS 플랜 신설 + Thunder 리브랜드 + 라이브러리 권한 게이트 + 오답노트 preview 엔드포인트, gradlew test 통과".
- 실패: 3회 재시도 후 `error`.
