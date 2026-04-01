# 백엔드 구현 계획

## 현재 상태

완료된 것:
- 도메인 모델: `Member`, `Subject`, `Question`
- JPA 엔티티 + Repository
- Flyway 마이그레이션: 테이블 생성(V1), 과목 시드 데이터(V2), question 테이블 단순화(V3)
- springdoc-openapi 의존성 추가

아직 없는 것:
- Controller, Service, Mapper 전부 없음
- `solve`, `solve_answer` 테이블 없음
- 인증/인가 없음

### 테이블 구조 (V3 기준)

```
question
├── subject_id       (FK → subject)
├── content          (TEXT, 문제 본문 + 선택지 포함)
├── correct_option   (TINYINT, 정답 번호 1~4)
├── explanation      (TEXT, 해설)
├── created_at
└── updated_at
```

`question_option`, `explanation` 테이블 삭제됨. 정답률은 `solve_answer`에서 집계.

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
persistent/subject/SubjectMapper.java
service/subject/SubjectService.java
controller/subject/SubjectController.java
controller/subject/SubjectResponse.java      (record)
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
persistent/question/QuestionMapper.java
service/question/QuestionService.java
controller/question/QuestionController.java
controller/question/QuestionResponse.java        (record, 정답 없음)
controller/question/QuestionDetailResponse.java  (record, 정답 + 해설)
```

**흐름:**

```
[문제 목록 — 정답 제외]
Client → QuestionController.getQuestions(subjectId, size)
           → QuestionService.getRandomQuestions(subjectId, size)
              → QuestionRepository.findRandomBySubjectId(subjectId, size)
              → QuestionMapper.toDomain(entity)
           → QuestionResponse로 변환 (correctOption, explanation 제외)

[문제 상세 — 정답 + 해설 포함]
Client → QuestionController.getQuestion(id)
           → QuestionService.getQuestion(id)
              → QuestionRepository.findById(id)
              → QuestionMapper.toDomain(entity)
           → QuestionDetailResponse로 변환
```

---

### Phase 3: 풀이/채점 API

```
POST /api/solves              → 답안 제출 + 채점
GET  /api/solves              → 내 풀이 기록 목록
GET  /api/solves/{id}         → 풀이 상세
```

**사전 작업: DB 마이그레이션**

```
db/migration/V4__create_solve_tables.sql
```

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
controller/solve/SolveRequest.java           (record)
controller/solve/SolveAnswerRequest.java     (record)
controller/solve/SolveResponse.java          (record)
controller/solve/SolveAnswerResponse.java    (record)
controller/solve/SolveSummaryResponse.java   (record)
controller/solve/SolveDetailResponse.java    (record)
controller/solve/SolveAnswerDetailResponse.java (record)
```

**채점 흐름 (POST /api/solves):**

```
Client → SolveController.submit(SolveRequest)
           → SolveService.solve(request)
              1. request.answers에서 questionId 목록 추출
              2. QuestionRepository로 해당 문제들의 correctOption 조회
              3. 각 답안 비교 → 정답 여부 판별
              4. SolveEntity + SolveAnswerEntity 생성 → DB 저장
              5. 점수 계산 (correctCount / totalCount * 100)
           → SolveResponse 반환 (201 Created)
```

---

### Phase 4: 오답 API

```
GET /api/wrong-answers?subjectId={id}  → 오답 문제 목록
GET /api/wrong-answers/stats           → 과목별 취약 영역 통계 (정답률 포함)
```

별도 테이블 없이 `solve_answer` 테이블에서 집계.

**만들 파일:**

```
service/wronganswer/WrongAnswerService.java
controller/wronganswer/WrongAnswerController.java
controller/wronganswer/WrongAnswerResponse.java      (record)
controller/wronganswer/WrongAnswerStatsResponse.java  (record)
```

---

### Phase 5: 인증 (Auth) + 회원 (Member)

```
POST  /api/auth/login/{provider}  → OAuth 로그인
GET   /api/members/me             → 내 프로필
PATCH /api/members/me             → 프로필 수정
```

**만들 파일:**

```
# 인증
service/auth/AuthService.java
service/auth/JwtProvider.java
controller/auth/AuthController.java
controller/auth/LoginRequest.java    (record)
controller/auth/LoginResponse.java   (record)

# 회원
persistent/member/MemberMapper.java
service/member/MemberService.java
controller/member/MemberController.java
controller/member/MemberResponse.java        (record)
controller/member/MemberUpdateRequest.java   (record)

# 인증 필터
config/SecurityConfig.java
config/JwtAuthenticationFilter.java
```

**인증 적용 범위:**

```
인증 불필요: GET /api/subjects, GET /api/questions, POST /api/auth/login
인증 필요:   POST /api/solves, GET /api/solves, GET /api/wrong-answers, GET /api/members/me
```

---

### Phase 6: 공통 처리

```
controller/common/ErrorResponse.java          (record)
controller/common/GlobalExceptionHandler.java (@RestControllerAdvice)
service/common/NotFoundException.java
service/common/BadRequestException.java
service/common/UnauthorizedException.java
```

---

## Flyway 마이그레이션 계획

| 버전 | 파일 | 내용 | 상태 |
|------|------|------|------|
| V1 | `V1__create_tables.sql` | member, subject, question 등 | 완료 |
| V2 | `V2__insert_subject_seed.sql` | 과목 시드 데이터 | 완료 |
| V3 | `V3__simplify_question_table.sql` | question 단순화, question_option/explanation 삭제 | 완료 |
| V4 | `V4__create_solve_tables.sql` | solve, solve_answer 테이블 | Phase 3 |
