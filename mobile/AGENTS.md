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

## Coding Rules

- Keep Android code under `app/src/main`.
- Do not reintroduce Capacitor or WebView loading for the main app experience.
- Preserve the existing 문어CBT web brand tone, logo, and practical CBT-focused UI language.
- Prefer small Compose screens, state holders, and repository APIs over large Activity logic.
- Store offline CBT content in Room and keep pending solve submissions idempotent with `clientSubmissionId`.

## Validation

- Verify Gradle sync and `:app:assembleDebug` when Android SDK is available.
- If Android SDK is missing, document the environment blocker instead of claiming Android build success.
