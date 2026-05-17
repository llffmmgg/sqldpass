# Validation Notes

## Passed

- `backend/.\gradlew.bat compileJava`
- `backend/.\gradlew.bat test --tests com.sqldpass.service.solve.SolveServiceTest`
- `backend/.\gradlew.bat test --tests com.sqldpass.controller.solve.SolveControllerTest`
- `backend/.\gradlew.bat test`
- `mobile/.\gradlew.bat :app:assembleDebug`

## Android SDK Setup

Android Studio was installed with `winget`, then Android command-line tools were installed from the official Google package:

- `commandlinetools-win-14742923_latest.zip`
- `platforms;android-35`
- `build-tools;35.0.0`
- `platform-tools`

The local SDK path is configured in ignored file `mobile/local.properties`:

```properties
sdk.dir=C:\\Users\\admin\\AppData\\Local\\Android\\Sdk
```

`mobile/.\gradlew.bat :app:assembleDebug` initially reached KSP and failed on Java/Kotlin target mismatch. `mobile/app/build.gradle` now sets both Java compile options and Kotlin JVM target to 17, and the debug APK build passes.
