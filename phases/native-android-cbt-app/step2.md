# Step 2 - Android Compose App

## Goal
Replace the incomplete Capacitor WebView shell in `mobile/` with a Kotlin Compose native Android app.

## Work
- Remove tracked Capacitor package/config/fallback files.
- Add Android Gradle project files under `mobile/`.
- Use Kotlin, Jetpack Compose, Material 3, Room, Retrofit/OkHttp, Google Sign-In, Play Billing.
- Reuse the existing `문어CBT` logo asset.
- Implement app structure around Home, Mock Exams, Offline, History, My tabs.
- Preserve the web brand tone while using native mobile layouts.

## Acceptance Criteria
- Android package is `com.sqldpass.app`.
- App name is `문어CBT`.
- UI is not a WebView and does not load `https://www.sqldpass.com`.
- Data layer contains API, Room cache, token store, sync queue, and billing/auth integration points.
- `mobile/AGENTS.md` documents Android-specific guidance.

## Validation
```powershell
.\gradlew.bat :app:assembleDebug
```

