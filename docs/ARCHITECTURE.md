# 아키텍처

## 디렉토리 구조

```text
sqldpass/
├─ frontend/                 # Next.js 16, React 19, TypeScript, Tailwind CSS 4
│  ├─ src/
│  │  ├─ app/                # App Router 라우트 (페이지·레이아웃·route handler)
│  │  ├─ components/         # 재사용 UI (ui/ primitive + 도메인 컴포넌트)
│  │  ├─ hooks/              # React hooks
│  │  ├─ lib/                # API 클라이언트, 도메인 헬퍼, offlineStore, payment, platform
│  │  └─ types/              # TypeScript 타입
│  ├─ content/blog/          # MDX 블로그 100여 편
│  └─ public/                # 정적 자원, sw.js, manifest.json
├─ backend/                  # Spring Boot 4, Java 21, Gradle
│  ├─ src/main/java/com/sqldpass/
│  │  ├─ config/             # WebMvcConfig, 인터셉터 3종, Jpa·Cache·Scheduling·AiClient·Flyway
│  │  ├─ controller/         # admin/, auth/, payment/, member/, question/, solve/,
│  │  │                       publicapi/, post/, mockexam/, notice/, notification/,
│  │  │                       upload/, pdf/, content/, bookmark/, feedback/, wronganswer/, streak/, subject/
│  │  ├─ domain/             # 순수 POJO 도메인 모델
│  │  ├─ persistent/         # JPA Entity·Repository·Mapper
│  │  └─ service/            # 비즈니스 로직 (도메인별 + payment, generation, upload, notification, pdf, grading)
│  └─ src/main/resources/
│     ├─ application*.yaml   # base / local / prod 프로필
│     └─ db/migration/       # Flyway V1–V79
├─ mobile/                   # Capacitor 7 (appId com.sqldpass.app, appName 문어CBT)
│  ├─ capacitor.config.ts    # external URL 모드 → https://www.sqldpass.com
│  └─ web/                   # Capacitor가 참조하는 웹 셸
├─ proxy/                    # OCI nginx (운영 단일 진입점)
│  ├─ nginx.conf · nginx-common.conf · nginx-init.conf
│  └─ docker-compose.yml
├─ .github/workflows/        # ci.yml, cd.yml, frontend-cd.yml, proxy-cd.yml
├─ docs/                     # PRD · ARCHITECTURE · ADR · UI_GUIDE · ANDROID_LAUNCH · IMPLEMENTATION · PROGRESS
├─ phases/                   # Harness phase·step (실행 결과 산출물)
└─ scripts/                  # execute.py + hooks/ (양 에이전트 공유 PowerShell hook)
```

## 런타임 토폴로지 (운영)

```text
사용자 브라우저 ── DNS ── api.sqldpass.com / www.sqldpass.com
                          │
                          ▼
                  OCI nginx (10.0.0.72:443, certbot DNS challenge)
                  ├─ /api/*  → Spring Boot :8080
                  └─ /        → Next.js :3000
```

- 운영은 `api.sqldpass.com`·`www.sqldpass.com` 모두 OCI nginx에서 종단. Vercel은 staging(`stg.sqldpass.com`) 전용.
- 프론트엔드 코드는 항상 상대 경로 `/api/*`로 호출 (별도 BASE 없음). 로컬 개발만 `NEXT_PUBLIC_API_URL` env가 있을 때 `next.config.ts`의 rewrites로 우회.
- 메모리/문서에 남은 "Vercel rewrites → OCI nginx → Spring Boot" 흐름의 핵심은 **운영 OCI 단일 종단**.

## 패턴

- 프론트엔드: Next.js App Router 기준. 페이지·레이아웃·route handler를 `frontend/src/app/`에 두고, 재사용 UI는 `frontend/src/components/`(특히 `components/ui/` primitive). 도메인 헬퍼·API 클라이언트는 `frontend/src/lib/`.
- 백엔드: Controller → Service → Persistent(JPA) → Domain(POJO) 흐름. HTTP DTO와 도메인·엔티티의 책임을 분리하고, `persistent/{domain}/Mapper`가 Entity → Domain 변환을 담당 (단방향).
- 공통: API 계약 변경은 프론트엔드 타입/API 호출부와 백엔드 Controller/DTO를 동시에 갱신.

## 인증·권한

JWT(Bearer, 7일 TTL)만 사용. 세션·CSRF 토큰은 사용하지 않음.

`backend/.../config/WebMvcConfig.java`에 인증 인터셉터 3종 + 캐시 제어 1종을 경로 패턴으로 등록한다.

| 인터셉터 | 역할 | 적용 경로 |
|----------|------|-----------|
| `MemberAuthInterceptor` | 토큰 필수, memberId 주입 | `/api/solves/**`, `/api/payment/**`, `/api/wrong-answers/**` 등 |
| `AdminAuthInterceptor` | 관리자 토큰 필수 | `/api/admin/**` (단 `/api/admin/login` 제외) |
| `OptionalMemberAuthInterceptor` | 토큰 있으면 주입, 없어도 통과 | `/api/posts/**`, `/api/public/**` 등 |
| `PublicCacheControlInterceptor` | `Cache-Control: public, max-age=1800` | `/api/public/**` GET |

Spring Security 메서드 어노테이션(`@PreAuthorize` 등)은 사용하지 않는다. 권한 정책은 인터셉터 + `SubscriptionService` 같은 서비스 계층에서 관리.

## 데이터 흐름

```text
사용자
  → frontend/src/app (RSC + Client Component)
  → frontend/src/components
  → frontend/src/lib API client (fetch + Bearer)
  → OCI nginx /api/*
  → backend Controller (인터셉터로 인증)
  → backend Service (도메인 로직 + 트랜잭션 경계)
  → backend Persistent/Domain
  → MySQL
```

## 상태 관리

- 서버 데이터는 API 응답 타입과 화면 상태를 분리한다.
- 클라이언트 상호작용은 React state/hooks를 우선 사용 (전역 store는 도입하지 않음).
- 백엔드 트랜잭션 경계는 Service 계층에서 관리.
- 오프라인 상태(앱)는 `frontend/src/lib/offlineStore.ts`의 IndexedDB로 분리 관리, `OfflineSyncManager`가 큐 드레인을 담당.

## 외부 연동

| 영역 | 라이브러리/서비스 | 위치 |
|------|------------------|------|
| 결제(웹) | PortOne v2 (KakaoPay EASY_PAY) | `service/payment/PortOneClient`, `frontend/src/lib/payment.ts` |
| 결제(앱) | Google Play Billing + Android Publisher API | `service/payment/PlayBillingClient`, `controller/payment/PaymentWebhookController` (RTDN) |
| AI 문제 생성 | Spring AI BOM 2.0.0-M4 — Claude Sonnet 4 / Gemini 2.5 Flash Lite / GPT-4o | `service/generation/` + `QuestionGenerationScheduler` |
| 인증 | Google OAuth (web code + 앱 ID 토큰) | `controller/auth/`, `service/auth/` |
| 이미지 업로드 | Cloudflare R2 presigned PUT | `service/upload/R2UploadService` |
| PDF 렌더링 | Playwright Chromium (`/print` 페이지 → R2 캐시) | `service/pdf/` |
| 알림(in-app) | DB `Notification` + `DiscordNotifier` 웹훅 | `service/notification/` |
| FCM 푸시 | (v1.1 보류) | — |

## 데이터베이스

- MySQL 8.0+, HikariCP (`prod` 풀 max 20 / min-idle 5).
- Flyway V1–V79 버전 마이그레이션. 롤백 스크립트는 두지 않고 forward-only.
- `spring.jpa.hibernate.ddl-auto: validate` — 스키마 변경은 항상 마이그레이션을 통해서만.
- Seed 데이터는 별도 스크립트 없이 마이그레이션 DML로 (`V2__insert_subject_seed.sql` 등).
- 결제 스키마는 `V74~V79` (payment·subscription 테이블, prorate 컬럼, provider enum, purchase_token).
- 모든 엔티티는 `BaseTimeEntity`를 상속하여 `createdAt`/`updatedAt` audit 필드를 가진다.

## 빌드·배포

- `.github/workflows/ci.yml`: 백엔드 PR Gradle 테스트 (5분 timeout).
- `.github/workflows/cd.yml`: 백엔드 main push → ARM 러너에서 Docker 빌드 → ghcr.io → OCI 8081(test) 워밍 후 8080 swap (Blue-Green, 1–3초 단절).
- `.github/workflows/frontend-cd.yml`: Next.js 3-stage Dockerfile, `--build-arg`로 `NEXT_PUBLIC_*` 주입, 3001 → 3000 swap.
- `.github/workflows/proxy-cd.yml`: nginx.conf SCP → `nginx -t` 검증 → 무중단 reload.

## 검증 명령

프론트엔드:

```powershell
cd frontend
npm run lint
npm run build
```

백엔드:

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

전체 (Harness Stop hook과 동일):

```powershell
.\scripts\hooks\stop-validation.ps1
```
