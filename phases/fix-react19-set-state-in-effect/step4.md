# Step 4 — frontend lint 0 errors 달성 + 회귀 검증

## 배경

step 1·2·3가 끝나면 lint errors 13건이 모두 사라졌어야 한다 (`set-state-in-effect` 12건 + `impure-function-during-render` 1건). 이 step은 *전체 정합성 검증*이고 변경 없이 검증 명령만 실행한다.

남은 warnings (Android 변경 책임의 4건은 `fix-android-minor` phase가 담당, 그 외 3rd-party `<img>` 권고 등)는 errors가 아니므로 본 phase에서는 손대지 않는다.

## 작업 디렉터리

```
frontend/
backend/
```

## 변경 대상

**없음 (검증만 수행)**.

## 검증 절차

### 1) Frontend lint 0 errors

```powershell
cd frontend
npm run lint
```

출력 마지막 줄:

```
✖ N problems (0 errors, M warnings)
```

`errors = 0`이어야 한다. warnings는 본 phase 범위 외이므로 0이 아니어도 OK.

### 2) Frontend build 통과

```powershell
cd frontend
npm run build
```

exit 0 + standalone 산출물 생성.

### 3) Backend 회귀 없음 (frontend 변경이 backend API 호출 시그니처를 건드리지 않았는지)

```powershell
cd backend
.\gradlew.bat test
```

backend는 frontend lint 정리와 무관하지만, 만약 step 2의 `useSubscription` 또는 OAuth 콜백 정리가 API 호출 위치/타이밍을 *우연히* 바꿨다면 통합 테스트가 잡을 수 있다.

### 4) 통합 hook (선택)

이번 세션에 정착한 lint-build-test 통합 스크립트로 한 방에:

```powershell
.\scripts\hooks\stop-validation.ps1
```

실패 시 PowerShell exit code != 0 — 어느 단계가 깨졌는지 stderr 메시지로 식별.

## Acceptance Criteria

1. `frontend/`에서 `npm run lint` → `0 errors`.
2. `frontend/`에서 `npm run build` → exit 0.
3. `backend/`에서 `.\gradlew.bat test` → BUILD SUCCESSFUL.
4. 위 step들에서 stage된 변경 외에 *추가* 코드 수정 없음 — 이 step은 검증 전용.
5. 만약 0 errors 도달 못 하면 어떤 위반이 남았는지 step 1·2·3 어디 누락이었는지 분석해 해당 step status를 `pending`으로 되돌리고 재실행 권고를 summary에 기록.

## 금지 사항

- 검증 도중 새 lint warning이 보여도 본 phase에서 고치지 마라. 이유: 본 phase 범위는 13개 errors. warning 정리는 별도 phase의 영역이거나 점진적 청소 대상.
- backend test 실패 시 backend 코드를 고치지 마라. 이유: 본 phase는 frontend lint 정리. backend 회귀 발견은 *blocker* 신호.

## 검증

위 검증 절차의 4단계가 모두 통과해야 phase가 `completed`가 된다.

## Status 규칙

- 성공: step 4 status를 `completed`, phase 전체를 `completed`로 마무리. summary에 "frontend lint 0 errors / build OK / backend test OK — 13건 모두 정리 완료" 기록.
- 실패: 검증 어느 단계든 깨지면 `error` + `error_message: "{단계명}: {error 요약}"`.
- blocked: backend test 회귀가 발견되면 `blocked` + `blocked_reason: "frontend lint 정리가 backend 호출 시그니처에 영향 — 사용자 합의 필요"`.
