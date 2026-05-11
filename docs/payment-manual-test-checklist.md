# 결제 수동 회귀 체크리스트

> 최종 업데이트: 2026-05-11
> 대상: QA / 운영자
> 목적: 단위·통합 테스트로 잡히지 않는 결제 흐름(프론트 이탈, PG 측 상태, 권한 부여, 환불 동기화)을 정기적으로 수동 회귀하기 위한 체크리스트.

본 문서는 sqldpass 결제 시스템(PortOne v2 KakaoPay + Play Billing)에 대한 9개 핵심 시나리오를 담고 있다. 각 시나리오는 체크박스 형태이므로 PR/Confluence/Notion 에 그대로 붙여 사용한다.

---

## 1. PortOne 테스트 모드 제약 사항 (사전 숙지)

운영 결제 플로우와 테스트 결제 플로우의 동작이 다르다. 테스트 시작 전 다음을 반드시 확인한다.

### 1-1. 테스트 모드 PG 정책

- **카카오페이 (테스트 모드)**: 실제 카카오페이 계정/카드를 사용하지 않는다. 임시로 발급되는 카카오페이 머니 또는 테스트용 카드 정보로 결제가 시뮬레이션된다. 실 결제는 발생하지 않는다.
- **KB국민카드**: PortOne 테스트 모드에서는 결제 불가. 운영 환경에서만 결제 가능. 테스트 시 다른 카드사로 검증한다.
- **결제 취소(이탈)**: 카카오페이 결제창에서 X 버튼으로 닫으면 PortOne SDK 가 `code=PAYMENT_USER_CANCELED` 류 코드를 반환한다.

### 1-2. 환경 변수 확인

| 위치 | 변수 | 용도 |
|------|------|------|
| frontend `.env.local` | `NEXT_PUBLIC_PORTONE_STORE_ID` | PortOne store 식별자 (`store-{uuid}`) |
| frontend `.env.local` | `NEXT_PUBLIC_PORTONE_CHANNEL_KEY` | PortOne 채널 키 |
| frontend `.env.local` | `NEXT_PUBLIC_PLAY_BILLING_SKU_THREE_DAY` 외 2종 | Play Billing SKU 오버라이드 (선택) |
| backend env | `PAYMENT_REVIEWER_NICKNAMES` | 화이트리스트 닉네임. 비어 있으면 전체 공개. comma-separated, 공백 없음 |
| backend env | `PORTONE_*` 시크릿 | PortOne API key, secret. 마스킹된 값으로 운용 |

### 1-3. 화이트리스트 게이트

- `PAYMENT_REVIEWER_NICKNAMES` 가 설정된 환경(MVP / staging)에서는 해당 닉네임 회원만 `/checkout` 진입 가능. 다른 회원은 `/api/payment/eligibility` 에서 `eligible=false` 응답.
- 비어 있으면(공개 정책) 모든 회원이 진입 가능.

### 1-4. 테스트 계정 준비

- 테스트 시작 전 다음 중 1개 이상 계정 준비:
  - 화이트리스트 등록된 Google 계정 (테스트용)
  - 활성 구독이 없는 계정 (없거나 만료된 상태)
  - 활성 구독이 있는 계정 (업그레이드/prorate 검증용)

---

## 2. 9 시나리오 체크리스트

### S1. 결제 성공 (Happy path)

- [ ] **사전 조건**: 로그인 상태, 활성 구독 없음, 화이트리스트 통과 (또는 공개 정책 환경).
- [ ] **액션**:
  - [ ] `/checkout` 진입
  - [ ] THREE_DAY 플랜 카드 클릭 → PortOne 결제창 → KAKAOPAY 선택 → 테스트 머니로 결제 완료
  - [ ] 같은 흐름으로 ONE_MONTH, UNLIMITED 도 각각 별도 계정/만료 후 검증
- [ ] **기대**:
  - [ ] 토스트: `"3일 이용권 결제 완료"` (UNLIMITED 는 `"평생 무제한 결제 완료"`) — success 톤
  - [ ] 약 0.8초 후 `/mock-exams` 로 자동 리다이렉트
  - [ ] `GET /api/payment/subscription` 호출 시 `active=true`, `plan` 정확, `expiresAt` 정확 (UNLIMITED 는 `null`)
- [ ] **DB 검증**:
  - [ ] `payment` 테이블: `status=PAID`, `paid_at != null`, `amount` = 정가 (3,900 / 9,900 / 29,900)
  - [ ] `subscription` 테이블: row 1개, `payment_id` = 위 payment.id, `expires_at` 일치
  - [ ] `subscription_history` 테이블: action=`GRANTED` 1건
- [ ] **권한 검증**:
  - [ ] ONE_MONTH 이상: 광고 비노출
  - [ ] UNLIMITED: PDF 다운로드 버튼 활성, 풀이 결과 화면에서 다운로드 동작

### S2. 결제 실패 (PG 측 거절)

- [ ] **사전 조건**: 로그인 상태, 활성 구독 없음.
- [ ] **액션**:
  - [ ] `/checkout` → THREE_DAY 결제창 진입
  - [ ] PortOne 테스트 카드 중 "한도 초과" 또는 "잔액 부족" 시나리오 카드를 사용해 결제 시도
  - [ ] (대안) 백엔드 `verify` 단계에서 PortOne API 가 status=FAILED 응답하도록 PG 콘솔에서 강제
- [ ] **기대**:
  - [ ] 토스트: `"결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."` — error 톤
  - [ ] `/checkout` 화면 잔존 (리다이렉트 없음)
- [ ] **DB 검증**:
  - [ ] `payment.status=FAILED` (or `CANCELLED`), `paid_at = null`
  - [ ] `subscription` 테이블에 신규 row **생성되지 않음**
  - [ ] `subscription_history` 변동 **없음**
- [ ] **권한 검증**: 회원의 plan/권한 변동 없음 (광고 노출 유지, PDF 비활성).

### S3. 결제 중 사용자 이탈

- [ ] **사전 조건**: 로그인 상태, 활성 구독 없음.
- [ ] **액션**:
  - [ ] `/checkout` → THREE_DAY 결제창 진입
  - [ ] 결제창 X 버튼으로 닫기 (또는 ESC, 모달 바깥 클릭)
- [ ] **기대**:
  - [ ] 토스트: `"결제를 취소하셨습니다."` — info 톤
  - [ ] `/checkout` 화면 잔존
- [ ] **DB 검증**:
  - [ ] `payment.status=PENDING` 잔존 (자동 정리 스케줄러 없음 — 운영자 수동 또는 별 phase)
  - [ ] `subscription` 변동 없음
- [ ] **권한 검증**: 변동 없음.
- [ ] **반복 동작**: 같은 회원이 다시 `/checkout` → 결제 시도 시 새 `payment` row 가 생성되고 정상 진행.

### S4. 중복 콜백 (verify 재호출, idempotency)

- [ ] **사전 조건**: S1 (결제 성공) 직후 같은 브라우저 탭.
- [ ] **액션**:
  - [ ] DevTools Network 패널에서 직전 `POST /api/payment/verify` 요청을 우클릭 → "Replay request" (또는 `curl` 로 같은 paymentId 재요청)
  - [ ] 동일 paymentId 로 2~3회 추가 호출
- [ ] **기대**:
  - [ ] 모든 응답 200 OK, body 동일 (`paymentId`, `amount`, `plan`, `expiresAt` 일치)
  - [ ] PortOne 측에 추가 호출 발생 없음 (idempotent 단락)
- [ ] **DB 검증**:
  - [ ] `payment` row: `status=PAID` 유지, 변경 시각 변동 없음
  - [ ] `subscription` row 1개 유지 (UNIQUE 제약 `idx_subscription_payment_id` 동작)
  - [ ] `subscription_history` action=GRANTED 1건만 (중복 기록 없음)

### S5. RTDN 웹훅 중복 수신 (Play Billing)

- [ ] **사전 조건**: Play Billing 으로 결제된 활성 구독 (S8 의 Play Billing 흐름 참고). `purchase_token` 기록 확인.
- [ ] **액션**:
  - [ ] 같은 RTDN envelope (notificationType=2 REFUND, 같은 purchaseToken) 를 `POST /api/webhook/play-billing/rtdn` 로 두 번 전송
  - [ ] 예시 (OIDC 검증 활성 시 Authorization 헤더 필요):
    ```bash
    curl -X POST 'https://api.sqldpass.com/api/webhook/play-billing/rtdn?token=<MASKED_RTDN_SECRET>' \
      -H 'Content-Type: application/json' \
      -d '{"message":{"data":"<base64-payload>"}}'
    ```
- [ ] **기대**:
  - [ ] 두 호출 모두 200 OK
  - [ ] PortOne 호출 없음 (Play Billing 토큰만 처리)
- [ ] **DB 검증**:
  - [ ] `payment.status=CANCELLED` (1회만 변경)
  - [ ] `subscription.expires_at` 이 환불 시각으로 업데이트 (1회만)
  - [ ] `subscription_history` action=`REFUNDED` 1건만 (중복 기록 없음)
- [ ] **로그 검증**: 두 번째 호출에서 idempotent 단락 로그 (e.g. "already refunded").

### S6. 결제 금액 변조 시도 (Tamper guard)

- [ ] **사전 조건**: 로그인 상태, 활성 구독 없음.
- [ ] **액션**:
  - [ ] `/checkout` 진입 후 DevTools 의 Network conditioning 또는 브레이크포인트로 `POST /api/payment/prepare` 응답 가로채기
  - [ ] 응답의 `amount` 를 `9900 → 100` 으로 수정해 PortOne SDK 에 전달
  - [ ] 결제 진행 → `verify` 호출
- [ ] **기대**:
  - [ ] 백엔드 `verify` 가 PortOne 결제 금액(100)과 `payment.amount`(9,900) 불일치를 감지해 `PAYMENT_AMOUNT_MISMATCH` 또는 동급 에러로 차단
  - [ ] 프론트 토스트: `"결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."`
- [ ] **DB 검증**:
  - [ ] `payment.status=FAILED` (verify 실패 기록)
  - [ ] `subscription` row 생성되지 않음
- [ ] **참고**: prepare 단계에서 `payment.amount` 가 properties 정가/prorate 로 캡쳐되므로 client 가 보낸 amount 는 무시됨 (회귀 방지 단위 테스트 step 2).

### S7. 환불 처리 (어드민)

- [ ] **사전 조건**: PortOne 결제 1건 (`payment.status=PAID`, 활성 subscription 존재). 어드민 JWT 보유.
- [ ] **액션**:
  ```bash
  curl -X POST 'https://api.sqldpass.com/api/admin/payments/<paymentId>/refund' \
    -H 'Authorization: Bearer <ADMIN_JWT>' \
    -H 'Content-Type: application/json' \
    -d '{"reason":"고객 요청"}'
  ```
- [ ] **기대 응답**: `{"paymentId":"...", "status":"refunded"}`
- [ ] **PG 검증**: PortOne 콘솔에서 해당 결제 건이 "취소됨" 상태로 표시.
- [ ] **DB 검증**:
  - [ ] `payment.status=CANCELLED`
  - [ ] `subscription.expires_at <= now()` (즉시 만료 처리)
  - [ ] `subscription_history` action=`REFUNDED` 1건, `actor_admin_id` = 호출한 어드민 id
- [ ] **권한 검증**:
  - [ ] 환불된 회원으로 다시 로그인 → `/api/payment/subscription` 응답 `active=false`
  - [ ] 광고 다시 노출, PDF 비활성

### S8. 결제 후 프리미엄 권한 즉시 부여

- [ ] **사전 조건**: S1 (UNLIMITED 결제 성공) 직후.
- [ ] **액션**:
  - [ ] 리다이렉트된 `/mock-exams` 에서 회차 선택 → 풀이 → 결과 화면 진입
  - [ ] 결과 화면에서 "PDF 다운로드" 버튼 확인 및 클릭
  - [ ] 다른 페이지(랜딩, 블로그 등) 이동해 광고 영역 확인
- [ ] **기대**:
  - [ ] PDF 다운로드: 새 탭/다운로드 창에서 PDF 정상 수신 (Playwright 렌더링 결과)
  - [ ] 광고: 모든 페이지에서 비노출 (AdSense `<ins>` 영역 제거 또는 hidden)
- [ ] **DB / API 검증**:
  - [ ] `GET /api/payment/subscription` → `removesAds=true`, `allowsPdf=true`
- [ ] **회귀 포인트**: 캐시/세션 갱신 없이 즉시 반영되어야 함. 새로고침 강제 X.

### S9. 결제 후 권한 미부여 복구 (Reissue)

- [ ] **사전 조건**: PAID 상태의 `payment` 가 있는데 `subscription` row 가 없거나 expired 인 비정상 상태. 재현 방법: DB 에서 수동 `DELETE FROM subscription WHERE payment_id=<id>;` (개발/스테이징 DB 에서만).
- [ ] **액션**:
  ```bash
  curl -X POST 'https://api.sqldpass.com/api/admin/payments/<paymentId>/reissue-subscription' \
    -H 'Authorization: Bearer <ADMIN_JWT>' \
    -H 'Content-Type: application/json' \
    -d '{"reason":"권한 부여 누락 복구"}'
  ```
- [ ] **기대 응답**: `{"paymentId":"...", "issued":true, "expiresAt":"..."}` (UNLIMITED 는 `expiresAt=null`)
- [ ] **DB 검증**:
  - [ ] `subscription` row 1개 복구, `payment_id` 일치
  - [ ] `subscription_history` action=`GRANTED`, `actor_admin_id` = 호출한 어드민 id
- [ ] **권한 검증**: 회원 측 `/api/payment/subscription` 즉시 `active=true`.
- [ ] **이중 호출 방지**: 같은 paymentId 로 다시 reissue 호출 → 이미 활성 구독 존재 시 409/idempotent 응답 (중복 row 없음).

---

## 3. 환경별 매트릭스

| 환경 | URL | PortOne | Play Billing | 화이트리스트 | 비고 |
|------|-----|---------|--------------|--------------|------|
| local | `http://localhost:3000` | 테스트 store/channel | 미사용 | 화이트리스트 ON 권장 | 백엔드 :8080, MySQL Docker |
| staging | `https://stg.sqldpass.com` | 테스트 모드 | 미사용 (앱 미배포) | `PAYMENT_REVIEWER_NICKNAMES` 설정됨 | Vercel 호스팅, 정식 회원 가능하나 실 카드 X |
| production | `https://www.sqldpass.com` | 운영 PG (실 결제) | 운영 SKU (실 결제) | 정식 출시 시 빈 리스트 | OCI nginx 단일 진입점 |

회귀 우선순위:
- S1 ~ S6: local 또는 staging 에서 매 릴리스 전.
- S7 ~ S9: staging 에서 격주 1회 + 운영 신규 어드민 권한 변경 시.
- Play Billing 시나리오(아래 섹션 4): 앱 빌드 변경 시 또는 SKU 변경 시.

---

## 4. Play Billing 별도 시나리오 (Android 앱)

웹과 분리해서 검증한다. 앱은 `mobile/` Capacitor 7 외부 URL 모드 — 웹뷰가 `https://www.sqldpass.com` 을 띄움.

### P1. 앱 빌드 + 내부 테스트 트랙 배포

- [ ] `mobile/` 에서 `npx cap sync android` → Android Studio 에서 release AAB 빌드
- [ ] Play Console > 테스트 > 내부 테스트 > 새 출시 만들기 → AAB 업로드
- [ ] 라이선스 테스트 계정으로 설치

### P2. Play Billing 결제 성공

- [ ] 앱에서 Google 로그인 (네이티브 ID 토큰 흐름 — `/api/auth/login/google/idtoken`)
- [ ] 화이트리스트 통과 후 `/checkout` 진입
- [ ] 플랜 선택 → `Capacitor.Plugins.Billing.purchase(sku)` 트리거 → Play Billing 결제 시트
- [ ] 테스트 카드로 결제 완료
- [ ] 백엔드 검증: `POST /api/payment/play-billing/verify` 호출 → 200, `payment.status=PAID`, `payment.purchase_token` 저장
- [ ] subscription row 1개 생성 + `provider=PLAY_BILLING`

### P3. RTDN 환불 동기화

- [ ] Play Console > 주문 관리 > 위 결제건 환불 처리
- [ ] 1~5분 내 RTDN 웹훅 수신 (`POST /api/webhook/play-billing/rtdn`, OIDC 토큰 검증)
- [ ] 백엔드 로그: `notificationType=2 REFUND` 처리, subscription `expires_at <= now()`, history REFUNDED
- [ ] 앱 재진입 시 `/api/payment/subscription` → `active=false`

### P4. 토큰 도용 시도 (보안)

- [ ] 다른 회원 계정으로 로그인 후 같은 `purchase_token` 으로 verify 호출 (Postman 등)
- [ ] 기대: 백엔드 응답 `"다른 회원의 결제 토큰입니다."` — 토큰 재사용 차단 (`payment.member_id` 불일치 감지).

---

## 5. 알려진 한계 (테스트 범위 외)

| 항목 | 현재 상태 | 비고 |
|------|----------|------|
| 가상계좌 (`VIRTUAL_ACCOUNT_ISSUED`) | 즉시 PAID 처리 안 함, payment 는 PENDING 잔존 | 입금 콜백 처리 정책 별 phase 필요 |
| Play Billing 부분 환불 | 미지원 | 전체 환불만 동기화 |
| 결제 도중 이탈 PENDING 자동 정리 | 스케줄러 없음 | 운영자 수동 또는 별 phase TODO |
| 정기 결제 (구독 자동 갱신) | 미지원 | THREE_DAY/ONE_MONTH 만료 시 사용자가 다시 결제 |
| 영수증 메일 발송 | 미지원 | PortOne 콘솔에서 수동 다운로드 |
| 환불 사유 코드화 | 자유 텍스트 (`reason` 필드) | 향후 enum 화 검토 |
| iOS 결제 | 앱 미존재 | Android 우선, iOS 별 phase |
| FCM 푸시 (결제 완료 알림) | 미구현 | v1.1+ 보류, 현재는 in-app 토스트만 |

---

## 부록: 자주 사용하는 검증 쿼리

```sql
-- 최근 결제 10건
SELECT id, member_id, plan, amount, status, paid_at, created_at
FROM payment
ORDER BY created_at DESC
LIMIT 10;

-- 특정 회원의 활성 구독
SELECT s.id, s.plan, s.expires_at, s.payment_id, s.provider
FROM subscription s
WHERE s.member_id = ?
  AND (s.expires_at IS NULL OR s.expires_at > NOW())
ORDER BY s.created_at DESC;

-- subscription history (감사용)
SELECT id, member_id, payment_id, action, actor_admin_id, reason, created_at
FROM subscription_history
WHERE payment_id = ?
ORDER BY created_at;

-- 미정리 PENDING (S3 이탈 잔존)
SELECT id, member_id, plan, amount, created_at
FROM payment
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL 1 HOUR;
```

---

## 문서 운영 규칙

- 새 시나리오 추가 시 위 9개 형식(사전 조건 / 액션 / 기대 / DB 검증 / 권한 검증) 유지.
- 토스트 문구는 `frontend/src/app/checkout/CheckoutClient.tsx` 와 `frontend/src/lib/payment.ts` 의 실제 메시지와 동기화.
- 엔드포인트/스키마 변경 시 본 문서의 curl 예시도 같이 업데이트.
- 실 PortOne store-id, secret, RTDN 토큰은 절대 본 문서에 적지 않는다 — 항상 `<MASKED>` 처리.
