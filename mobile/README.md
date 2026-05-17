# 문어CBT Android

Native Android CBT client for sqldpass.

## Local Setup

1. Install Android Studio or Android SDK Command-line Tools.
2. Set `ANDROID_HOME` or create `mobile/local.properties`:

```properties
sdk.dir=C:\\Users\\admin\\AppData\\Local\\Android\\Sdk
```

3. Provide the Google OAuth web client ID used for Android Google Sign-In ID token validation:

```properties
GOOGLE_WEB_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

You can put that in `~/.gradle/gradle.properties`, pass `-PGOOGLE_WEB_CLIENT_ID=...`, or place it in `mobile/gradle.properties` for local-only testing.
4. Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Notes

- The app is native Compose UI, not a WebView shell.
- Backend API base URL defaults to `https://www.sqldpass.com/`.
- Offline CBT content is loaded through `GET /api/mobile/content/snapshot`.
- Pending offline solve submissions use `clientSubmissionId` for idempotent server sync.
