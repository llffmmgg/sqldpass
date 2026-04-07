# sqldpass

> SQLD · 정보처리기사 실기 무료 CBT 모의고사 플랫폼
> https://www.sqldpass.com

매번 새로 추가되는 실전형 문제를 풀고, 회차별 점수와 오답을 자동으로 추적할 수 있는 웹 서비스입니다. 회원가입 없이 학습 허브에서 기출 문제를 둘러볼 수 있고, Google 로그인 후에는 모의고사 응시 / 카테고리 무한 풀이 / 오답 노트 / 회차별 통계 기능을 사용할 수 있습니다.

## 주요 기능

- **즉석 랜덤 모의고사** — 누를 때마다 새 문제로 구성된 SQLD 50문항 / 정보처리기사 실기 20문항 세트
- **카테고리 무한 풀이** — 과목별로 문제를 끝없이 풀이. 정처기 모의고사로 만들어진 문제도 자동으로 풀에 합류
- **오답 자동 복습** — 자격증별로 틀린 문제만 모아 취약 영역을 분석
- **회차별 실력 추적** — 점수 추이, 풀이 시간, 연속 학습일 등을 대시보드에서 확인
- **공개 콘텐츠 허브 (`/learn`)** — 비로그인 유입 대응을 위한 SSG/ISR 페이지 + JSON-LD 구조화 데이터
- **관리자 도구** — 모의고사 생성/삭제, 회원별 학습 대시보드, AI 문제 자동 생성 모니터링

## 기술 스택

| 영역 | 기술 |
|---|---|
| **백엔드** | Java 21 · Spring Boot 4 · Spring Data JPA · Spring AI · MySQL 8 · Flyway · Gradle 9 |
| **프론트엔드** | Next.js 16 (App Router) · React 19 · TypeScript 5 · Tailwind CSS 4 · react-markdown · react-syntax-highlighter |
| **AI** | Anthropic Claude (생성) / Google Gemini (검수) — Spring AI 어댑터로 분기 |
| **인증** | Google OAuth 2.0 + 자체 JWT |
| **인프라** | Docker Compose · Vercel(frontend) · 자체 호스팅(backend) · Loki + Promtail (관측) |

## 모노레포 구조

```
sqldpass/
├── backend/         Spring Boot API 서버 (Java 21, Gradle)
├── frontend/        Next.js App Router (TypeScript)
├── docs/            설계/운영 문서
├── observability/   Loki + Promtail 설정
├── proxy/           리버스 프록시 설정
├── scripts/         운영 스크립트
└── docker-compose.yaml    로컬 개발용 MySQL
```

각 디렉토리에는 더 세부적인 가이드를 담은 `CLAUDE.md` 가 함께 있습니다.

## 빠른 시작

### 사전 준비
- Java 21
- Node.js 20+
- Docker (로컬 MySQL 용)

### 1) MySQL 띄우기
```bash
docker compose up -d
# mysql://localhost:3307 (database: sqldpass)
```

### 2) 백엔드 실행
```bash
cd backend
./gradlew bootRun     # macOS / Linux / Git Bash
gradlew.bat bootRun   # Windows CMD/PowerShell
```
- 기본 프로필: `local`
- 포트: `8080`
- Swagger: http://localhost:8080/swagger-ui/index.html

환경 변수 (없어도 동작하지만 AI/OAuth는 비활성):
```
ANTHROPIC_API_KEY=...
GOOGLE_GENAI_API_KEY=...
GOOGLE_OAUTH_CLIENT_ID=...
GOOGLE_OAUTH_CLIENT_SECRET=...
JWT_SECRET=local-dev-secret-key-minimum-32-characters-long
```

### 3) 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
# http://localhost:3000
```

`frontend/.env.local` 에 백엔드 URL 지정 (기본 `http://localhost:8080`):
```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## 백엔드 빌드 & 테스트

```bash
cd backend
./gradlew build              # 컴파일 + 테스트 + jar
./gradlew test               # 테스트만
./gradlew bootRun            # 로컬 실행
```

## 프론트엔드 빌드 & 린트

```bash
cd frontend
npm run dev          # 개발 서버
npm run build        # 프로덕션 빌드
npm run start        # 프로덕션 서버
npm run lint         # ESLint
```

## 아키텍처 노트

### 자격증별 모의고사 생성 흐름
- **SQLD**: 자동 스케줄러/관리자가 카테고리별 풀에 문제를 채워 둠 → 모의고사 생성 시 풀에서 50문제 추출
- **정처기 실기**: 풀 사전 적재 없음. 모의고사 생성 시점에 카테고리 분포 템플릿(4종 회전) + 시드 풀(카테고리당 5개) 기반으로 AI에 N개 변형을 즉석 요청 → 생성된 20문제는 자동으로 카테고리 무한 풀이 풀에도 합류

### 다양성 보장 (정처기)
- 카테고리당 5개 시드를 풀에서 무작위 추출 → 시드별 변형 1개 생성 (난이도 1/3/5 자연 분산)
- 시드 코드의 식별자(함수/클래스/변수명) + 직전 30개 출제 식별자 = forbidden identifier 목록을 AI에 전달
- 최근 30개 정답과 8자 이상 일치하는 결과 발견 시 강화된 회피 신호로 1회 재시도

### Layer 규칙 (백엔드)
```
[조회] DB → Entity → Mapper → Domain → Service → Controller → Response DTO
[저장] Request DTO → Service → Entity 직접 생성 → DB
```
- `domain/`은 JPA 의존성 없는 순수 POJO
- `persistent/`의 Mapper만 Entity↔Domain 변환 담당
- DTO는 `controller/`의 Java `record`

자세한 규칙은 `backend/CLAUDE.md` 참조.

### 프론트 핵심 라우트
- `/` 랜딩
- `/learn`, `/learn/[cert]`, `/learn/[cert]/[category]` 공개 콘텐츠 (SSG/ISR)
- `/q/[id]` 개별 문제 공개 페이지 (SEO 핵심)
- `/solve` 카테고리 무한 풀이
- `/mock-exams`, `/mock-exams/[id]` 모의고사 (게스트 미리보기 + 로그인 후 응시)
- `/dashboard` 학습 대시보드
- `/admin/**` 관리자

## 운영

- **DB 마이그레이션**: Flyway. `backend/src/main/resources/db/migration/V*.sql`
- **로그**: Spring Boot → Promtail → Loki. 1초 이상 걸린 쿼리는 WARN으로 자동 수집
- **AI 호출 모니터링**: 생성/검수 결과를 Discord webhook으로 전송
- **Search Console / 네이버 서치어드바이저**: `https://www.sqldpass.com` 로 등록 (sitemap `/sitemap.xml`)

## 라이선스

Private project. All rights reserved.
