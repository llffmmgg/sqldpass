# Step 3 — 빌드 검증 + 커밋 + 푸시 + phase 마무리

## 배경

Step 1·2 누적 변경을 로컬에서 검증한 뒤 commit + push, phase index 정리.

## 작업 디렉터리

저장소 루트.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

`BUILD SUCCESSFUL` 이어야 함 (GOOGLE_WEB_CLIENT_ID 비어도 통과).

## Git

```powershell
git add mobile/app/build.gradle mobile/gradle.properties mobile/AGENTS.md `
        .github/workflows/mobile-ci.yml `
        phases/index.json phases/mobile-secrets-and-ci/
git status
git commit -m "..."   # 본문은 phase 요약
git pull --rebase
git push
```

## Acceptance Criteria

1. `:app:assembleDebug` BUILD SUCCESSFUL.
2. 커밋 한 개에 본 phase 의 모든 변경 포함.
3. `git push` 성공, GitHub Actions 의 `mobile-ci` 워크플로우가 새 commit 으로 트리거되어 BUILD SUCCESSFUL.
4. `phases/mobile-secrets-and-ci/index.json` 의 모든 step `completed`, phase `completed`.
5. 루트 `phases/index.json` 의 본 phase status `completed`, `completed_at` 기록.

## 금지 사항

- 새 기능 추가 금지. 이유: 본 step 은 검증·커밋·정리 전용.
- `git push --force` 금지. 이유: 공유 main 보호.
- pre-commit hook 우회 (`--no-verify`) 금지. 이유: 사용자 룰.

## Status 규칙

- 성공: phase 마무리.
- 실패: 빌드/push 실패 시 원인 분석 → 해당 step 으로 복귀.
