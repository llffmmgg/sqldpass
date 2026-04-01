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

## Layer Rules

### 데이터 흐름

```
[조회] DB → Entity → Mapper → Domain → Service → Controller → Response DTO
[저장] Request DTO → Service → Entity 직접 생성 → DB
```

- Domain → Entity 변환은 하지 않는다. 저장 시에는 Request DTO에서 Entity를 직접 생성한다.
- Entity → Domain 변환은 `persistent/` 패키지 내 Mapper 클래스가 담당한다.

### 패키지 구조

```
com.sqldpass/
├── domain/          # 순수 도메인 모델 (POJO, JPA 의존성 없음)
├── persistent/      # JPA Entity, Repository, Mapper
├── service/         # 비즈니스 로직
└── controller/      # REST Controller, Request/Response DTO
```

### Mapper 규칙

- Mapper는 `persistent/` 패키지에 위치한다 (예: `persistent/question/QuestionMapper.java`)
- Entity → Domain 변환만 담당한다 (단방향)
- Domain 패키지는 Entity를 import하지 않는다
- Entity 패키지는 Domain을 import하지 않는다

### DTO 규칙

- DTO는 Java `record`로 작성한다
- Request/Response DTO는 `controller/` 패키지에 위치한다

## Coding Style

- Java 표준 스타일, 들여쓰기 공백 4칸
- 클래스 `PascalCase`, 메서드/필드 `camelCase`, 상수 `UPPER_SNAKE_CASE`
- Spring 컴포넌트는 생성자 주입 우선
- Lombok 사용 금지. getter/setter/생성자 등 직접 작성
- 객체 생성은 무조건 생성자 사용 (Builder 패턴 금지)
