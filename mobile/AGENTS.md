# Mobile Guidelines

## Stack

- Native Android app
- Kotlin
- Jetpack Compose + Material 3
- Room for offline cache
- Retrofit/OkHttp for API calls
- Google Sign-In for authentication
- Google Play Billing for subscriptions

## Commands

Run commands from `mobile/`.

- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:testDebugUnitTest`

## Local secrets

`GOOGLE_WEB_CLIENT_ID` (Android Google Sign-In ID 토큰 audience — GCP Web 클라이언트 ID) 는 빌드 시 APK 안 `res/values/strings.xml` 의 `google_web_client_id` 로 박힘. 읽기 우선순위:

1. `-PGOOGLE_WEB_CLIENT_ID=...` CLI 인자
2. `mobile/gradle.properties` — **커밋되는 파일이라 실제 값 두지 말 것**
3. `~/.gradle/gradle.properties` — 개발자 글로벌 (권장)
4. 환경변수 `ORG_GRADLE_PROJECT_GOOGLE_WEB_CLIENT_ID` — CI 표준
5. `mobile/local.properties` — gitignore. 로컬 한정 권장

비어 있어도 빌드는 통과(회귀 검증 가능). 단 Google 로그인이 즉시 `RESULT_CANCELED` 로 튕김.

GCP Console 에서 OAuth 클라이언트 2개 발급 필요:
- Android 유형 — 패키지 `com.sqldpass.app` + 디버그 SHA-1 (또는 릴리스 SHA-1)
- Web 유형 — 위 식별자가 본 변수에 들어가는 값

키스토어 / 릴리스 서명 / Play Store 자동 업로드는 별도 phase.

## Coding Rules

- Keep Android code under `app/src/main`.
- Do not reintroduce Capacitor or WebView loading for the main app experience.
- Preserve the existing 문어CBT web brand tone, logo, and practical CBT-focused UI language.
- Prefer small Compose screens, state holders, and repository APIs over large Activity logic.
- Store offline CBT content in Room and keep pending solve submissions idempotent with `clientSubmissionId`.

## Validation

- Verify Gradle sync and `:app:assembleDebug` when Android SDK is available.
- If Android SDK is missing, document the environment blocker instead of claiming Android build success.
