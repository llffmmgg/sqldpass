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

## 테스트

- 구현 코드와 테스트 코드 항상 같이 작성
- 완료 조건: `./gradlew test` 통과 후 보고
- 테스트 실패 시 스스로 수정, 통과까지 완료
- 상세 가이드: [`docs/TEST_GUIDE.md`](../docs/TEST_GUIDE.md) 참조

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
- Lombok 사용 허용 (@Getter, @NoArgsConstructor, @RequiredArgsConstructor, @Slf4j 등)
- 객체 생성은 무조건 생성자 사용 (Builder 패턴 금지)

## 신규 기능 추가 시 필수 체크리스트

새 엔드포인트/엔티티/도메인을 추가할 때 **반드시** 아래를 모두 점검하고,
누락된 게 있으면 사용자에게 묻지 말고 같은 작업에 포함시킨다.

### 새 JPA Entity (`@Entity`)를 추가했는가?
- [ ] `db/migration/V{다음번호}__{이름}.sql` 마이그레이션 작성 (필수)
  - 운영은 Flyway + Hibernate `ddl-auto: validate`라서 마이그레이션 없으면 부팅 실패
  - 최신 V## 확인: `ls backend/src/main/resources/db/migration | tail`
- [ ] 컬럼 추가/변경/삭제도 동일하게 새 V## 발급 (기존 파일 수정 금지)

### 새 REST 컨트롤러 또는 엔드포인트를 추가했는가?
- [ ] 인증이 필요하면 `WebMvcConfig.addInterceptors`의 `addPathPatterns`에
      해당 경로(`/api/{...}/**`)를 등록했는가? 등록 안 하면 `request.getAttribute("memberId")`가 null → 401
- [ ] admin 전용이면 `adminAuthInterceptor` 경로에, 사용자면 `memberAuthInterceptor` 경로에

### 도메인 클래스(`domain/**`)의 생성자 시그니처를 바꿨는가?
- [ ] 해당 도메인을 `new`로 호출하는 모든 곳을 grep으로 찾아 같이 수정
      (특히 테스트의 mock 데이터)
- [ ] Mapper, Response DTO도 함께 업데이트

### 새 컬럼을 nullable로 추가하면서 기존 코드가 NotNull을 가정하는 곳은 없나?
- [ ] `resolveNickname`처럼 null 분기 추가 필요 여부 확인

### 작업 마무리 전 self-check
- [ ] `./gradlew compileJava compileTestJava` 통과
- [ ] git status — untracked 파일 중 본 변경에 속하는 것 빠짐없이 stage했나?
- [ ] 마이그레이션, 인터셉터, 라우팅 등록 — 셋 다 봤나?
