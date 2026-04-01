# 테스트 가이드

JUnit 5, AssertJ, Mockito, Spring Boot Test를 사용한다.
기본 원칙은 **레이어 책임에 맞는 범위만 검증**이다.

---

## 1. 공통 원칙

- 테스트 프레임워크: JUnit 5
- 검증: AssertJ 우선 사용
- 예외 검증: `assertThatThrownBy` 우선 사용
- 테스트 이름: `@DisplayName`으로 의도를 드러낸다
- 기능 변경 시 관련 테스트를 함께 수정하거나 추가한다
- 구현 디테일보다 계약과 규칙을 검증한다
- 중복되는 테스트 설정은 공통 베이스 클래스, fixture, helper로 추출한다

## 2. 테스트 계층

| 계층 | 방식 | 스프링 컨텍스트 |
|------|------|----------------|
| domain | 순수 단위 테스트 | X |
| service | `@SpringBootTest(webEnvironment = NONE)` + H2 | O |
| controller | `@WebMvcTest` + `@MockBean` | 부분 |
| persistence | 테스트 안 함 (JPA/Spring Data 신뢰) | - |

## 3. 도메인 테스트 규칙

- 스프링 없이 객체를 직접 생성해서 검증
- 정상 생성, 상태 전이, 도메인 검증 실패를 테스트
- 외부 의존성, DB, 웹 계층을 끌어오지 않는다

## 4. 서비스 테스트 규칙

- `@SpringBootTest(webEnvironment = NONE)` + H2 DB 사용
- 공통 베이스가 필요하면 추상 테스트 베이스 클래스 사용
- 트랜잭션 경계, 소유권 검증(memberId), 조회 조건을 우선 검증
- 테스트 데이터 생성은 fixture 우선 사용
- 컨트롤러 책임(HTTP status, JSON shape)은 검증하지 않는다

## 5. 컨트롤러 테스트 규칙

- `@WebMvcTest(대상Controller.class)` + MockMvc
- 서비스는 `@MockBean`으로 대체
- 검증 범위:
  - 요청 바인딩, Bean Validation
  - 응답 status/body/header
  - 인증 주체 전달 (`@AuthMember` 등)
  - 예외가 `GlobalExceptionHandler`를 통해 올바른 응답으로 매핑되는지
- 서비스 내부 비즈니스 로직이나 DB 동작은 검증하지 않는다
- JWT 필터/실제 인증 체인까지 검증해야 할 때만 `@SpringBootTest` + MockMvc

### 컨트롤러 테스트 작성 기준

- 성공 케이스 1개 이상
- 잘못된 입력에 대한 400 검증
- 인증 필요 API면 401/403 검증
- 서비스 예외가 예상 코드로 변환되는지 검증
- 응답 JSON 필드명과 타입 계약 검증

## 6. 테스트 데이터 규칙

- fixture를 우선 사용한다
- fixture는 도메인 의미가 드러나는 기본값을 제공해야 한다
- 테스트 본문에서는 검증에 필요한 값만 덮어쓴다
- 테스트 간 데이터 공유를 전제로 작성하지 않는다

## 7. 환경 규칙

- `src/test/resources/application.yaml` 기준 설정 사용
- 테스트 DB: H2 인메모리 (`MODE=MySQL`)
- Flyway 비활성화, `ddl-auto: create-drop`
- JWT/OAuth 설정은 테스트용 더미 값 사용
- 외부 네트워크 호출은 금지, mock/stub으로 대체

## 8. assertion 규칙

- 여러 필드를 함께 검증할 때는 `assertAll` 사용 가능
- 컬렉션은 크기뿐 아니라 핵심 속성까지 함께 검증
- 예외 검증 시 예외 타입 + 에러 코드까지 검증

## 9. 금지 사항

- 테스트 삭제나 비활성화로 문제를 숨기지 않는다
- 불필요한 mock/stub을 과하게 넣지 않는다
- 하나의 테스트에서 여러 책임을 동시에 검증하지 않는다
- 컨트롤러 테스트에서 서비스 내부 구현까지 검증하지 않는다
- 도메인 테스트에서 스프링 컨텍스트를 띄우지 않는다
