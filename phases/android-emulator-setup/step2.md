# Step 2 — capacitor.config.ts WebView 디버깅 분기

## 배경

`mobile/capacitor.config.ts:16` 의 `webContentsDebuggingEnabled: false` 는 release 빌드에서는 옳지만 debug 빌드에서도 false 라 Chrome `chrome://inspect` 으로 emulator 안 WebView 의 DevTools 에 접근할 수 없다. BillingPlugin Layer 1 검증(Step 3)은 console 에서 직접 `Capacitor.Plugins.Billing.purchase(...)` 를 호출해 plugin 등록·native 응답을 확인하는 흐름이므로, **debug 빌드에서만 true** 가 되도록 분기를 추가해야 한다.

가장 가벼운 방법: `process.env.NODE_ENV !== "production"` 으로 분기. Capacitor CLI 가 `cap sync` 실행 시점에 그루비 build.gradle 의 `webViewDebugging` 같은 native 설정이 아니라 `mobile/android/app/src/main/assets/capacitor.config.json` 으로 직렬화하므로 빌드 시점의 NODE_ENV 가 그대로 반영된다.

## 작업 디렉터리

```
mobile/
```

## 변경 대상

- `mobile/capacitor.config.ts` (1줄 수정)

## 변경 내용

기존 (Line 16):
```typescript
android: {
  allowMixedContent: false,
  backgroundColor: "#0a0a0a",
  webContentsDebuggingEnabled: false,
},
```

수정 후:
```typescript
android: {
  allowMixedContent: false,
  backgroundColor: "#0a0a0a",
  // debug 빌드(NODE_ENV !== "production") 에서만 Chrome chrome://inspect 접근 허용.
  // release 빌드는 false 로 유지 — WebView remote debugging 노출 시 인앱 코드/세션 토큰 누출 위험.
  webContentsDebuggingEnabled: process.env.NODE_ENV !== "production",
},
```

이후 `cd mobile && npx cap sync android` 로 변경을 native 프로젝트에 반영. `mobile/android/app/src/main/assets/capacitor.config.json` 이 갱신되지만 이 파일은 이미 `.gitignore` 처리됨 (확인: `mobile/.gitignore:24`).

## 검증

```powershell
# debug 모드 sync
cd mobile
npx cap sync android

# 결과 확인 — 자동 생성된 capacitor.config.json 에 webContentsDebuggingEnabled: true 가 들어있어야 함
Get-Content android/app/src/main/assets/capacitor.config.json | Select-String "webContentsDebuggingEnabled"
# → "webContentsDebuggingEnabled": true (debug 빌드 환경)
```

`npx cap sync` 의 기본 NODE_ENV 는 미정의(`undefined !== "production"` → true). 명시적으로 production 빌드 시에는 `NODE_ENV=production npx cap sync android` 또는 release 빌드 파이프라인에서 자동 처리.

## Acceptance Criteria

1. `mobile/capacitor.config.ts` 의 `webContentsDebuggingEnabled` 가 `process.env.NODE_ENV !== "production"` 표현식으로 변경됨.
2. 변경 의도를 설명하는 주석 추가 (release 빌드는 false 유지 명시).
3. `npx cap sync android` 후 `mobile/android/app/src/main/assets/capacitor.config.json` 에 `"webContentsDebuggingEnabled": true` 가 들어있음 (NODE_ENV unset 상태 = debug 가정).
4. release 빌드 시 NODE_ENV=production 으로 sync 하면 false 로 유지되는지 (수동 확인 또는 별도 follow-up).

## 금지 사항

- `webContentsDebuggingEnabled: true` 로 무조건 하드코딩하지 마라. **이유**: release APK 가 그대로 Play Store 에 올라가면 누구나 sqldpass 앱의 WebView console 에 붙어 인앱 세션·localStorage 토큰을 추출 가능. 보안 사고 직결.
- `allowMixedContent`, `androidScheme`, `allowNavigation` 같은 다른 capacitor.config.ts 값을 같이 손대지 마라. **이유**: 이 step 의 범위는 디버깅 1줄. 다른 값은 별도 변경 phase.
- `mobile/android/app/src/main/assets/capacitor.config.json` 을 직접 수정하지 마라. **이유**: 그 파일은 `cap sync` 가 자동 생성, gitignore 처리. 수동 수정은 다음 sync 에서 덮어쓰임.
- release/debug 분기를 `mobile/android/app/build.gradle` 의 `buildTypes.debug { debuggable true; ... }` 같은 native 쪽에 두지 마라. **이유**: Capacitor 의 `webContentsDebuggingEnabled` 는 WebView 의 `setWebContentsDebuggingEnabled()` 를 호출하는 곳이고, Capacitor 가 그 호출을 capacitor.config.json 값으로 게이팅한다. native build flag 와는 무관.

## 커밋

```powershell
git add mobile/capacitor.config.ts
git commit -m "feat(mobile): debug 빌드에서만 WebView 디버깅 허용 (chrome://inspect)

mobile/capacitor.config.ts 의 webContentsDebuggingEnabled 를
process.env.NODE_ENV !== 'production' 분기로 변경.

- debug 빌드(NODE_ENV unset 또는 development): true
  → Chrome chrome://inspect 으로 emulator WebView console 접근 가능
  → BillingPlugin 등 native plugin 직접 호출 검증 가능
- release 빌드(NODE_ENV=production): false
  → Play Store 배포 빌드에서 WebView 노출 방지 (보안)

Refs: phases/android-emulator-setup/step2.md"
```

## Status 규칙

- 성공: step 2 status `completed`, summary "capacitor.config.ts webContentsDebuggingEnabled 를 NODE_ENV 분기로 변경, cap sync android 반영".
- 실패: TypeScript 컴파일 또는 cap sync 실패 시 `error` + `error_message`.
