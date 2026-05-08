---
name: harness
description: Harness 프레임워크 워크플로우로 sqldpass 모노레포 작업을 설계, 분할, 실행한다. 사용자가 "harness", "프레임워크", "phase", "step", "실행 계획", "작업 분할"을 요청할 때 사용한다.
---

# Harness Workflow

이 프로젝트는 Harness 프레임워크를 사용하는 프론트엔드/백엔드 모노레포다.

## 읽어야 할 파일

- `/AGENTS.md`
- `/CLAUDE.md`
- `/frontend/AGENTS.md` 또는 `/backend/AGENTS.md` 중 작업 대상에 해당하는 문서
- `/docs/PRD.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/docs/UI_GUIDE.md`

## 워크플로우

1. 탐색: 문서를 읽고 기획, 아키텍처, 설계 의도를 파악한다.
2. 논의: API 계약, 데이터 모델, 인증/인가, 배포 영향처럼 결정이 필요한 사항을 사용자에게 제시한다.
3. Step 설계: 작업을 작고 자기완결적인 step으로 나눈다.
4. 파일 생성: 승인 후 `phases/index.json`, `phases/{task-name}/index.json`, `phases/{task-name}/step{N}.md`를 생성한다.
5. 실행: Claude는 `python scripts/execute.py {task-name} --agent claude`, Codex는 `python scripts/execute.py {task-name} --agent codex`를 사용한다. 필요하면 `--push`를 추가한다.

## Step Rules

- 하나의 step은 하나의 레이어 또는 모듈만 다룬다.
- 프론트엔드와 백엔드를 동시에 수정해야 하면 API 계약 step과 앱별 구현 step으로 분리한다.
- 관련 문서, 이전 step 산출물 경로, 작업 디렉터리, 검증 명령을 명시한다.
- 프론트엔드 작업은 `frontend/`에서 `npm run lint`, `npm run build`를 검증 기준으로 둔다.
- 백엔드 작업은 `backend/`에서 `.\\gradlew.bat test`, `.\\gradlew.bat compileJava`를 검증 기준으로 둔다.
- API 계약 변경은 프론트엔드와 백엔드 검증을 모두 포함한다.
- 금지사항은 "X를 하지 마라. 이유: Y" 형식으로 구체화한다.

## 상태 규칙

- 성공: 해당 step을 `completed`로 변경하고 `summary`를 기록한다.
- 실패: 수정 3회 후에도 실패하면 `error`와 `error_message`를 기록한다.
- 사용자 개입 필요: `blocked`와 `blocked_reason`을 기록하고 중단한다.
