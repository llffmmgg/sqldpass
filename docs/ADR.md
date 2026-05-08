# Architecture Decision Records

## 철학

sqldpass는 프론트엔드와 백엔드를 함께 관리하는 모노레포다. 기능 속도보다 API 계약의 일관성, 테스트 가능한 백엔드 로직, 유지보수 가능한 UI 구조를 우선한다.

---

### ADR-001: Next.js App Router 기반 프론트엔드

**결정**: 프론트엔드는 `frontend/`의 Next.js 16 App Router, React 19, TypeScript, Tailwind CSS 4를 기준으로 개발한다.

**이유**: 라우트 단위 화면 구성, 서버/클라이언트 컴포넌트 분리, 정적 콘텐츠와 앱 UI를 한 프로젝트에서 관리하기 쉽다.

**트레이드오프**: App Router와 React 최신 버전에 맞춘 패턴을 유지해야 하며, 라이브러리 호환성을 확인해야 한다.

### ADR-002: Spring Boot 계층형 백엔드

**결정**: 백엔드는 `backend/`의 Spring Boot 4, Java 21, Gradle을 사용하고 Controller, Service, Persistent, Domain 계층을 유지한다.

**이유**: HTTP API, 비즈니스 로직, 영속성 책임을 분리해 테스트와 변경 영향 분석을 쉽게 한다.

**트레이드오프**: 작은 기능도 계층별 파일이 늘어날 수 있으므로, 불필요한 추상화는 피한다.

### ADR-003: 루트 Harness, 앱별 실행

**결정**: Harness phase와 공통 문서는 루트에서 관리하고, 실제 명령은 `frontend/` 또는 `backend/` 작업 디렉터리에서 실행한다.

**이유**: 기능은 두 앱을 함께 건드릴 수 있지만 빌드 도구와 검증 명령은 앱별로 다르다.

**트레이드오프**: step 파일에 작업 범위와 실행 디렉터리를 명확히 쓰지 않으면 루트에서 잘못된 명령을 실행할 수 있다.

### ADR-004: 이중 결제 채널 (PortOne + Play Billing)

**결정**: 웹 결제는 PortOne v2(KakaoPay EASY_PAY), 안드로이드 앱 결제는 Google Play Billing을 사용한다. 두 흐름은 단일 `SubscriptionEntity`로 합쳐지고, `provider` enum과 `purchase_token` 컬럼(`V77~V79`)으로 구분한다. 환불은 RTDN 웹훅(`/api/webhook/play-billing/rtdn`)으로 동기화한다.

**이유**: Play Store 정책상 디지털 상품은 인앱 결제 강제. 웹은 한국 결제 환경에 맞춰 PortOne을 그대로 유지한다.

**트레이드오프**: 결제 검증·환불 동기화 로직이 두 가지로 갈라진다 — `PaymentService.verify()` 분기를 단순하게 유지해야 함.

### ADR-005: OCI nginx 단일 운영 진입점

**결정**: 운영의 `api.sqldpass.com`·`www.sqldpass.com` 모두 OCI nginx(10.0.0.72)에서 종단한다. Vercel은 staging(`stg.sqldpass.com`) 전용. 프론트엔드 코드는 항상 상대 경로 `/api/*`로 호출한다.

**이유**: AI 문제 생성·Playwright PDF 렌더링·결제 검증처럼 장기 응답이나 외부 API를 거치는 워크로드를 Vercel serverless 한도(60s) 안에 가둘 수 없다. 자체 호스트로 비용·관측성·로그를 직접 통제한다.

**트레이드오프**: OCI nginx 인스턴스가 SPOF. 무중단 reload(`nginx -t` + reload)와 백엔드 Blue-Green 스왑(8081 → 8080)으로 단절을 1–3초로 제한.

### ADR-006: 인터셉터 기반 인증, Spring Security 메서드 어노테이션 미사용

**결정**: `WebMvcConfig`에 `MemberAuthInterceptor`/`AdminAuthInterceptor`/`OptionalMemberAuthInterceptor` 3종을 경로 패턴으로 등록한다. `@PreAuthorize`나 메서드 단위 권한 어노테이션은 도입하지 않는다.

**이유**: JWT 단일 토큰, 역할은 admin·member 2단계, 게이팅 기준이 경로 단위로 명확하다. 인터셉터로 충분하고 권한 분기 비용이 낮다.

**트레이드오프**: 권한 정책이 컨트롤러 어노테이션이 아니라 `WebMvcConfig`에 분산되므로, 새 컨트롤러를 만들 때 어떤 인터셉터에 매칭되는지 항상 확인해야 한다.

### ADR-007: Capacitor 외부 URL 모드 + IndexedDB 오프라인 큐

**결정**: 안드로이드 앱은 네이티브 화면을 두지 않고 `https://www.sqldpass.com`을 Capacitor 7 웹뷰로 띄운다(`mobile/capacitor.config.ts`의 `server.url`). 오프라인 모의고사 풀이는 `frontend/src/lib/offlineStore.ts` IndexedDB와 `OfflineSyncManager`로 큐잉하고, 온라인 복귀 시 `submitSolveOffline()`이 자동 sync 한다.

**이유**: 단일 코드베이스로 웹·앱을 동시에 운영, 네이티브 화면 분리 비용을 회피.

**트레이드오프**: 첫 부팅에 콘텐츠 prefetch가 필요(`/api/content/snapshot` ETag 캐시). 복잡한 네이티브 UI가 필요해지면 모델 자체를 재검토해야 한다.

### ADR-008: Service Worker는 앱 한정

**결정**: Service Worker(`public/sw.js`)는 Capacitor 환경에서만 등록한다. 웹에서 기존에 등록된 SW는 `ServiceWorkerRegistrar.tsx`에서 unregister + cache cleanup으로 청소한다(commit `c5122ad`).

**이유**: 웹은 Vercel 캐시·Next 라우터로 충분하고, SW가 오히려 배포 후 잔류 캐시·디버깅 난이도를 키운다. 앱은 오프라인 셸 캐시(NetworkFirst HTML / CacheFirst static, `/api/*` 제외)가 필수.

**트레이드오프**: 같은 코드베이스 안에서 환경별로 동작이 갈라지므로, SW 변경 시 앱·웹 양쪽 영향을 명시적으로 검토해야 한다.

### ADR-009: AI 문제 생성은 Spring AI 다중 provider

**결정**: Spring AI BOM 2.0.0-M4 위에서 Claude Sonnet 4를 주 generator로, Gemini 2.5 Flash Lite를 보조, GPT-4o를 폴백으로 두고 `sqldpass.ai.generator.provider` 설정으로 분기한다. 일일 cron(`QuestionGenerationScheduler`, dev 비활성), 컨텐츠 해시 중복 차단(`V22`), `SqldMcqGenerationValidator` 후처리로 품질을 유지한다.

**이유**: 단일 provider 장애·비용 변동·정책 변경 위험을 분산. 모델별 강점이 달라(Claude 추론, Gemini 다양성, GPT 안정성) 폴백 체인이 의미 있다.

**트레이드오프**: provider 추가/교체 시 `AiClientConfig` + 환경 변수 + 키 발급이 동시에 필요. 모델별 응답 형식 차이는 `QuestionGenerationService` 내부에서 정규화해야 한다.
