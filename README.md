# sqldpass

https://www.sqldpass.com

SQLD, 정보처리기사 실기, 컴퓨터활용능력 1급 필기 CBT 사이트입니다.

AI로 매번 새로운 변형 문제를 생성하기 때문에, 답을 외우지 않고 연습할 수 있습니다.

## 기능

- 모의고사 (SQLD 50문항 / 정처기 실기 20문항 / 컴활 60문항)
- 카테고리별 무한 풀이
- 오답 자동 복습
- 회차별 점수 추이 대시보드
- Google 로그인

## 기술 스택

- 백엔드: Java 21, Spring Boot 4, JPA, MySQL 8, Flyway
- 프론트엔드: Next.js 16, React 19, TypeScript, Tailwind CSS 4
- AI: Anthropic Claude (생성), Google Gemini (검수)
- 인프라: Docker Compose, Vercel, nginx, GitHub Actions

## 프로젝트 구조

```
sqldpass/
├── backend/         Spring Boot API
├── frontend/        Next.js
├── observability/   Loki + Promtail
├── proxy/           nginx
├── scripts/         운영 스크립트
└── docker-compose.yaml
```

## 로컬 실행

```bash
docker compose up -d
cd backend && ./gradlew bootRun
cd frontend && npm install && npm run dev
```

필요한 환경변수:

```
ANTHROPIC_API_KEY=
GOOGLE_GENAI_API_KEY=
GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
JWT_SECRET=
```

## 라이선스

[MIT](./LICENSE)
