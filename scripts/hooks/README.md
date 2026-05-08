# Shared Hooks

이 디렉터리는 Claude Code와 Codex 양쪽이 공유하는 Harness lifecycle hook 스크립트를 담는다.

- Codex: `.codex/hooks.json`이 이 스크립트들을 등록한다 (`.codex/config.toml`로 `codex_hooks` 활성화 필요).
- Claude Code: `.claude/settings.json`의 `hooks` 블록이 이 스크립트들을 등록한다.

스크립트의 출력 JSON은 두 런타임이 모두 이해하는 `hookSpecificOutput` 포맷을 사용한다.

## Hooks

- `pre-tool-use-guard.ps1`: 위험 명령 패턴(`rm -rf`, `git push --force`, `git reset --hard`, `DROP TABLE`, `Remove-Item ... -Recurse -Force`)을 차단한다.
- `tdd-guard.ps1`: `apply_patch`/`Edit`/`Write`로 구현 파일을 수정할 때, 같은 변경에서 테스트가 함께 변경되거나 기존 매칭 테스트가 있는지 확인한다.
- `lint-build-test.ps1`: 변경된 파일을 보고 필요한 경우에만 프론트엔드 lint/build 또는 백엔드 test를 실행한다.
- `stop-validation.ps1`: 프론트엔드 `npm run lint`, `npm run build`와 백엔드 `.\gradlew.bat test`를 강제 실행한다.
- `tdd-guard.sh`: 같은 정책의 shell 버전이다 (Windows에서는 PowerShell 버전을 사용한다).

## Usage

전체 검증:

```powershell
.\scripts\hooks\stop-validation.ps1
```

필요한 검증만 자동 선택:

```powershell
.\scripts\hooks\lint-build-test.ps1
```

전체 lint/build/test 강제:

```powershell
.\scripts\hooks\lint-build-test.ps1 -All
```

프론트엔드만 / 백엔드만:

```powershell
.\scripts\hooks\stop-validation.ps1 -FrontendOnly
.\scripts\hooks\stop-validation.ps1 -BackendOnly
```

명령 guard 동작 확인:

```powershell
'{"hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"git status"}}' | .\scripts\hooks\pre-tool-use-guard.ps1
```

Codex는 프로젝트 `.codex/` 레이어를 trust해야 hooks가 로드된다. Claude Code는 `.claude/settings.json`이 자동 적용된다.
