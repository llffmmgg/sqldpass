# Step 8 — 문서/주석 4티어 → 3-tier 정리

## 배경

P2-7: 코드/주석에 "4티어" 표현이 남아 있는데 `SubscriptionPlan` enum 은 3-tier (THREE_DAY/ONE_MONTH/UNLIMITED). 무료를 4번째로 친 표현이지만, "4티어 구독 결제" 문구가 새로 코드 읽는 사람에게 혼란을 준다.

수정 대상은 Java/TS 주석 4곳. **마이그레이션 SQL 의 주석은 변경 금지** — Flyway checksum 변경 시 운영 부팅이 거부된다 (`V75__create_subscription_table.sql` 첫 줄 "4티어 구독 모델" 그대로 둔다).

본 step 은 코드 동작 변경 0건, 주석만 정리.

## 작업 디렉터리

```
backend/, frontend/
```

## 변경 대상

| 파일 | 라인 | 변경 |
|------|------|------|
| `backend/src/main/java/com/sqldpass/controller/payment/PaymentController.java` | 30, 37 | "4티어 구독 결제 (PortOne)" → "구독 결제 (PortOne)" 또는 "3-tier 구독 결제 (PortOne)" |
| `backend/src/main/java/com/sqldpass/persistent/payment/PaymentEntity.java` | 54 | "구독 plan — 4티어 도입 후 prepare 시 채워짐..." → "구독 plan — prepare 시 채워짐..." |
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | 26 | "4티어 구독 결제..." → "구독 결제..." |
| `frontend/src/lib/payment.ts` | 1 | "PortOne V2 4티어 구독 결제 흐름." → "PortOne V2 구독 결제 흐름." |

문구 통일 원칙: "4티어" 자체를 제거하고 단순히 "구독 결제" 또는 "구독 plan 결제" 로 교체. THREE_DAY/ONE_MONTH/UNLIMITED 의 enum 자체가 코드에 명확하므로 추가 설명 불필요.

## 검증

```powershell
cd backend
.\gradlew.bat compileJava

cd ..\frontend
npm run lint
npm run build
```

코드 동작 변경이 없으므로 컴파일/빌드만 통과하면 된다.

## Acceptance Criteria

1. 위 4개 파일에서 "4티어" 표현이 모두 제거된다.
2. 마이그레이션 SQL 파일의 주석은 변경되지 않는다 (`V75__create_subscription_table.sql` 등 그대로).
3. `gradlew.bat compileJava` + `npm run build` 통과.
4. 코드 동작/시그니처/타입 변경 없음 (diff 가 주석/문자열 라인만).

## 금지 사항

- V## 마이그레이션 SQL 파일을 수정하지 마라. 이유: Flyway checksum 변경 시 운영 부팅 거부.
- "4티어" 외의 주석을 손대지 마라 (예: 다른 doc drift 발견하더라도 본 step 범위 외). 이유: PR diff 를 좁게 유지.
- frontend `payment.ts:195` 의 `currency: "CURRENCY_KRW"` 를 변경하지 마라. 이유: PortOne SDK 의 통화 식별자 형식 — 코드 동작 영향.
- 타이핑/렌더링/번들에 영향 가는 export/import 변경 금지. 이유: 본 step 은 주석만.

## Status 규칙

- 성공: step 8 `completed`, summary 에 "주석 4건 4티어 표현 제거, 마이그레이션 SQL 무변경, build OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: 사용자가 "4티어" 표현 유지 의사가 있을 시 `blocked`.
