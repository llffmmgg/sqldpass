# Repository Guidelines

## Scope

This repository is a monorepo with two active apps:

- `frontend/`: Next.js 16, React 19, TypeScript, Tailwind CSS 4
- `backend/`: Spring Boot 4, Java 21, Gradle

Read this file first for repo-wide rules. Then read the `AGENTS.md` inside the app you are changing for stack-specific guidance.

## Working Directories

- Run frontend commands from `frontend/`.
- Run backend commands from `backend/`.
- Do not install dependencies or generate build artifacts at the repository root unless the repo is explicitly restructured to support that.

## Ownership And Boundaries

- Keep frontend changes inside `frontend/` unless the task explicitly requires a cross-cutting change.
- Keep backend changes inside `backend/` unless the task explicitly requires a cross-cutting change.
- If a task touches API contracts, update both sides in one pass or document the mismatch clearly.

## Shared Conventions

- Prefer small, focused commits and PRs with one clear concern.
- Do not commit secrets, local credentials, `.env` files, or database passwords.
- Add tests or validation for behavior changes when the local project setup supports it.
- Preserve existing project structure and naming patterns before introducing new abstractions.

## Documentation Layout

- Put repo-wide rules in this file only.
- Put framework and module-specific rules in `frontend/AGENTS.md` and `backend/AGENTS.md`.
- Avoid copying the same rule into multiple files unless a local exception is needed.
