# Step 6 — 빌드 검증 + phase 마무리

## 배경

Step 1~5 누적 변경에 대해 최종 빌드 검증 + phase index.json 정리.

## 작업 디렉터리

`mobile/`

## 변경 대상

- `phases/mobile-5tab-runner/index.json` (status `completed`, completed_at)
- `phases/index.json` 의 본 phase entry (status `completed`, completed_at)

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

테스트 명령은 본 phase 범위 밖(현재 `mobile/app` 에 단위 테스트가 없음, junit dep 만 선언). 빌드 통과 = OK.

## Acceptance Criteria

1. `:app:assembleDebug` BUILD SUCCESSFUL.
2. `phases/mobile-5tab-runner/index.json` 의 모든 step status `completed`, phase status `completed`, `completed_at` 기록.
3. 루트 `phases/index.json` 의 본 phase status `completed`, `completed_at` 기록.

## 금지 사항

- 새 기능 추가 금지. 이유: 본 step 은 검증·정리 전용.

## Status 규칙

- 성공: phase 마무리.
- 실패: 빌드 실패 시 원인 분석 후 해당 step 으로 되돌아가 수정 — 본 step 은 `pending` 으로 유지.
