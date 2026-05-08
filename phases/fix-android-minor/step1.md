# Step 1 — 루트 AGENTS.md에 mobile/ 디렉터리 반영

## 배경

Harness Review에서 발견: 루트 `AGENTS.md`는 여전히 "two active apps: frontend/, backend/"로만 명시되어 있고, `a366259` commit으로 추가된 `mobile/`(Capacitor 7) 디렉터리가 미반영. `docs/ARCHITECTURE.md`·`docs/ADR.md`·`docs/PRD.md`는 mobile/을 인정하는 형태로 갱신됐지만 가이드 진입점인 루트 AGENTS.md만 누락.

`scripts/execute.py:_load_guardrails()`가 루트 `AGENTS.md`를 매 step prompt에 주입하므로, 이 파일이 mobile/을 인지하지 못하면 후속 작업이 mobile/을 "낯선 디렉터리"로 다룰 위험.

## 작업 디렉터리

저장소 루트 (`AGENTS.md` 한 파일만 변경).

## 변경 대상

- `AGENTS.md`

## 변경 내용

`## Scope` 섹션의 두 앱 목록에 mobile 한 줄 추가:

```diff
 - `frontend/`: Next.js 16, React 19, TypeScript, Tailwind CSS 4
 - `backend/`: Spring Boot 4, Java 21, Gradle
+- `mobile/`: Capacitor 7 (Android 앱, `https://www.sqldpass.com` 외부 URL 모드)
```

`## Working Directories` 섹션에도 mobile 항목 한 줄 추가:

```diff
 - Run frontend commands from `frontend/`.
 - Run backend commands from `backend/`.
+- Run mobile commands from `mobile/`.
```

`## Ownership And Boundaries` 섹션은 frontend/backend 두 앱 기준 그대로 두되, "mobile/은 frontend/를 외부 URL 모드로 점프하므로 자체 화면 코드를 두지 않는다"는 한 줄을 추가해 ADR-007과 정합.

## Acceptance Criteria

1. `AGENTS.md` Scope·Working Directories에 `mobile/` 항목 추가됨.
2. Ownership And Boundaries에 "mobile/은 자체 화면 코드를 두지 않는다" 라인 1개 추가됨.
3. 본문 톤·문체는 기존과 동일 (영어 위주, 한국어 conventional commits 메모는 그대로 유지).

## 금지 사항

- mobile/AGENTS.md를 새로 만들지 마라. 이유: 외부 URL 모드라 mobile/은 화면 코드를 두지 않고 Capacitor 설정·빌드 산출물만 가짐. 별도 가이드 파일이 필요할 만큼 규칙이 없음.
- ARCHITECTURE.md/ADR.md/PRD.md를 같이 수정하지 마라. 이유: 이미 갱신된 상태이고 이 step의 범위는 루트 AGENTS.md 한 파일.

## 검증

```powershell
# 텍스트 변경만이라 자동 테스트 없음. 수동 확인:
Select-String -Path AGENTS.md -Pattern 'mobile/'
```

세 줄(Scope, Working Directories, Ownership And Boundaries 각 1줄)이 포함되어야 함.

## Status 규칙

- 성공: `phases/fix-android-minor/index.json`의 step 1 status를 `completed`로, summary에 "루트 AGENTS.md에 mobile/ 추가 (Scope·Working Directories·Ownership 3개소)" 한 줄 기록.
- 실패: 3회 시도 후에도 불일치 시 `error` + `error_message` 기록.
