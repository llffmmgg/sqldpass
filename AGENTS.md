# Repository Guidelines

## Scope

This repository is a monorepo with two active apps and one native mobile app:

- `frontend/`: Next.js 16, React 19, TypeScript, Tailwind CSS 4
- `backend/`: Spring Boot 4, Java 21, Gradle
- `mobile/`: Native Android app, Kotlin, Jetpack Compose, Room, Retrofit, Play Billing

Read this file first. Then read the `AGENTS.md` inside the app you are changing for stack-specific guidance.

## Working Directories

- Run frontend commands from `frontend/`.
- Run backend commands from `backend/`.
- Run mobile Android commands from `mobile/`.
- Do not install dependencies or generate build artifacts at the repository root unless the repo is explicitly restructured to support that.

## Ownership And Boundaries

- Keep frontend changes inside `frontend/` unless the task explicitly requires a cross-cutting change.
- Keep backend changes inside `backend/` unless the task explicitly requires a cross-cutting change.
- `mobile/` is a native Android client. Keep Android UI, offline cache, Google Sign-In, and Play Billing code inside `mobile/`; do not move Android-only behavior into `frontend/`.
- If a task touches API contracts, update both sides in one pass or document the mismatch clearly.

## Harness Workflow

- Use the root harness configuration for cross-app planning, phase tracking, and shared documentation.
- Keep app-specific implementation rules in `frontend/AGENTS.md`, `backend/AGENTS.md`, and `mobile/AGENTS.md`.
- A phase can target one app only, or both apps when the feature spans an API contract.
- Step files should list the exact app directories and validation commands they require.

## Shared Conventions

- Prefer small, focused commits and PRs with one clear concern.
- 커밋 메시지는 한국어로 작성하되 접두어(feat, fix, chore 등)는 영어 유지 (예: `feat: 로그인 유효성 검사 추가`)
- Do not commit secrets, local credentials, `.env` files, or database passwords.
- Add tests or validation for behavior changes when the local project setup supports it.
- Preserve existing project structure and naming patterns before introducing new abstractions.

## Documentation Layout

- Put repo-wide rules in this file only.
- Put framework and module-specific rules in `frontend/AGENTS.md` and `backend/AGENTS.md`.
- Keep harness planning artifacts under `phases/`.
- Avoid copying the same rule into multiple files unless a local exception is needed.
