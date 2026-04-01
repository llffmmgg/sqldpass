# 구현 현황

---

# 백엔드

## 기술 스택

- Java 21, Spring Boot 4.0.5, Gradle
- Spring Data JPA, Hibernate, MySQL 8.0
- Flyway (마이그레이션), springdoc-openapi (Swagger UI)
- 테스트: JUnit 5, AssertJ, H2 인메모리 DB

## API 목록

### 과목

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/subjects` | 과목 트리 조회 | X |

### 문제

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/questions?subjectId={id}&size={n}` | 과목별 랜덤 문제 (정답 미포함) | X |
| GET | `/api/questions/{id}` | 문제 상세 (정답+해설 포함) | X |

### 풀이/채점

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/solves` | 답안 제출 + 채점 | X-Member-Id |
| GET | `/api/solves` | 내 풀이 기록 목록 | X-Member-Id |
| GET | `/api/solves/{id}` | 풀이 상세 | X |

### 오답

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/wrong-answers?subjectId={id}` | 오답 문제 목록 | X-Member-Id |
| GET | `/api/wrong-answers/stats` | 과목별 취약 영역 통계 | X-Member-Id |

> 인증은 현재 `X-Member-Id` 헤더로 임시 처리. 추후 JWT로 교체 예정.

## DB 테이블

| 테이블 | 설명 | 마이그레이션 |
|--------|------|-------------|
| member | 회원 (OAuth용) | V1 |
| subject | 과목 (트리 구조) | V1 + V2(시드) |
| question | 문제 (content에 선택지 포함, correct_option, explanation) | V1 + V3(단순화) |
| solve | 풀이 회차 (member, subject, 점수) | V4 |
| solve_answer | 회차별 개별 답안 (선택/정답 번호, 정답 여부) | V4 |

## 패키지 구조

```
com.sqldpass/
├── config/              JpaConfig
├── controller/
│   ├── common/          ErrorResponse, GlobalExceptionHandler
│   ├── subject/         SubjectController, SubjectResponse
│   ├── question/        QuestionController, QuestionResponse, QuestionDetailResponse
│   ├── solve/           SolveController, SolveRequest, SolveResponse 등
│   └── wronganswer/     WrongAnswerController, WrongAnswerResponse 등
├── service/
│   ├── common/          NotFoundException
│   ├── subject/         SubjectService
│   ├── question/        QuestionService
│   ├── solve/           SolveService
│   └── wronganswer/     WrongAnswerService
├── domain/
│   ├── member/          Member
│   ├── subject/         Subject
│   ├── question/        Question
│   └── solve/           Solve, SolveAnswer
└── persistent/
    ├── common/          BaseTimeEntity
    ├── member/          MemberEntity, MemberRepository
    ├── subject/         SubjectEntity, SubjectRepository, SubjectMapper
    ├── question/        QuestionEntity, QuestionRepository, QuestionMapper
    └── solve/           SolveEntity, SolveAnswerEntity, SolveRepository,
                         SolveAnswerRepository, SolveMapper, Projection 인터페이스
```

## 레이어 규칙

```
[조회] DB → Entity → Mapper → Domain → Service → Controller → Response DTO (record)
[저장] Request DTO → Service → Entity 직접 생성 → DB
```

- Domain ↔ Entity 서로 import 금지
- Mapper는 persistent/ 패키지, Entity → Domain 단방향만
- DTO는 Java record

## 테스트 (14개)

| 테스트 | 방식 | 개수 |
|--------|------|------|
| SubjectServiceTest | @SpringBootTest + H2 | 2 |
| SubjectControllerTest | @WebMvcTest + MockitoBean | 2 |
| QuestionServiceTest | @SpringBootTest + H2 | 3 |
| QuestionControllerTest | @WebMvcTest + MockitoBean | 3 |
| SolveServiceTest | @SpringBootTest + H2 | 2 |
| SolveControllerTest | @WebMvcTest + MockitoBean | 2 |

## Swagger UI

`http://localhost:8080/swagger-ui/index.html`

---

# 프론트엔드

## 기술 스택

- Next.js 16.2.2 (App Router), React 19, TypeScript 5
- Tailwind CSS 4
- 폰트: Space Grotesk + JetBrains Mono (Google Fonts)

## 페이지

| 경로 | 파일 | 설명 |
|------|------|------|
| `/` | `app/page.tsx` | 랜딩 페이지 |
| `/solve` | `app/solve/page.tsx` | 과목 선택 → 문제 풀기 → 채점 결과 |

## 주요 파일

| 파일 | 설명 |
|------|------|
| `lib/api.ts` | API 호출 유틸 + 타입 정의 (Subject, Question, Solve 등) |
| `components/ScrollReveal.tsx` | IntersectionObserver 스크롤 애니메이션 |
| `app/globals.css` | 색상 토큰, 키프레임 애니메이션, 유틸리티 클래스 |
| `app/layout.tsx` | 폰트 설정 (Space Grotesk, JetBrains Mono) |
| `next.config.ts` | API 프록시 (rewrites) |

## API 프록시 (로컬 vs 운영)

```
프론트 코드: /api/subjects → Next.js rewrites → ${NEXT_PUBLIC_API_URL}/api/subjects
```

| 환경 | NEXT_PUBLIC_API_URL | 설정 파일 |
|------|---------------------|-----------|
| 로컬 | `http://localhost:8080` | `.env.local` |
| 운영 | 운영 백엔드 URL | 배포 환경변수 |

## 문제 풀이 흐름 (/solve)

```
1. 과목 선택    GET /api/subjects → 트리 표시 → 하위 과목 클릭
2. 문제 풀기    GET /api/questions?subjectId={id}&size=10 → 1문제씩 표시
3. 답안 제출    POST /api/solves → 채점
4. 결과 확인    점수, 문제별 정오답 표시
```

---

# 미구현

| 항목 | 설명 |
|------|------|
| 인증 (OAuth + JWT) | 현재 X-Member-Id 헤더 임시 처리 |
| 회원 API | GET /api/members/me, PATCH /api/members/me |
| 운영 Docker | Dockerfile, docker-compose.prod.yaml |
| 문제 데이터 | DB에 실제 문제 시드 데이터 없음 |
| 오답 노트 UI | 프론트 /wrong-answers 페이지 |
| 풀이 기록 UI | 프론트 /history 페이지 |
