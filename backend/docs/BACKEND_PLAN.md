# 백엔드 구현 계획

## 현재 상태

완료된 것:
- 도메인 모델: `Member`, `Subject`, `Question`, `QuestionOption`, `Explanation`
- JPA 엔티티 + Repository
- Flyway 마이그레이션: 테이블 생성(V1), 과목 시드 데이터(V2)
- springdoc-openapi 의존성 추가

아직 없는 것:
- Controller, Service, Mapper 전부 없음
- `solve`, `solve_answer` 테이블 없음
- 인증/인가 없음

---

## 구현 순서

인증 없이 먼저 핵심 기능을 구현하고, 마지막에 인증을 붙인다.

### Phase 1: 과목 API (가장 단순, 뼈대 세팅)

```
GET /api/subjects → 과목 트리 조회
```

이 API를 만들면서 Controller → Service → Mapper 레이어 구조를 확립한다.

**만들 파일:**

```
persistent/subject/SubjectMapper.java        # Entity → Domain 변환
service/subject/SubjectService.java          # 비즈니스 로직
controller/subject/SubjectController.java    # REST endpoint
controller/subject/SubjectResponse.java      # Response DTO (record)
```

**흐름:**

```
Client → SubjectController.getSubjects()
           → SubjectService.getSubjectTree()
              → SubjectRepository.findByParentIsNullOrderByDisplayOrder()
              → SubjectMapper.toDomain(entity)  (children 재귀 포함)
           → SubjectResponse로 변환하여 반환
```

---

### Phase 2: 문제 API

```
GET /api/questions?subjectId={id}&size={n}  → 랜덤 문제 목록 (정답 미포함)
GET /api/questions/{id}                      → 문제 상세 (정답 + 해설 포함)
```

**만들 파일:**

```
persistent/question/QuestionMapper.java          # Entity → Domain 변환
service/question/QuestionService.java            # 비즈니스 로직
controller/question/QuestionController.java      # REST endpoint
controller/question/QuestionResponse.java        # 목록용 (정답 없음)
controller/question/QuestionOptionResponse.java
controller/question/QuestionDetailResponse.java  # 상세용 (정답 + 해설)
controller/question/QuestionOptionDetailResponse.java
```

**흐름:**

```
[문제 목록]
Client → QuestionController.getQuestions(subjectId, size)
           → QuestionService.getRandomQuestions(subjectId, size)
              → QuestionRepository.findRandomBySubjectId(subjectId, size)
              → QuestionMapper.toDomain(entity)
           → QuestionResponse로 변환 (isCorrect 제외)

[문제 상세]
Client → QuestionController.getQuestion(id)
           → QuestionService.getQuestion(id)
              → QuestionRepository.findById(id) + fetch options, explanation
              → QuestionMapper.toDomain(entity)
           → QuestionDetailResponse로 변환 (isCorrect, explanation 포함)
```

**주의:** 문제 목록에서는 정답 정보를 절대 내려보내지 않는다. 클라이언트에서 정답 확인 불가능하게.

---

### Phase 3: 풀이/채점 API

```
POST /api/solves              → 답안 제출 + 채점
GET  /api/solves              → 내 풀이 기록 목록
GET  /api/solves/{id}         → 풀이 상세
```

**사전 작업: DB 마이그레이션**

```
db/migration/V3__create_solve_tables.sql
```

`solve`, `solve_answer` 테이블 생성.

**만들 파일:**

```
# 도메인
domain/solve/Solve.java
domain/solve/SolveAnswer.java

# 영속성
persistent/solve/SolveEntity.java
persistent/solve/SolveAnswerEntity.java
persistent/solve/SolveRepository.java
persistent/solve/SolveMapper.java

# 서비스
service/solve/SolveService.java

# 컨트롤러
controller/solve/SolveController.java
controller/solve/SolveRequest.java           # Request DTO
controller/solve/SolveAnswerRequest.java
controller/solve/SolveResponse.java          # 채점 결과
controller/solve/SolveAnswerResponse.java
controller/solve/SolveSummaryResponse.java   # 목록용
controller/solve/SolveDetailResponse.java    # 상세용
controller/solve/SolveAnswerDetailResponse.java
```

**채점 흐름 (POST /api/solves):**

```
Client → SolveController.submit(SolveRequest)
           → SolveService.solve(request)
              1. request.answers에서 questionId 목록 추출
              2. QuestionRepository로 해당 문제들의 정답(correctOption) 조회
              3. 각 답안 비교 → 정답 여부 판별
              4. SolveEntity + SolveAnswerEntity 생성 → DB 저장
              5. 점수 계산 (correctCount / totalCount * 100)
           → SolveResponse 반환 (201 Created)
```

**풀이 기록 조회 (GET /api/solves):**

```
Client → SolveController.getSolves()
           → SolveService.getMySolves(memberId)
              → SolveRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
           → SolveSummaryResponse 목록 반환
```

**풀이 상세 (GET /api/solves/{id}):**

```
Client → SolveController.getSolve(id)
           → SolveService.getSolve(id)
              → SolveRepository.findById(id) + fetch answers
              → 각 answer의 question content, explanation 함께 조회
           → SolveDetailResponse 반환
```

---

### Phase 4: 오답 API

```
GET /api/wrong-answers?subjectId={id}  → 오답 문제 목록
GET /api/wrong-answers/stats           → 과목별 취약 영역 통계
```

별도 테이블 없이 `solve_answer` 테이블에서 집계한다.

**만들 파일:**

```
service/wronganswer/WrongAnswerService.java
controller/wronganswer/WrongAnswerController.java
controller/wronganswer/WrongAnswerResponse.java
controller/wronganswer/WrongAnswerStatsResponse.java
```

**오답 목록 흐름:**

```
Client → WrongAnswerController.getWrongAnswers(subjectId)
           → WrongAnswerService.getWrongAnswers(memberId, subjectId)
              → SolveRepository 커스텀 쿼리
                (solve_answer에서 is_correct = false인 항목을 
                 question_id 기준으로 GROUP BY, COUNT, MAX(created_at))
           → WrongAnswerResponse 목록 반환
```

**취약 영역 통계 흐름:**

```
Client → WrongAnswerController.getStats()
           → WrongAnswerService.getStats(memberId)
              → SolveRepository 커스텀 쿼리
                (solve_answer를 subject별로 GROUP BY,
                 총 풀이 수 / 오답 수 / 오답률 계산)
           → WrongAnswerStatsResponse 목록 반환
```

---

### Phase 5: 인증 (Auth) + 회원 (Member)

```
POST  /api/auth/login/{provider}  → OAuth 로그인
GET   /api/members/me             → 내 프로필
PATCH /api/members/me             → 프로필 수정
```

**사전 작업:**
- JWT 라이브러리 의존성 추가 (jjwt)
- Spring Security 설정 (또는 간단한 필터 기반)

**만들 파일:**

```
# 인증
service/auth/AuthService.java
service/auth/JwtProvider.java
controller/auth/AuthController.java
controller/auth/LoginRequest.java
controller/auth/LoginResponse.java

# 회원
persistent/member/MemberMapper.java
service/member/MemberService.java
controller/member/MemberController.java
controller/member/MemberResponse.java
controller/member/MemberUpdateRequest.java

# 인증 필터
config/SecurityConfig.java       (또는 WebMvcConfig + 인터셉터)
config/JwtAuthenticationFilter.java
```

**로그인 흐름:**

```
Client → AuthController.login(provider, LoginRequest)
           → AuthService.login(provider, code)
              1. OAuth provider에 code로 access_token 요청
              2. access_token으로 사용자 정보 조회
              3. MemberRepository.findByProviderAndProviderId()로 기존 회원 확인
              4. 없으면 MemberEntity 생성 → 저장
              5. JwtProvider.createToken(memberId) → JWT 발급
           → LoginResponse(accessToken, member) 반환
```

**인증 적용 범위:**

```
인증 불필요:
  - GET /api/subjects
  - GET /api/questions
  - GET /api/questions/{id}
  - POST /api/auth/login/{provider}

인증 필요:
  - POST /api/solves
  - GET  /api/solves
  - GET  /api/solves/{id}
  - GET  /api/wrong-answers
  - GET  /api/wrong-answers/stats
  - GET  /api/members/me
  - PATCH /api/members/me
```

---

### Phase 6: 공통 처리

**에러 핸들링:**

```
controller/common/ErrorResponse.java          # record
controller/common/GlobalExceptionHandler.java # @RestControllerAdvice
```

**커스텀 예외:**

```
service/common/NotFoundException.java       # 404
service/common/BadRequestException.java     # 400
service/common/UnauthorizedException.java   # 401
```

---

## 전체 파일 트리 (완성 시)

```
com.sqldpass/
├── config/
│   ├── SecurityConfig.java
│   └── JwtAuthenticationFilter.java
├── controller/
│   ├── common/
│   │   ├── ErrorResponse.java
│   │   └── GlobalExceptionHandler.java
│   ├── auth/
│   │   ├── AuthController.java
│   │   ├── LoginRequest.java
│   │   └── LoginResponse.java
│   ├── member/
│   │   ├── MemberController.java
│   │   ├── MemberResponse.java
│   │   └── MemberUpdateRequest.java
│   ├── subject/
│   │   ├── SubjectController.java
│   │   └── SubjectResponse.java
│   ├── question/
│   │   ├── QuestionController.java
│   │   ├── QuestionResponse.java
│   │   ├── QuestionOptionResponse.java
│   │   ├── QuestionDetailResponse.java
│   │   └── QuestionOptionDetailResponse.java
│   ├── solve/
│   │   ├── SolveController.java
│   │   ├── SolveRequest.java
│   │   ├── SolveAnswerRequest.java
│   │   ├── SolveResponse.java
│   │   ├── SolveAnswerResponse.java
│   │   ├── SolveSummaryResponse.java
│   │   ├── SolveDetailResponse.java
│   │   └── SolveAnswerDetailResponse.java
│   └── wronganswer/
│       ├── WrongAnswerController.java
│       ├── WrongAnswerResponse.java
│       └── WrongAnswerStatsResponse.java
├── service/
│   ├── common/
│   │   ├── NotFoundException.java
│   │   ├── BadRequestException.java
│   │   └── UnauthorizedException.java
│   ├── auth/
│   │   ├── AuthService.java
│   │   └── JwtProvider.java
│   ├── member/
│   │   └── MemberService.java
│   ├── subject/
│   │   └── SubjectService.java
│   ├── question/
│   │   └── QuestionService.java
│   ├── solve/
│   │   └── SolveService.java
│   └── wronganswer/
│       └── WrongAnswerService.java
├── domain/
│   ├── member/Member.java
│   ├── subject/Subject.java
│   ├── question/
│   │   ├── Question.java
│   │   ├── QuestionOption.java
│   │   └── Explanation.java
│   └── solve/
│       ├── Solve.java
│       └── SolveAnswer.java
└── persistent/
    ├── common/BaseTimeEntity.java
    ├── member/
    │   ├── MemberEntity.java
    │   ├── MemberRepository.java
    │   └── MemberMapper.java
    ├── subject/
    │   ├── SubjectEntity.java
    │   ├── SubjectRepository.java
    │   └── SubjectMapper.java
    ├── question/
    │   ├── QuestionEntity.java
    │   ├── QuestionOptionEntity.java
    │   ├── ExplanationEntity.java
    │   ├── QuestionRepository.java
    │   └── QuestionMapper.java
    └── solve/
        ├── SolveEntity.java
        ├── SolveAnswerEntity.java
        ├── SolveRepository.java
        └── SolveMapper.java
```

---

## Flyway 마이그레이션 계획

| 버전 | 파일 | 내용 | Phase |
|------|------|------|-------|
| V1 | `V1__create_tables.sql` | member, subject, question 등 (완료) | - |
| V2 | `V2__insert_subject_seed.sql` | 과목 시드 데이터 (완료) | - |
| V3 | `V3__create_solve_tables.sql` | solve, solve_answer 테이블 | Phase 3 |
