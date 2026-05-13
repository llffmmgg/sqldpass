# Step 1 — mobile/ 워크스페이스 부팅 + Android 네이티브 프로젝트 생성

## 배경

`mobile/` 디렉토리는 `package.json`, `capacitor.config.ts`, `web/index.html` 같은 설정 파일만 존재하고 `node_modules/`도 `android/` 네이티브 프로젝트도 없는 비어 있는 상태다. `docs/ANDROID_LAUNCH.md` Step 5-3 의 `npx cap sync android` 를 실행하려면 먼저 의존성 설치와 안드로이드 네이티브 프로젝트 트리 생성이 선행되어야 한다.

`docs/ARCHITECTURE.md`·`docs/ADR.md ADR-007` 에 따라 이 앱은 외부 URL 모드(Capacitor 7 + `server.url = https://www.sqldpass.com`)로 동작하므로, `cap add android` 가 생성하는 표준 안드로이드 네이티브 트리에 별도 화면 코드를 더하지 않고 그대로 사용한다.

## 작업 디렉터리

```
mobile/
```

## 변경 대상

| 파일/디렉토리 | 변경 | 비고 |
|---|---|---|
| `mobile/package-lock.json` | 갱신 또는 생성 | `npm install` 산출물 |
| `mobile/android/` 트리 전체 | 새로 생성 (수십~수백 파일) | `npx cap add android` 산출물. 단 `.gitignore` 로 제외되는 항목 제외 |
| `mobile/.gitignore` | 필요 시 보강 | Capacitor 7 산출물 누락 항목 확인 |

## 실행 명령

```powershell
cd mobile
npm install
npx cap add android
npx cap sync android
```

`npm install` 은 ~30초 ~2분 소요. `cap add android` 는 안드로이드 네이티브 프로젝트 템플릿을 ~1분 안에 생성하며, 콘솔에 `[success] android platform added!` 출력. `cap sync android` 는 `mobile/capacitor.config.ts` 와 의존성 플러그인을 안드로이드 프로젝트에 반영.

## 검증

```powershell
# 안드로이드 프로젝트 트리 존재 확인
Test-Path mobile/android/app/build.gradle           # → True (또는 build.gradle.kts)
Test-Path mobile/android/app/src/main/AndroidManifest.xml  # → True

# capacitor.config.ts 의 appId 가 AndroidManifest 에 반영됐는지
Select-String -Path mobile/android/app/src/main/AndroidManifest.xml -Pattern 'com.sqldpass.app'
# 또는 build.gradle 의 applicationId 확인
Select-String -Path mobile/android/app/build.gradle -Pattern 'applicationId'
```

성공 조건:
- `mobile/android/app/build.gradle` (또는 `.kts`) 존재
- `mobile/android/app/src/main/AndroidManifest.xml` 의 `package=` 또는 `build.gradle` 의 `applicationId` 가 `com.sqldpass.app`
- `mobile/android/app/src/main/res/values/strings.xml` 의 `app_name` 이 `문어CBT` (`capacitor.config.ts` 의 `appName` 반영)
- `git status` 에서 `mobile/android/` 의 새 파일들이 untracked 로 보이되, `mobile/android/build/`, `mobile/android/app/build/`, `mobile/android/.gradle/`, `mobile/android/local.properties`, `mobile/android/app/src/main/assets/public/`, `mobile/android/app/src/main/assets/capacitor.*.json` 등은 ignore 되어 표시되지 않음

## Acceptance Criteria

1. `mobile/node_modules/` 생성 (gitignore 처리됨).
2. `mobile/android/` 네이티브 프로젝트 트리 생성.
3. `AndroidManifest.xml` 또는 `build.gradle` 에 `com.sqldpass.app` 식별자 반영.
4. `strings.xml` 의 `app_name` 이 `문어CBT`.
5. `mobile/.gitignore` 에 빌드 산출물·키스토어·`google-services.json`·Capacitor sync 임시 파일이 모두 제외됨 (현행 항목으로 충분한지 점검).
6. `git status` 확인 시 빌드 산출물·`local.properties`·`*.keystore` 등이 untracked 로 보이지 않음.

## 금지 사항

- `mobile/AGENTS.md` 를 새로 만들지 마라. **이유**: 직전 phase `fix-android-minor/step1` 에서 "외부 URL 모드라 mobile/ 은 화면 코드를 두지 않고 Capacitor 설정·빌드 산출물만 가짐. 별도 가이드 파일이 필요할 만큼 규칙이 없음" 으로 결정. 안내가 더 필요하면 `docs/ANDROID_LAUNCH.md` 만 갱신.
- `mobile/android/local.properties` 를 commit 하지 마라. **이유**: 사용자별 Android SDK 경로가 들어가 다른 머신에서 빌드 깨짐.
- `mobile/android/app/google-services.json` 같은 외부 서비스 키 파일을 commit 하지 마라. **이유**: `.gitignore` 에 이미 명시. 실제 파일은 Firebase 콘솔에서 다운로드 후 로컬에만 배치.
- `mobile/capacitor.config.ts` 의 `appId`, `appName`, `server.url`, `androidScheme`, `allowNavigation`, `webContentsDebuggingEnabled` 값을 변경하지 마라. **이유**: 이 step 의 범위는 부팅이지 설정 변경이 아니다.
- `@capacitor-firebase/authentication`, Play Billing 플러그인, FCM 푸시 플러그인 등 신규 npm 의존성을 설치하지 마라. **이유**: `docs/ANDROID_LAUNCH.md` Step 1 의 옵션 선택이 선행되어야 하고, 결정 자체가 0.5~2일 작업. 별도 phase 영역.
- `npx capacitor-assets generate --android` 를 실행하지 마라. **이유**: `mobile/assets/icon.png`, `mobile/assets/splash.png` 원본 이미지가 없다. 디자인 자산 준비는 별도 작업.
- 빌드(`gradlew assembleDebug/Release`)를 실행하지 마라. **이유**: 빌드 산출물이 untracked 로 잔뜩 떠서 step 의 결과물 식별이 어려워진다. 빌드 검증은 Step 2 에서.

## 커밋

```powershell
# 모든 새 파일을 staging (단, ignore 항목은 자동 제외)
git add mobile/

# 산출물 한 commit
git commit -m "feat(android): mobile/ workspace 부팅 + cap add android 로 안드로이드 네이티브 프로젝트 트리 생성

- npm install 로 Capacitor 7 + @capacitor/android, @capacitor/app, @capacitor/browser, @capacitor/push-notifications 설치
- npx cap add android 로 mobile/android/ 네이티브 프로젝트 트리 생성 (applicationId=com.sqldpass.app, app_name=문어CBT)
- npx cap sync android 로 capacitor.config.ts 반영

Refs: phases/android-launch-prep/step1.md, docs/ANDROID_LAUNCH.md Step 2~5"

git push origin feat-android-launch-prep
```

## Status 규칙

- 성공: `phases/android-launch-prep/index.json` 의 step 1 status 를 `completed` 로, summary 에 "mobile/ npm install + cap add android + sync 완료, 안드로이드 네이티브 트리 생성됨" 한 줄 기록.
- 실패: 3회 시도 후에도 `cap add android` 가 깨지면 `error` + `error_message` 기록. 흔한 원인: Node 버전 부적합(<18), 또는 `@capacitor/cli` 버전 미스매치.
- blocked: JDK/Android SDK 부재로 `cap sync` 가 실패하면 `blocked` + `blocked_reason: "docs/ANDROID_LAUNCH.md Step 2-1, 2-2 의 JDK 17 / Android Studio 설치가 사용자 환경에 필요"` 기록.
