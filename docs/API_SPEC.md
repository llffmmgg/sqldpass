# API 명세서

## 개요

- **Base URL:** `/api`
- **문서 자동 생성:** springdoc-openapi (Swagger UI: `/swagger-ui/index.html`)
- **인증:** OAuth 2.0 (추후 구현)
- **DTO:** Java `record` 사용

---

## 1. 과목 (Subject)

### `GET /api/subjects`

과목 목록을 트리 구조로 조회한다.

**Response `200 OK`**

```json
[
  {
    "id": 1,
    "name": "1과목: 데이터 모델링의 이해",
    "displayOrder": 1,
    "children": [
      { "id": 3, "name": "데이터 모델링의 이해", "displayOrder": 1, "children": [] },
      { "id": 4, "name": "데이터 모델과 SQL", "displayOrder": 2, "children": [] }
    ]
  },
  {
    "id": 2,
    "name": "2과목: SQL 기본 및 활용",
    "displayOrder": 2,
    "children": [
      { "id": 5, "name": "SQL 기본", "displayOrder": 1, "children": [] },
      { "id": 6, "name": "SQL 활용", "displayOrder": 2, "children": [] },
      { "id": 7, "name": "관리 구문", "displayOrder": 3, "children": [] }
    ]
  }
]
```

**DTO:**

```java
public record SubjectResponse(
    Long id,
    String name,
    int displayOrder,
    List<SubjectResponse> children
) {}
```

---

## 2. 문제 (Question)

> 선택지는 `content` 본문에 포함. 정답 번호(`correctOption`)와 해설(`explanation`)은 question 테이블 컬럼.

### `GET /api/questions?subjectId={id}&size={n}`

과목별 랜덤 문제 목록을 조회한다. 풀이 화면에서 사용.

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `subjectId` | Long | Y | 과목 ID (하위 과목) |
| `size` | int | N | 문제 수 (기본 10) |

**Response `200 OK`**

```json
[
  {
    "id": 42,
    "subjectId": 5,
    "content": "다음 SQL의 실행 결과로 올바른 것은?\n\n1. CNT: 3, HIGH_SAL: 2\n2. CNT: 4, HIGH_SAL: 2\n3. CNT: 3, HIGH_SAL: 3\n4. CNT: 4, HIGH_SAL: 3"
  }
]
```

> 정답(`correctOption`)과 해설은 포함하지 않는다. 채점 시 서버에서 판별.

**DTO:**

```java
public record QuestionResponse(
    Long id,
    Long subjectId,
    String content
) {}
```

### `GET /api/questions/{id}`

문제 상세 조회. 정답과 해설을 포함한다 (풀이 완료 후 리뷰용).

**Response `200 OK`**

```json
{
  "id": 42,
  "subjectId": 5,
  "content": "다음 SQL의 실행 결과로 올바른 것은?\n\n1. CNT: 3, HIGH_SAL: 2\n2. CNT: 4, HIGH_SAL: 2\n3. CNT: 3, HIGH_SAL: 3\n4. CNT: 4, HIGH_SAL: 3",
  "correctOption": 1,
  "explanation": "WHERE dept_id = 10 조건으로 3건이 조회되며..."
}
```

**DTO:**

```java
public record QuestionDetailResponse(
    Long id,
    Long subjectId,
    String content,
    int correctOption,
    String explanation
) {}
```

---

## 3. 풀이 (Solve)

> 신규 테이블 `solve`, `solve_answer` 필요.

### `POST /api/solves`

답안을 제출하고 채점 결과를 받는다.

**Request Body**

```json
{
  "subjectId": 5,
  "answers": [
    { "questionId": 42, "selectedOption": 1 },
    { "questionId": 43, "selectedOption": 3 },
    { "questionId": 44, "selectedOption": 2 }
  ]
}
```

**DTO:**

```java
public record SolveRequest(
    Long subjectId,
    List<SolveAnswerRequest> answers
) {}

public record SolveAnswerRequest(
    Long questionId,
    int selectedOption
) {}
```

**Response `201 Created`**

```json
{
  "id": 1,
  "subjectId": 5,
  "totalCount": 3,
  "correctCount": 2,
  "score": 66,
  "solvedAt": "2026-04-01T14:30:00",
  "answers": [
    { "questionId": 42, "selectedOption": 1, "correctOption": 1, "correct": true },
    { "questionId": 43, "selectedOption": 3, "correctOption": 2, "correct": false },
    { "questionId": 44, "selectedOption": 2, "correctOption": 2, "correct": true }
  ]
}
```

**DTO:**

```java
public record SolveResponse(
    Long id,
    Long subjectId,
    int totalCount,
    int correctCount,
    int score,
    LocalDateTime solvedAt,
    List<SolveAnswerResponse> answers
) {}

public record SolveAnswerResponse(
    Long questionId,
    int selectedOption,
    int correctOption,
    boolean correct
) {}
```

### `GET /api/solves`

내 풀이 기록 목록 (요약).

**Response `200 OK`**

```json
[
  {
    "id": 1,
    "subjectName": "SQL 기본",
    "totalCount": 10,
    "correctCount": 7,
    "score": 70,
    "solvedAt": "2026-04-01T14:30:00"
  }
]
```

**DTO:**

```java
public record SolveSummaryResponse(
    Long id,
    String subjectName,
    int totalCount,
    int correctCount,
    int score,
    LocalDateTime solvedAt
) {}
```

### `GET /api/solves/{id}`

풀이 상세 (문제별 정오답 + 해설 포함).

**Response `200 OK`**

```json
{
  "id": 1,
  "subjectName": "SQL 기본",
  "totalCount": 10,
  "correctCount": 7,
  "score": 70,
  "solvedAt": "2026-04-01T14:30:00",
  "answers": [
    {
      "questionId": 42,
      "questionContent": "다음 SQL의 실행 결과로 올바른 것은?",
      "selectedOption": 1,
      "correctOption": 1,
      "correct": true,
      "explanation": "WHERE dept_id = 10 조건으로..."
    }
  ]
}
```

**DTO:**

```java
public record SolveDetailResponse(
    Long id,
    String subjectName,
    int totalCount,
    int correctCount,
    int score,
    LocalDateTime solvedAt,
    List<SolveAnswerDetailResponse> answers
) {}

public record SolveAnswerDetailResponse(
    Long questionId,
    String questionContent,
    int selectedOption,
    int correctOption,
    boolean correct,
    String explanation
) {}
```

---

## 4. 오답 (Wrong Answer)

### `GET /api/wrong-answers?subjectId={id}`

내 오답 문제 목록. 과목별 필터링 가능.

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `subjectId` | Long | N | 과목 ID (없으면 전체) |

**Response `200 OK`**

```json
[
  {
    "questionId": 43,
    "questionContent": "다음 중 INNER JOIN에 대한 설명으로...",
    "subjectName": "SQL 기본",
    "wrongCount": 3,
    "lastWrongAt": "2026-04-01T14:30:00"
  }
]
```

**DTO:**

```java
public record WrongAnswerResponse(
    Long questionId,
    String questionContent,
    String subjectName,
    int wrongCount,
    LocalDateTime lastWrongAt
) {}
```

### `GET /api/wrong-answers/stats`

과목별 취약 영역 통계.

**Response `200 OK`**

```json
[
  {
    "subjectId": 5,
    "subjectName": "SQL 기본",
    "totalSolved": 30,
    "wrongCount": 12,
    "wrongRate": 40
  }
]
```

**DTO:**

```java
public record WrongAnswerStatsResponse(
    Long subjectId,
    String subjectName,
    int totalSolved,
    int wrongCount,
    int wrongRate
) {}
```

---

## 5. 인증 (Auth) / 회원 (Member)

### `POST /api/auth/login/{provider}`

OAuth 로그인. provider별 인가 코드를 받아 토큰을 발급한다.

| Path 변수 | 설명 |
|-----------|------|
| `provider` | OAuth provider (`google`, `github` 등) |

**Request Body**

```json
{
  "code": "oauth-authorization-code"
}
```

**DTO:**

```java
public record LoginRequest(String code) {}

public record LoginResponse(String accessToken, MemberResponse member) {}
```

**Response `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "member": {
    "id": 1,
    "nickname": "홍길동",
    "email": "hong@example.com",
    "profileImage": "https://..."
  }
}
```

### `GET /api/members/me`

내 프로필 조회. `Authorization: Bearer {token}` 필요.

**Response `200 OK`**

```json
{
  "id": 1,
  "nickname": "홍길동",
  "email": "hong@example.com",
  "profileImage": "https://..."
}
```

**DTO:**

```java
public record MemberResponse(
    Long id,
    String nickname,
    String email,
    String profileImage
) {}
```

### `PATCH /api/members/me`

프로필 수정.

**Request Body**

```json
{
  "nickname": "새닉네임"
}
```

**DTO:**

```java
public record MemberUpdateRequest(String nickname) {}
```

**Response `200 OK`** — `MemberResponse` 동일

---

## 테이블 구조

### `question` (V3에서 단순화됨)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGINT PK | auto_increment |
| `subject_id` | BIGINT FK | subject 참조 |
| `content` | TEXT | 문제 본문 (선택지 텍스트 포함) |
| `correct_option` | TINYINT | 정답 번호 (1-4) |
| `explanation` | TEXT | 해설 (nullable) |
| `created_at` | DATETIME(6) | |
| `updated_at` | DATETIME(6) | |

> `question_option`, `explanation` 테이블은 삭제됨. 정답률은 `solve_answer`에서 집계.

### `solve` (신규, Flyway V4)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGINT PK | auto_increment |
| `member_id` | BIGINT FK | member 참조 |
| `subject_id` | BIGINT FK | subject 참조 |
| `total_count` | INT | 총 문제 수 |
| `correct_count` | INT | 정답 수 |
| `score` | INT | 점수 (0-100) |
| `created_at` | DATETIME(6) | 풀이 시각 |
| `updated_at` | DATETIME(6) | |

### `solve_answer`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGINT PK | auto_increment |
| `solve_id` | BIGINT FK | solve 참조 |
| `question_id` | BIGINT FK | question 참조 |
| `selected_option` | TINYINT | 선택한 번호 (1-4) |
| `correct_option` | TINYINT | 정답 번호 (1-4) |
| `is_correct` | BOOLEAN | 정답 여부 |
| `created_at` | DATETIME(6) | |
| `updated_at` | DATETIME(6) | |

---

## 기술 사항

- **Swagger UI:** springdoc-openapi로 자동 생성 (`/swagger-ui/index.html`)
- **DTO:** Java `record` 사용 (불변, 간결)
- **에러 응답:** 표준 형식 사용

```json
{
  "status": 404,
  "message": "문제를 찾을 수 없습니다.",
  "code": "QUESTION_NOT_FOUND"
}
```

```java
public record ErrorResponse(int status, String message, String code) {}
```
