# Step 7 — 결제 수동 회귀 체크리스트 문서

## 배경

frontend 결제 이탈, PortOne 테스트 모드 특수 사항(KB국민카드 테스트 불가, 임시 카카오페이 머니), QA 수동 회귀 시나리오는 단위 테스트로 못 잡는다. `docs/payment-manual-test-checklist.md` 문서로 정리해 운영 QA 가 정기적으로 돌릴 수 있게 한다.

사용자 결정: frontend vitest 인프라 도입은 별 phase. 본 step 은 마크다운 문서만.

## 작업 디렉터리

```
/ (저장소 루트)
```

## 변경 대상

신규 1개:

| 파일 | 역할 |
|------|------|
| `docs/payment-manual-test-checklist.md` | 9 시나리오 수동 회귀 체크리스트 |

## 문서 구성 (필수 섹션)

### 1. PortOne 테스트 모드 제약 (BACKGROUND)
- 카카오페이 테스트 모드는 실제 계정이 아니라 임시 카카오페이 머니/카드 사용
- KB국민카드: 테스트 모드 결제 불가, 실 운영에서만
- 테스트 환경 설정: `NEXT_PUBLIC_PORTONE_STORE_ID` / `NEXT_PUBLIC_PORTONE_CHANNEL_KEY` 테스트 값
- 백엔드 `PAYMENT_REVIEWER_NICKNAMES` env 설정 시 화이트리스트 닉네임만 결제 가능

### 2. 9 시나리오 체크리스트 (체크박스 형식)

각 시나리오는 다음 형식:
```markdown
### S1. 결제 성공
- [ ] 사전 조건: 로그인, 활성 구독 없음
- [ ] 액션: /checkout → THREE_DAY/ONE_MONTH/UNLIMITED 각 plan 결제 카드 클릭 → PortOne 결제창 → KAKAOPAY 선택 → 결제 완료
- [ ] 기대: toast "결제 완료", /mock-exams 리다이렉트, /api/payment/subscription active=true
- [ ] DB: payment.status=PAID, subscription row 1개, history GRANTED
- [ ] 권한 검증: 광고 비노출(ONE_MONTH 이상), PDF 다운로드 활성(UNLIMITED)
```

9 시나리오 모두 위 형식으로:

1. **S1 결제 성공** — 정상 흐름, 권한 부여 확인
2. **S2 결제 실패** — 카드 한도 초과/잔액 부족 시뮬레이션 → toast 에러, payment.status=FAILED
3. **S3 결제 중 사용자 이탈** — 결제창에서 X 닫기 → toast "결제를 취소하셨습니다." (info 톤), payment.status=PENDING 잔존(별도 정리 정책)
4. **S4 중복 콜백** — 같은 결제 직후 verify 다시 호출 (DevTools 에서 POST 재시도) → 동일 응답, subscription row 1개 유지
5. **S5 웹훅 중복 수신** — Pub/Sub 테스트 메시지를 RTDN endpoint 에 두 번 POST → 둘 다 200, history REFUNDED 1개
6. **S6 결제 금액 변조** — DevTools 에서 `/api/payment/prepare` 응답의 amount 를 가로채 수정 후 PortOne SDK 호출 → 백엔드 verify 가 PAYMENT_AMOUNT_MISMATCH 로 차단
7. **S7 환불 처리** — 어드민 `POST /api/admin/payments/{id}/refund` 호출 → PortOne 측 취소 + subscription expiresAt=now + history REFUNDED
8. **S8 프리미엄 권한 부여** — S1 직후 즉시 mock-exam PDF 다운로드, 광고 미노출 확인
9. **S9 결제 후 권한 미부여 복구** — DB 에서 subscription row 만 수동 삭제 → 어드민 `POST /api/admin/payments/{id}/reissue-subscription` → row 복구, history GRANTED

### 3. 환경별 매트릭스

| 환경 | PortOne | Play Billing | 비고 |
|------|---------|--------------|------|
| local | 테스트 store/channel | 미사용 | 화이트리스트 회원으로만 |
| staging (stg.sqldpass.com) | 테스트 모드 | 미사용 | 정식 회원 가능, 실 카드 X |
| production | 실 PG | 실 SKU | 실 결제 |

### 4. Play Billing 별도 시나리오 (Android 앱)
- 앱 빌드 후 Play Console 내부 테스트 트랙 배포 → 테스트 계정으로 구매 → `/api/payment/play-billing/verify` 검증
- RTDN 환불 테스트: Play Console 에서 환불 → 백엔드 RTDN endpoint 가 OIDC 검증 + revoke

### 5. 알려진 한계
- 가상계좌(VIRTUAL_ACCOUNT_ISSUED) 결제는 본 백엔드에서 즉시 PAID 처리 안 함 — 별 phase 정책
- Play Billing 부분 환불 미지원 — 전체 환불만
- 결제 도중 사용자 이탈로 남은 PENDING row 자동 정리 스케줄러 없음 — 운영자 수동 또는 별 phase

## 작성 가이드

- 한국어로 작성. 마크다운 체크박스(- [ ]) 사용 — QA 가 PR 또는 Confluence 에 붙여 사용.
- 화면 캡쳐 placeholder 는 두지 않음 (산출물 노이즈). 필요시 후속 phase 에서 별도 첨부.
- 백엔드 명령 예시 (`gh api`, `curl`) 는 실 운영 가능한 형태로 작성. 실 토큰/URL 은 마스킹 처리.
- 작성 시 sqldpass 의 다른 docs (`docs/PROGRESS.md`, `docs/ARCHITECTURE.md`) 스타일 따른다.

## 검증

```powershell
cd /c/Users/admin/Desktop/sqldpass/sqldpass
# 마크다운 lint (없으면 생략)
# 또는 단순 git diff 검토
```

문서 파일이라 자동 검증은 없음. 사용자/QA 가 읽기 좋은지 본인 검토 후 commit.

## Acceptance Criteria

1. `docs/payment-manual-test-checklist.md` 가 위 5 섹션 (제약/9 시나리오/환경/Play Billing/한계) 모두 포함해 작성된다.
2. 9 시나리오 각각 체크박스 형식 + 사전 조건/액션/기대/DB 검증/권한 검증 항목 포함.
3. 문서 길이 200줄 ~ 400줄 범위 (지나치게 짧지도 길지도 않게).
4. 다른 코드/테스트 변경 0건.

## 금지 사항

- 마크다운 안에 실 PortOne 토큰/secret/store-id 를 노출하지 마라. 이유: 보안.
- frontend 테스트 인프라(vitest/playwright) 도입 코드를 본 step 에서 작성하지 마라. 이유: 사용자 결정 — 별 phase.
- 본 phase 의 이전 step (1~6) 산출물에 의존한 시나리오를 검증 못 하면 TODO 로 명시. 이유: 문서가 코드보다 앞서지 않도록.

## Status 규칙

- 성공: step 7 `completed`, summary 에 "수동 회귀 체크리스트 docs/payment-manual-test-checklist.md 작성, 9 시나리오 + Play Billing + 환경 매트릭스 포함".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: 문서 톤/포맷에 사용자 결정 필요 시 `blocked`.
