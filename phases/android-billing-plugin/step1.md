# Step 1 — Gradle 의존성: Kotlin + Google Play Billing Library v6

## 배경

`mobile/android/` 네이티브 프로젝트는 직전 phase(`android-launch-prep`)에서 Capacitor 가 생성한 표준 Java 템플릿이다. BillingPlugin 을 Kotlin 으로 작성하려면 Kotlin Gradle 플러그인 + Kotlin stdlib + Google Play Billing Library 의존성을 모두 추가해야 한다.

Capacitor 7 의 inline 플러그인은 별도 npm 패키지로 빼지 않고 `mobile/android/app/src/main/java/com/sqldpass/app/` 안에 직접 두는 방식이 가장 단순하다 (`docs/ANDROID_LAUNCH.md` Step 1 옵션 A 권장). 이 step 은 그 인프라(Gradle 빌드 설정)만 깐다. 실제 Kotlin 코드 추가는 Step 2.

## 작업 디렉터리

```
mobile/android/
```

## 변경 대상

- `mobile/android/variables.gradle` (버전 상수 2개 추가)
- `mobile/android/build.gradle` (kotlin-gradle-plugin classpath 1줄 추가)
- `mobile/android/app/build.gradle` (kotlin-android plugin + 2개 의존성)

## 변경 내용

### 1) `mobile/android/variables.gradle` — 버전 상수 추가

기존 `ext { ... }` 블록 끝(`cordovaAndroidVersion = '10.1.1'` 다음)에 두 줄 추가:

```groovy
ext {
    // ... 기존 줄들 ...
    cordovaAndroidVersion = '10.1.1'
    kotlinVersion = '1.9.25'
    playBillingVersion = '6.2.1'
}
```

**버전 선택 근거**:
- `kotlinVersion = '1.9.25'`: AGP 8.7.2 와 호환되는 안정판. 2.0+ 도 가능하지만 K2 컴파일러 마이그레이션 부담 회피.
- `playBillingVersion = '6.2.1'`: `docs/ANDROID_LAUNCH.md` Step 1 의 "Play Billing Library v6 직접 호출" 가이드와 정합. v6.2 에서 `enablePendingPurchases(PendingPurchasesParams)` 도입되어 v6.2.1 사용.

### 2) `mobile/android/build.gradle` — kotlin-gradle-plugin classpath 추가

`buildscript { dependencies { ... } }` 블록 안에 1줄 추가:

```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.7.2'
        classpath 'com.google.gms:google-services:4.4.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    }
}
```

주의: `$kotlinVersion` 보간이 동작하려면 `apply from: "variables.gradle"` 이 buildscript 평가 전에 로드되어야 하는데, Gradle 의 buildscript 블록은 외부 스크립트 평가보다 먼저 실행되므로 변수 해석이 깨질 수 있다. **만약 보간 실패 시 리터럴 `'1.9.25'` 로 변경**.

### 3) `mobile/android/app/build.gradle` — kotlin-android plugin + 2개 의존성

상단에 `apply plugin` 1줄 추가:

```groovy
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'
```

`dependencies { ... }` 블록 안에 2줄 추가 (기존 줄들 다음, `implementation project(':capacitor-cordova-android-plugins')` 다음):

```groovy
dependencies {
    // 기존 줄들 그대로 유지
    implementation project(':capacitor-cordova-android-plugins')
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    implementation "com.android.billingclient:billing-ktx:$playBillingVersion"
}
```

## 검증

```powershell
cd mobile/android
.\gradlew.bat tasks --quiet
.\gradlew.bat assembleDebug
```

성공 조건:
- `gradlew tasks` 가 Gradle Sync 에러 없이 통과 — Kotlin 플러그인 + Billing Library 가 정상 해석됨.
- `gradlew assembleDebug` 가 통과 — 빌드 산출물 `mobile/android/app/build/outputs/apk/debug/app-debug.apk` 생성.
- 산출물은 `.gitignore` 의 `build/` 룰로 제외됨.

**환경 의존**: 사용자 환경에 Android Studio + SDK Platform 34 가 설치돼 있어야 검증 가능 (`docs/ANDROID_LAUNCH.md` Step 2-2). 미설치 시 빌드 검증은 Step 2 와 함께 사용자가 환경 셋업 후 일괄 진행.

## Acceptance Criteria

1. `mobile/android/variables.gradle` 에 `kotlinVersion`, `playBillingVersion` 두 상수 추가됨.
2. `mobile/android/build.gradle` buildscript dependencies 에 kotlin-gradle-plugin classpath 추가됨.
3. `mobile/android/app/build.gradle` 상단에 `apply plugin: 'kotlin-android'` 추가됨.
4. `mobile/android/app/build.gradle` dependencies 에 `kotlin-stdlib` + `billing-ktx` 추가됨.
5. (환경 가능 시) `.\gradlew.bat assembleDebug` 통과.

## 금지 사항

- Kotlin 2.x 로 올리지 마라. **이유**: AGP 8.7.2 환경에서 안정 검증 불충분. K2 컴파일러 이슈 대비 1.9 LTS 라인 유지.
- Billing Library 7.x 로 올리지 마라. **이유**: `docs/ANDROID_LAUNCH.md` 가 v6 기준이고, backend `PlayBillingClient` 도 v6 가정 (Google Play Developer API 호환). v7 도입 시 backend 동시 검증 phase 별도.
- `apply plugin: 'org.jetbrains.kotlin.android'` 같은 FQN 으로 쓰지 마라. **이유**: 기존 `apply plugin: 'com.android.application'` 패턴과 통일성 유지 (둘 다 짧은 형식). 그루비 build.gradle 컨벤션.
- KSP, kotlin-android-extensions 같은 추가 플러그인 끼우지 마라. **이유**: BillingPlugin 한 파일에 annotation processing 없음. 의존성을 최소로 유지.
- `kotlin-stdlib-jdk8` 또는 `kotlin-stdlib-jdk7` 변종 쓰지 마라. **이유**: Kotlin 1.9 부터 `kotlin-stdlib` 단일 패키지로 통합. jdk7/jdk8 변종은 deprecated.

## 커밋

```powershell
git add mobile/android/variables.gradle mobile/android/build.gradle mobile/android/app/build.gradle
git commit -m "feat(android-billing): Kotlin 플러그인 + Google Play Billing Library v6 의존성 추가

- mobile/android/variables.gradle 에 kotlinVersion(1.9.25) + playBillingVersion(6.2.1) 상수 추가
- mobile/android/build.gradle buildscript 에 kotlin-gradle-plugin classpath 추가
- mobile/android/app/build.gradle 에 kotlin-android plugin + kotlin-stdlib + billing-ktx 의존성 추가

BillingPlugin.kt(Step 2) 작성에 필요한 빌드 인프라. AGP 8.7.2 / Capacitor 7 호환.

Refs: phases/android-billing-plugin/step1.md, docs/ANDROID_LAUNCH.md Step 1 옵션 A"
```

## Status 규칙

- 성공: step 1 status `completed`, summary "kotlinVersion 1.9.25 + playBillingVersion 6.2.1, kotlin-android plugin + billing-ktx 의존성 추가".
- 실패: 3회 재시도 후에도 Gradle Sync 가 깨지면 `error` + `error_message`. 흔한 원인: kotlinVersion 변수 보간 실패(리터럴로 변경), AGP-Kotlin 호환 불일치.
- blocked: Android SDK 미설치로 Gradle 검증 불가하면 `blocked` + `blocked_reason: "docs/ANDROID_LAUNCH.md Step 2-2 (Android Studio 설치) 필요"`.
