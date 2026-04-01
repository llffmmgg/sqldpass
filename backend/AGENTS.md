# Backend Guidelines

## Stack

- Java 21
- Spring Boot 4.0.5
- Gradle wrapper
- Spring Web MVC, Spring Data JPA, Bean Validation
- Flyway for MySQL migrations

## Project Layout

- Main code: `src/main/java/com/sqldpass`
- Config: `src/main/resources/application.yaml`
- Migrations and resources: `src/main/resources/db`
- Tests: `src/test/java/com/sqldpass`

## Commands

Run commands from `backend/`.

- `.\gradlew.bat build`
- `.\gradlew.bat test`
- `.\gradlew.bat bootRun`

On Git Bash or WSL, use `./gradlew`.

## Coding Rules

- Follow standard Java formatting with 4-space indentation.
- Keep package names under `com.sqldpass`.
- Use `PascalCase` for classes, `camelCase` for methods and fields, and `UPPER_SNAKE_CASE` for constants.
- Prefer constructor injection for Spring components.
- Use Lombok only to reduce obvious boilerplate. Do not hide important behavior behind Lombok annotations.

## Data And API Changes

- When changing entities, DTOs, validation, or controllers, verify the API contract impact explicitly.
- Put schema changes in Flyway migrations instead of ad hoc manual SQL steps.
- Do not hardcode local database credentials in tracked files.

## Testing

- Add or update JUnit 5 tests for changed behavior when practical.
- Prefer targeted slice or unit tests over broad integration coverage when the database setup is incomplete.
- If tests cannot run because of local environment gaps, state that clearly in the final handoff.
