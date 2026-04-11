# sqldpass

> **자격증 시험, 매번 새로운 문제로 연습하자.**
>
> https://www.sqldpass.com

SQLD, 정보처리기사 실기, 컴퓨터활용능력 1급 필기를 한 곳에서 풀 수 있는 무료 CBT 플랫폼입니다.

기존 CBT 사이트는 문제가 고정되어 있어서 몇 번 풀면 답이 외워집니다. sqldpass는 **AI가 검증된 시드 문제를 기반으로 변형 문제를 생성**하기 때문에, 매번 새로운 문제로 연습할 수 있습니다.

---

## 이런 걸 할 수 있습니다

**모의고사** — SQLD 50문항, 정처기 실기 20문항, 컴활 60문항. 실제 시험과 동일한 구성으로 매번 새로운 세트가 생성됩니다.

**카테고리 무한 풀이** — 과목별로 끝없이 문제를 풀 수 있습니다. 이미 푼 문제는 후순위로 밀려납니다.

**오답 자동 복습** — 틀린 문제만 모아서 다시 풀기. 2번 연속 맞추면 자동으로 마스터 처리됩니다.

**채점 결과** — 손글씨 빨간 색연필 느낌의 채점 UI. 회차별 점수 추이를 대시보드에서 추적합니다.

**비로그인 미리보기** — Google 로그인 없이도 모의고사 목록과 문제 미리보기를 볼 수 있습니다.

---

## 기술적으로 재미있는 부분

### AI 문제 생성 파이프라인

사람이 검증한 시드 문제를 기반으로, AI가 변형 문제를 생성합니다.

- **토픽 단위 생성** — 세부 주제별로 기본/심화/고난도 3문제를 동시 생성
- **Few-shot 78개** — 출제 스타일을 학습시켜 품질 유지
- **hash 기반 중복 차단** — 회차 간/내 본문 유사도를 체크해서 중복 방지
- **chunk 분할 호출** — AI 응답이 잘리는 문제를 영구 해결

### AI 검증 파이프라인

AI가 만든 문제를 다시 AI가 검증합니다.

- 정답 정확도, 해설 일치, 난이도 적절성, 오타를 자동 검사
- 단순 오류는 자동 수정, 복잡한 건 관리자 리뷰 큐로 분류
- 생성은 Anthropic Claude, 검증은 교차 검증으로 신뢰도 확보

### 정처기 실기 — 단답형/서술형 채점

정처기 실기는 4지선다가 아닙니다. 단답형과 서술형 채점 엔진을 별도로 구현했습니다.

- 단답형: 정규화 비교 (대소문자, 공백, 특수문자 처리)
- 서술형: 키워드 포함 여부 + 부분 점수

### 무중단 배포

포트 재바인딩 방식으로 다운타임 0에 가까운 배포를 구현했습니다. 새 컨테이너를 다른 포트에 띄우고, 헬스체크 통과 후 nginx upstream을 전환합니다.

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| **백엔드** | Java 21 · Spring Boot 4 · Spring Data JPA · Spring AI · MySQL 8 · Flyway |
| **프론트엔드** | Next.js 16 (App Router) · React 19 · TypeScript · Tailwind CSS 4 |
| **AI** | Anthropic Claude (생성) · Google Gemini (검수) |
| **인증** | Google OAuth 2.0 + JWT |
| **인프라** | Docker Compose · Vercel · Loki + Grafana · GitHub Actions CI/CD |

---

## 프로젝트 구조

```
sqldpass/
├── backend/         Spring Boot API 서버
├── frontend/        Next.js 프론트엔드
├── observability/   Loki + Promtail 설정
├── proxy/           nginx 리버스 프록시
├── scripts/         운영 스크립트
└── docker-compose.yaml
```

---

## 로컬 실행

```bash
# 1. MySQL
docker compose up -d

# 2. 백엔드 (localhost:8080)
cd backend && ./gradlew bootRun

# 3. 프론트엔드 (localhost:3000)
cd frontend && npm install && npm run dev
```

AI 생성/OAuth는 환경변수 설정이 필요합니다. 없어도 기본 기능은 동작합니다.

<details>
<summary>환경변수 목록</summary>

```
ANTHROPIC_API_KEY=...
GOOGLE_GENAI_API_KEY=...
GOOGLE_OAUTH_CLIENT_ID=...
GOOGLE_OAUTH_CLIENT_SECRET=...
JWT_SECRET=local-dev-secret-key-minimum-32-characters-long
```

</details>

---

## 기여

이슈나 PR은 언제든 환영합니다.

1. Fork 후 브랜치 생성
2. 변경 사항 커밋 (커밋 메시지는 한국어, 접두어는 영어: `feat: 로그인 유효성 검사 추가`)
3. PR 제출

---

## 라이선스

[MIT License](./LICENSE)
