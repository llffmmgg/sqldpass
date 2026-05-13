# Step 2 — Android Gradle release 서명 + versionCode/versionName 설정

## 배경

`docs/ANDROID_LAUNCH.md` Step 5-2 ("mobile/android 빌드 설정") 는 release 빌드를 위해 `mobile/android/app/build.gradle` 의 `android.signingConfigs.release` 와 `buildTypes.release.signingConfig` 를 환경변수 기반으로 구성하라고 명시한다. Step 1 에서 `cap add android` 가 생성한 기본 build.gradle 은 debug 키스토어만 가지므로, 사용자가 키스토어를 생성하기 전에 빌드 설정 자체는 미리 들어가 있어야 release AAB 빌드 흐름이 막힘 없이 가능해진다.

versionCode/versionName 도 기본값(`1` / `"1.0"`)이 아니라 sqldpass 초기 출시에 맞는 값으로 명시한다.

## 작업 디렉터리

```
mobile/android/
```

## 변경 대상

- `mobile/android/app/build.gradle` (Capacitor 7 기본은 그루비 `.gradle`. 만약 `.gradle.kts` 가 생성됐다면 그쪽을 수정)

## 변경 내용

### 1) `defaultConfig` — versionCode/versionName

기존 `defaultConfig` 블록 안의 `versionCode`, `versionName` 을 다음으로 갱신:

```groovy
defaultConfig {
    applicationId "com.sqldpass.app"
    minSdkVersion rootProject.ext.minSdkVersion
    targetSdkVersion rootProject.ext.targetSdkVersion
    versionCode 1
    versionName "0.1.0"
    testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    aaptOptions {
        // Files and dirs to omit from the packaged assets dir, modified to accommodate modern web apps.
        // Default: https://android.googlesource.com/platform/frameworks/base/+/282e181b58cf72b6ca770dc7ca5f91f135444502/tools/aapt/AaptAssets.cpp#61
        ignoreAssetsPattern '!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~'
    }
}
```

`versionCode 1`, `versionName "0.1.0"` 만 명시적으로 갱신하면 됨. 나머지 줄은 Capacitor 기본값 유지.

### 2) `signingConfigs.release` 블록 추가

`android { ... }` 내부의 `defaultConfig` 다음에 `signingConfigs` 블록을 신설(또는 기존 블록에 release 추가):

```groovy
signingConfigs {
    release {
        def keystorePath = System.getenv("SQLDPASS_KEYSTORE_PATH")
        if (keystorePath) {
            storeFile file(keystorePath)
            storePassword System.getenv("SQLDPASS_KEYSTORE_PASS")
            keyAlias System.getenv("SQLDPASS_KEY_ALIAS") ?: "sqldpass"
            keyPassword System.getenv("SQLDPASS_KEY_PASS")
        }
    }
}
```

- 환경변수 4종: `SQLDPASS_KEYSTORE_PATH`, `SQLDPASS_KEYSTORE_PASS`, `SQLDPASS_KEY_ALIAS` (선택, 기본 `sqldpass`), `SQLDPASS_KEY_PASS`.
- `keystorePath` 가 비어 있으면 `signingConfigs.release` 블록 안이 비어 있는 상태로 남는다 — debug 빌드는 정상 동작, release 빌드는 Gradle 이 "signing config 누락" 으로 fail. 이게 의도된 안전 장치(키스토어 없는 환경에서 실수로 release 빌드해도 unsigned APK 가 안 나옴).

### 3) `buildTypes.release` 에 signingConfig 적용

기존 `buildTypes.release` 블록을 다음으로 갱신:

```groovy
buildTypes {
    release {
        minifyEnabled false
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        signingConfig signingConfigs.release
    }
}
```

`minifyEnabled` 는 Capacitor 의 외부 URL 모드 특성상 false 유지가 안전 (앱 자체 JS/리소스가 거의 없어 minify 이득 적고, 디버깅 난이도만 올라감). proguard 라인은 기본 그대로.

### 4) 다른 항목

`namespace`, `compileSdk`, `minSdkVersion`, `targetSdkVersion`, `dependencies` 블록은 손대지 마라. Capacitor 7 기본값이 이미 Play Store 요구치(`targetSdk 34+`)를 만족.

## 검증

```powershell
cd mobile/android

# Gradle wrapper 가 build.gradle 을 정상 파싱하는지
.\gradlew.bat tasks --quiet

# 환경변수 없는 상태에서 debug 빌드는 통과해야 함
.\gradlew.bat assembleDebug

# 환경변수 없이 release 빌드를 시도하면 "signing config missing" 으로 fail 해야 함 (의도된 동작)
# (선택 검증: 사용자가 키스토어 생성한 뒤 본인 환경에서 별도 확인)
```

성공 조건:
- `gradlew tasks` 가 build.gradle 파싱 에러 없이 통과
- `gradlew assembleDebug` 가 통과 (`mobile/android/app/build/outputs/apk/debug/app-debug.apk` 생성)
- 빌드 산출물은 `.gitignore` 의 `android/build/`, `android/app/build/` 로 제외되어 untracked 로 보이지 않음

## Acceptance Criteria

1. `mobile/android/app/build.gradle` 의 `defaultConfig.versionCode = 1`, `versionName = "0.1.0"`.
2. `signingConfigs.release` 블록이 환경변수 4종 (`SQLDPASS_KEYSTORE_PATH`, `SQLDPASS_KEYSTORE_PASS`, `SQLDPASS_KEY_ALIAS`, `SQLDPASS_KEY_PASS`) 을 읽는 형태로 추가됨.
3. `buildTypes.release.signingConfig = signingConfigs.release` 적용됨.
4. `gradlew assembleDebug` 통과 (debug 빌드 정상).
5. 환경변수 미설정 상태에서 release 빌드를 시도하면 fail (의도된 안전 장치 검증).
6. 키스토어 파일/비밀번호가 build.gradle 에 하드코딩되지 않음.

## 금지 사항

- 키스토어 비밀번호를 build.gradle 에 하드코딩하지 마라. **이유**: 키스토어 비밀번호가 git 에 노출되면 누구나 sqldpass 앱 사칭 APK 를 만들 수 있고, Play App Signing 으로도 완전 복구 불가능.
- `*.keystore`, `*.jks` 파일을 commit 하지 마라. **이유**: `mobile/.gitignore` 에 이미 명시. 키스토어는 사용자가 로컬에서 생성해 `$env:USERPROFILE\sqldpass-release.keystore` 같은 외부 경로에 보관.
- `keystore.properties` 파일을 만들지 마라. **이유**: Android 관용은 `keystore.properties` 또는 환경변수 둘 중 하나. `docs/ANDROID_LAUNCH.md` Step 5-2 가 환경변수 방식을 채택했으므로 그에 맞춘다. properties 파일은 실수로 commit 되는 사고가 잦다.
- `minifyEnabled true` 로 변경하지 마라. **이유**: Capacitor 외부 URL 모드라 앱 내 JS/리소스가 거의 없다. minify 이득은 미미하고, 디버깅 난이도와 첫 빌드 실패 가능성만 올라간다.
- `applicationId`, `minSdkVersion`, `targetSdkVersion`, `compileSdk`, `namespace` 를 변경하지 마라. **이유**: Capacitor 7 기본값이 이미 Play Store 정책 (target API 34+) 을 만족. 변경은 별도 검토 phase.
- `proguard-rules.pro` 에 룰을 추가하지 마라. **이유**: minify 가 비활성이라 룰이 동작하지 않는다.
- Step 1 에서 다룬 npm 의존성을 추가하지 마라. **이유**: 이 step 의 범위는 build.gradle 한 파일.

## 커밋

```powershell
git add mobile/android/app/build.gradle
git commit -m "feat(android): release signingConfig 환경변수 기반 + versionCode 1 / versionName 0.1.0 설정

- mobile/android/app/build.gradle 의 defaultConfig 에 versionCode=1, versionName=\"0.1.0\" 명시
- signingConfigs.release 블록 추가 (env: SQLDPASS_KEYSTORE_PATH/PASS, SQLDPASS_KEY_ALIAS/PASS)
- buildTypes.release.signingConfig = signingConfigs.release 적용
- 키스토어 자체는 사용자가 docs/ANDROID_LAUNCH.md Step 5-1 절차로 별도 생성/보관

Refs: phases/android-launch-prep/step2.md, docs/ANDROID_LAUNCH.md Step 5-2"

git push origin feat-android-launch-prep
```

## Status 규칙

- 성공: `phases/android-launch-prep/index.json` 의 step 2 status 를 `completed` 로, summary 에 "build.gradle release signingConfig (env 기반) + versionCode/Name 설정, assembleDebug OK" 한 줄 기록.
- 실패: 3회 시도 후에도 build.gradle 파싱 또는 assembleDebug 가 깨지면 `error` + `error_message` 기록.
- blocked: build.gradle 이 `.gradle.kts` (Kotlin DSL) 로 생성됐는데 그루비 문법 변환이 깔끔하지 않으면 `blocked` + `blocked_reason: "Kotlin DSL build.gradle.kts 로 생성됨 — DSL 변환 검토 필요"` 기록. (드물지만 Capacitor 8+ 가 KTS 로 전환할 가능성 대비.)
