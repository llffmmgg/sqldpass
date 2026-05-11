# Step 2 — 환불 그라운드 룰 운영 문서화

## 배경

PortOne 통합 점검(10대 항목)에서 발견된 갭 #2.

환불 처리 경로는 두 가지:
- **정상**: 어드민이 sqldpass `/api/admin/payments/{paymentId}/refund` 호출 → 백엔드가 PortOne cancel API + DB CANCELLED + 구독 회수 + history REFUNDED 를 단일 트랜잭션으로 처리.
- **사고**: 어드민이 PortOne 콘솔 / 카카오페이 어드민 / KG이니시스 어드민에서 직접 "취소" 버튼 → 돈은 환불되지만 sqldpass DB 는 PAID 그대로 → 사용자가 환불받고도 구독 계속 이용 = 회사 손해.

코드 변경 없이 운영 문서에 그라운드 룰을 명시하면 사고를 예방할 수 있다. `docs/payment-manual-test-checklist.md` 는 이미 QA/운영자가 회귀 시 참조하는 문서라 같은 곳에 두는 게 동선상 자연스럽다.

## 작업 디렉터리

```
docs/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `docs/payment-manual-test-checklist.md` | 섹션 1(사전 숙지) 안에 `1-5. 환불 그라운드 룰 (필독)` 신규 추가 |

기존 1-1 ~ 1-4 와 섹션 2 이후는 변경 없음. 1-5 를 끝에 붙이므로 번호 재매김 없음.

## 추가할 내용 (1-5 섹션 본문)

```markdown
### 1-5. 환불 그라운드 룰 (필독)

환불은 반드시 sqldpass 어드민 환불 endpoint 로만 처리한다.

- 올바른 경로: 어드민 화면의 환불 버튼 (`POST /api/admin/payments/{paymentId}/refund`)
  - 백엔드가 PortOne `cancel` API 호출 → `PaymentEntity.status = CANCELLED` → 구독 회수 → `subscription_history.action = REFUNDED` 기록을 **단일 트랜잭션**으로 수행한다.
  - 실패 시 전체 롤백 → DB 와 PG 상태가 항상 동기.

- 금지된 경로:
  - **PortOne 콘솔에서 직접 "취소" 버튼**: 결제는 환불되지만 sqldpass DB 는 PAID 그대로. 구독이 회수되지 않아 사용자가 환불 + 서비스 이용 동시 가능.
  - **카카오페이 어드민 / KG이니시스 어드민 직접 취소**: 동일한 사고. 추가로 PortOne 상태와도 불일치할 수 있다.

#### 사고 발생 시 복구 절차

PG 콘솔에서 이미 취소된 결제를 뒤늦게 발견한 경우:

1. DB 에서 결제 조회 → 현재 status 확인.
   ```sql
   SELECT id, payment_id, member_id, status, amount FROM payment WHERE payment_id = ?;
   ```
2. 어드민 환불 endpoint 재호출 시 PortOne 이 "이미 취소" 응답을 주면 백엔드 동기화가 안 될 수 있으므로, 운영자가 SQL 로 직접 보정:
   ```sql
   UPDATE payment SET status = 'CANCELLED' WHERE payment_id = ?;
   UPDATE subscription SET expires_at = NOW() WHERE payment_id = ?;
   INSERT INTO subscription_history (member_id, action, payment_id, reason, actor_admin_id, created_at)
   VALUES (?, 'REFUNDED', ?, 'PG 콘솔 직접 취소 수동 동기화', ?, NOW());
   ```
3. 사고 원인(누가/언제/왜 PG 콘솔에서 직접 취소했는지) 기록 후 재발 방지 — 본 그라운드 룰을 어드민 인원에게 재공지.
```

## 검증

마크다운 추가만, 자동 검증 없음. 다음만 확인:

- 기존 섹션 1-1 ~ 1-4 / 섹션 2 이후 내용 변경 0.
- 새 섹션의 코드블럭(```sql, ```markdown)이 정상 닫혀 있고 다음 섹션의 구분선/제목과 충돌 없음.
- 문서 전체 라인 수가 기존 298 + 약 35~40 줄 증가.

## Acceptance Criteria

1. `docs/payment-manual-test-checklist.md` 에 `### 1-5. 환불 그라운드 룰 (필독)` 섹션이 추가된다.
2. 올바른 경로 / 금지 경로 / 사고 복구 절차 세 부분 모두 포함.
3. 사고 복구 SQL 예시 포함 (payment, subscription, subscription_history 3개 테이블).
4. 기존 1-1 ~ 1-4, 섹션 2 이후 변경 없음.

## 금지 사항

- 별도 `PAYMENT_OPERATIONS.md` 파일을 새로 만들지 마라. 이유: 운영자 동선상 같은 회귀 체크리스트 안에 두는 게 자연스럽고, 문서 분산은 누락을 부른다.
- 사고 복구 SQL 에 `DELETE` 를 쓰지 마라. 이유: 결제 이력은 절대 삭제 금지. `UPDATE` 와 `INSERT` 로 보정.
- `actor_admin_id` 를 NULL 로 두지 마라. 이유: 보정 작업도 추적 가능해야 한다. 운영자가 자기 어드민 ID 를 넣게 안내.
- 코드 파일(`*.ts`, `*.java`) 을 수정하지 마라. 이유: 본 step 은 문서화 전용.

## Status 규칙

- 성공: step 2 `completed`, summary 에 "환불 그라운드 룰 1-5 섹션 추가".
- 실패: 마크다운 깨짐 등 발생 시 `error`.
