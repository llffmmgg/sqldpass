# PRD: sqldpass

## 목표

자격증 학습자가 **랜덤 풀이 → 모의고사 → 오답 회독 → 진척도 확인**까지 한 곳에서 끊김 없이 진행하도록 한다. 광고·결제·게시판은 학습 흐름을 보조하는 위치에 둔다.

## 사용자

- 자격증 시험을 준비하는 학습자 (1차 페르소나)
- 기출/모의고사를 반복 풀이하며 취약 영역을 좁히려는 사용자
- 합격 수기·정보를 공유·소비하는 사용자 (게시판)
- 콘텐츠와 운영 데이터를 관리하는 운영자 (관리자)

## 지원 자격증

`backend/src/main/resources/db/migration/V2__insert_subject_seed.sql` 기준 6종.

- SQLD
- 정보처리기사 필기 / 실기 (engineer-written, engineer-practical)
- 컴퓨터활용능력 1급 / 2급 (cl1, cl2)
- ADsP

라우트·시그니처 색상은 자격증별로 분기되며, `/blog`도 자격증 카테고리 단위로 구성된다.

## 핵심 사용자 흐름

| 라우트 | 흐름 | 인증 |
|--------|------|------|
| `/solve` | 랜덤 풀이(객관식·단답·서술) | 비로그인 가능 (IP 쿼터) |
| `/cbt-mock-exam` | CBT 모드 모의고사 | 비로그인 가능 |
| `/mock-exams` | 회차 카탈로그 | 비로그인 가능 |
| `/past-exams`, `/learn` | 기출 (SSR + ISR) | 비로그인 가능 |
| `/wrong-answers` | 오답 회독·재시도 | 로그인 필수 |
| `/dashboard` | 활동 그래프·스트릭·뱃지 | 로그인 필수 |
| `/history` | 풀이 기록 요약 | 로그인 필수 |
| `/profile` | 닉네임·구독·탈퇴 | 로그인 필수 |
| `/checkout` | 결제 (PortOne/Play Billing 분기) | 로그인 필수 |
| `/admin/**` | 관리자 콘솔 | 관리자 JWT |

## 부가 기능

- `/board` 합격 수기 게시판 — `category=PASS_REVIEW`, 게시 상태 `PENDING/PUBLISHED` 모더레이션, 댓글
- `/blog` MDX 100여 편 — 자격증별 가이드·기출 분석·전략, SSR + ISR
- 정적 페이지: `/about`, `/changelog`, `/privacy`, `/terms`, `/refund`
- `/q/{shortlink}` 문제 딥링크 (공유용)
- `/print` 인쇄 최적화 페이지 (Playwright PDF 렌더링 진입점)

## 구독 / 결제 모델

- 4티어: FREE / THREE_DAY / ONE_MONTH / UNLIMITED
- 가격: 3,900₩ / 9,900₩ / 29,900₩ — UNLIMITED는 만료 없음
- 업그레이드 시 잔여일수 prorate 차감
- **이중 결제 채널**: 웹은 PortOne v2 (KakaoPay EASY_PAY), Android 앱은 Play Billing
- MVP 단계에서는 reviewer whitelist eligibility 게이트가 `/checkout`을 제어 (정식 출시 시 빈 리스트 → 전체 공개)
- 환불은 RTDN(Real-time Developer Notifications) 웹훅으로 자동 동기화

## Android 앱 전용 기능

웹 앱과 동일한 코드베이스를 Capacitor 7 웹뷰로 띄우되, 앱에서만 동작하는 기능:

- Service Worker 등록 (HTML NetworkFirst, static CacheFirst, `/api/*` 제외)
- IndexedDB 오프라인 모의고사 큐 (`offlineStore.ts`) — 비행기 모드에서 풀이 후 온라인 복귀 시 자동 sync
- Play Billing 분기 (`isCapacitorApp()` → `Capacitor.Plugins.Billing.purchase()`)
- 네이티브 Google Sign-In ID 토큰 흐름 (`/api/auth/login/google/idtoken`)
- 앱 표시 이름은 `문어CBT` (`mobile/capacitor.config.ts`)

## MVP 제외

- iOS 앱 (안드로이드 우선)
- FCM 푸시 알림 (v1.1로 보류, in-app `Notification` 도메인은 백엔드에 존재)
- 신규 시험 종목 추가 (현 6종에 집중)
- API 계약 없이 프론트엔드만 선행하는 기능

## 기술 전제

- 프론트엔드: `frontend/` Next.js 16, React 19, TypeScript, Tailwind CSS 4
- 백엔드: `backend/` Spring Boot 4, Java 21, Gradle, MySQL 8, Flyway
- 모바일: `mobile/` Capacitor 7 (외부 URL 모드, `https://www.sqldpass.com`)
- 인프라: `proxy/` OCI nginx 단일 진입점, GitHub Actions 4종 워크플로우
- Harness: 루트 `phases/` + `scripts/execute.py`로 작업 계획·실행 흐름 관리
