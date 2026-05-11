# Step 3 — 개인정보처리방침: email 수집 항목 명시

## 배경

개인정보처리방침에 수집 항목을 명시하지 않으면 **법적 의무 위반**. 현재 `frontend/src/app/privacy/page.tsx` 는 v2026-05-07 기준 email 언급이 전혀 없다 (회원가입 정보 = "닉네임, provider ID" 만). Step 1·2 에서 email 수집이 시작되므로 약관 업데이트는 머지와 동시 또는 그 이전.

이용약관(`terms/page.tsx`) 도 결제 영수증 발송 언급은 선택사항 — 본 step 에서는 SKIP.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/app/privacy/page.tsx` | 섹션 1 (회원가입 정보, 결제 정보), 섹션 2 (이용 목적), 섹션 9 (개정 이력), 최상단 최종 개정일 |

## 변경 내용

### 최상단 (L17)
```diff
- <p className="mt-2 text-sm text-muted">최종 개정일: 2026년 5월 6일</p>
+ <p className="mt-2 text-sm text-muted">최종 개정일: 2026년 5월 11일</p>
```

### 섹션 1 — 회원가입 정보 (L22-25)
```diff
- <strong>회원가입 정보</strong> — Google OAuth 로그인을 통해 제공받는 닉네임, 프로필
- 식별자(provider ID). 비밀번호는 저장하지 않습니다.
+ <strong>회원가입 정보</strong> — Google OAuth 로그인을 통해 제공받는 닉네임, 프로필
+ 식별자(provider ID), 이메일 주소. 비밀번호는 저장하지 않습니다. 이메일은 사용자가
+ Google 동의 화면에서 권한을 허용하고 Google 측에서 인증한(email_verified) 경우에만
+ 수집·저장됩니다.
```

### 섹션 1 — 결제 정보 (L37-40)
```diff
- <strong>결제 정보</strong> — 유료 모의고사 결제 시 PortOne 으로부터 전달받은 결제
- 식별자(paymentId), 결제 상태, 결제 금액, 결제 시각, 잠금 해제 대상 회차 ID. 카드 번호·
- CVC·유효기간 등 카드 정보는 사이트가 직접 수집·저장하지 않습니다.
+ <strong>결제 정보</strong> — 유료 모의고사 결제 시 PortOne 으로부터 전달받은 결제
+ 식별자(paymentId), 결제 상태, 결제 금액, 결제 시각, 잠금 해제 대상 회차 ID. 카드 번호·
+ CVC·유효기간 등 카드 정보는 사이트가 직접 수집·저장하지 않습니다. 결제 처리 및
+ 전자영수증 발송을 위해 회원 이메일이 PortOne 및 PG사(KG이니시스 등)에 전달됩니다.
```

### 섹션 2 — 이용 목적 (L45-49)
```diff
  <ul className="list-disc pl-5">
    <li>학습 진척 추적, 오답노트, 모의고사 채점 및 통계 제공</li>
    <li>서비스 개선과 문제 품질 관리</li>
    <li>이용자 문의·피드백 응대</li>
+   <li>결제 처리, 전자영수증·결제 관련 알림 발송</li>
  </ul>
```

### 섹션 9 — 개정 이력 (L172-176)
```diff
  <ul className="list-disc pl-5">
+   <li>2026-05-11 — Google OAuth email 수집 항목 추가 (KG이니시스 신용카드 결제 customer.email 대응)</li>
    <li>2026-05-07 — 개인정보처리책임자·개인정보 처리 위탁(코리아포트원) 항목 추가</li>
    <li>2026-05-06 — 결제 정보 수집·PortOne 연동·보관 기간 조항 추가</li>
    <li>2026-04-09 — 최초 작성</li>
  </ul>
```

## 검증

마크다운/JSX 깨짐 없음 확인. lint/build 가 기존대로 통과:
```powershell
cd frontend
npm run lint
npm run build
```

## Acceptance Criteria

1. 최종 개정일이 "2026년 5월 11일" 로 갱신된다.
2. 섹션 1 회원가입 정보에 "이메일 주소" 와 "email_verified" 인 경우만 수집 명시.
3. 섹션 1 결제 정보에 PortOne/PG 로 email 전달 명시.
4. 섹션 2 이용 목적에 결제/영수증 알림 항목 추가.
5. 섹션 9 개정 이력 최상단에 2026-05-11 라인 추가.
6. `npm run lint` 0 errors, `npm run build` 성공.

## 금지 사항

- 다른 섹션(보관 기간, 위탁, 권리, 책임자) 본문을 수정하지 마라. 이유: 본 step 은 email 수집 명시만. 기존 내용 변경은 별도 PR.
- 섹션 7 (위탁) 의 PortOne 위탁 업무 텍스트를 바꾸지 마라. 이유: "결제 연동 서비스 제공" 으로 이미 포괄적. 명확화는 별도 PR에서 검토.
- `terms/page.tsx` 를 같이 수정하지 마라. 이유: 본 step 은 개인정보처리방침만. terms 보강은 선택사항 — 별도 phase.
- 보관 기간(섹션 4) 을 변경하지 마라. 이유: email 도 회원 정보로 함께 회원 탈퇴 시 파기. 별도 규정 불필요.

## Status 규칙

- 성공: step 3 `completed`, summary 에 "privacy/page.tsx 에 email 수집 항목/이용 목적/개정일 추가".
- 실패: lint/build 회귀 시 `error`.
