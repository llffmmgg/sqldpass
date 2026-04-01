# Backend — CLAUDE.md

## Tech Stack

- **Java 21**, **Spring Boot 4.0.5**, **Gradle 9.4.1** (wrapper)
- Spring Web MVC, Spring Data JPA, Bean Validation
- MySQL (runtime dependency)
- JUnit 5 (JUnit Platform)

## Build & Run Commands

모든 Gradle 명령은 `backend/` 디렉토리에서 실행.

```bash
# 빌드
cd backend && ./gradlew build

# 테스트 전체 실행
cd backend && ./gradlew test

# 단일 테스트 실행
cd backend && ./gradlew test --tests "com.sqldpass.SomeTestClass.methodName"

# 애플리케이션 실행
cd backend && ./gradlew bootRun
```

Windows CMD/PowerShell에서는 `gradlew.bat` 사용.

## Architecture

- 패키지 루트: `com.sqldpass`
- 엔트리포인트: `SqldpassApplication.java` (`@SpringBootApplication`)
- 설정 파일: `src/main/resources/application.yaml`
- MySQL 데이터소스 설정이 application.yaml에 아직 없으므로, DB 연결 시 설정 추가 필요

## Coding Style

- Java 표준 스타일, 들여쓰기 공백 4칸
- 클래스 `PascalCase`, 메서드/필드 `camelCase`, 상수 `UPPER_SNAKE_CASE`
- Spring 컴포넌트는 생성자 주입 우선
- Lombok 사용 금지. getter/setter/생성자 등 직접 작성
- 객체 생성은 무조건 생성자 사용 (Builder 패턴 금지)
