# Step 3 — 에뮬레이터 실행 + APK 빌드/설치 + Layer 1 검증

## 배경

Step 1 에서 SDK + AVD 준비, Step 2 에서 debug WebView 디버깅 허용. 이제 emulator 띄우고 BillingPlugin 의 native 호출 응답까지 확인한다. Layer 1 검증의 핵심: **Chrome `chrome://inspect` 에서 emulator WebView 의 DevTools 에 접근해 `Capacitor.Plugins.Billing.purchase(...)` 를 직접 호출**해 응답을 본다. sqldpass 사이트 로그인 흐름(Google OAuth → 네이티브 ID 토큰)은 Auth 플러그인 별도 phase 영역이므로 우회.

## 작업 디렉터리

```
mobile/android/   (Gradle 빌드용)
PowerShell 임의 위치 (emulator/adb/chrome)
```

## 변경 대상

코드 변경 없음. 모든 게 런타임 실행 + 검증 결과 정리.

## 실행 명령

### 3-A. 에뮬레이터 실행

```powershell
# background 로 GUI 창 띄움
Start-Process -FilePath "$env:ANDROID_HOME\emulator\emulator.exe" -ArgumentList @("-avd", "sqldpass_test")
```

첫 cold boot 는 2~5분. 이후는 snapshot 으로 ~30초.

```powershell
# 부팅 완료까지 대기
adb wait-for-device
adb shell getprop sys.boot_completed   # "1" 이 나올 때까지

# 또는 한 줄로
while ((adb shell getprop sys.boot_completed 2>$null) -ne "1") { Start-Sleep -Seconds 5 }
adb devices   # emulator-5554  device 표시 확인
```

### 3-B. debug APK 빌드

```powershell
cd mobile/android
.\gradlew.bat assembleDebug
```

첫 빌드는 Gradle/Capacitor/AndroidX/Kotlin/Billing Library 의존성 다운로드로 5~10분. 이후 ~30초.

산출: `mobile/android/app/build/outputs/apk/debug/app-debug.apk`.

### 3-C. APK 설치

```powershell
adb install -r mobile/android/app/build/outputs/apk/debug/app-debug.apk
# 또는 절대 경로
adb install -r "$PWD\app\build\outputs\apk\debug\app-debug.apk"
```

검증: `adb shell pm list packages | findstr sqldpass` → `package:com.sqldpass.app`.

### 3-D. 앱 실행 + logcat 모니터링

```powershell
# 별도 PowerShell 창 (logcat 백그라운드 스트림)
adb logcat -s Capacitor:* CapacitorPlugin:* BillingClient:* AndroidRuntime:E

# 본 창에서 앱 실행
adb shell am start -n com.sqldpass.app/.MainActivity
```

emulator GUI 에서 "문어CBT" WebView 가 sqldpass.com 을 로드하는 것 확인.

### 3-E. Chrome DevTools 로 WebView 접근 (Layer 1 핵심)

1. **호스트 PC 에서 Chrome 브라우저** 열기 → 주소창에 `chrome://inspect` 입력.
2. "Remote Target" 섹션 아래 `emulator-5554` 의 `com.sqldpass.app` WebView 가 보이는지 확인.
3. 그 항목 옆 "inspect" 클릭 → DevTools 창이 별도 창으로 뜸.
4. DevTools 의 Console 탭으로 이동.

### 3-F. BillingPlugin 직접 호출

DevTools Console 에서:

```javascript
// 1. plugin 등록 확인
console.log("Plugins:", Object.keys(window.Capacitor?.Plugins ?? {}));
// 기대값: 배열 안에 "Billing", "App", "Browser", "PushNotifications" 등 포함

// 2. native purchase 호출
const r1 = await window.Capacitor.Plugins.Billing.purchase({ productId: "iap_one_month" });
console.log("purchase iap_one_month:", r1);
// 기대값: {success: false, errorCode: "BILLING_UNAVAILABLE" | "ITEM_UNAVAILABLE", errorMessage: "..."}
// (Play Console SKU 미등록 상태이므로 거절이 정상)

// 3. productId 누락
const r2 = await window.Capacitor.Plugins.Billing.purchase({}).catch(e => ({ rejected: true, error: e.message }));
console.log("purchase empty:", r2);
// 기대값: rejected 또는 errorCode INVALID_INPUT
```

### 3-G. logcat 확인

호출 후 logcat 스트림에서 다음 키워드가 보이면 OK:
- `Capacitor` 태그: `Billing` plugin 호출 진입 로그
- `BillingClient` 태그: `Service connected` / `queryProductDetails` / 거절 응답
- 에러 없음 (특히 `AndroidRuntime: FATAL EXCEPTION` 없음 = native 코드 정상)

## Acceptance Criteria

| # | 확인 항목 | 기대 |
|---|---|---|
| 1 | `adb devices` | `emulator-5554  device` |
| 2 | `pm list packages` | `package:com.sqldpass.app` 포함 |
| 3 | emulator 화면 | WebView 로 sqldpass.com 또는 폴백 페이지 로드 |
| 4 | `chrome://inspect` | `com.sqldpass.app` WebView 의 "inspect" 링크 활성 |
| 5 | DevTools console `Object.keys(window.Capacitor.Plugins)` | `"Billing"` 포함 |
| 6 | `Billing.purchase({productId:"iap_one_month"})` | `{success:false, errorCode:"BILLING_UNAVAILABLE"\|"ITEM_UNAVAILABLE", ...}` |
| 7 | logcat | `Capacitor`/`BillingClient` 진입 로그, FATAL EXCEPTION 없음 |

이 7개가 모두 통과하면 BillingPlugin 의 **컴파일 + 등록 + native 호출 + 응답 직렬화** 가 검증된 것 = Layer 1 완료.

## 금지 사항

- emulator 띄운 채로 `adb -s emulator-5554 reboot` 같은 강제 재시작 명령 쓰지 마라. **이유**: cold boot 다시 5분 + snapshot 손상 가능. emulator GUI 의 close 버튼 또는 `adb emu kill` 정공.
- `webContentsDebuggingEnabled` 를 true 인 채로 release AAB 빌드하지 마라. **이유**: Step 2 의 NODE_ENV 분기를 우회. release 빌드 시 반드시 `NODE_ENV=production`.
- 결제 카드를 사이트 UI 에서 직접 클릭하려고 하지 마라. **이유**: sqldpass Google 로그인 → 네이티브 ID 토큰 흐름이 이 phase 범위 밖. console 직접 호출이 정공.
- DevTools console 에서 `localStorage.setItem("user_token", "...")` 로 mock 토큰 박지 마라. **이유**: backend 가 토큰 검증하므로 mock 은 동작 안 함. 또한 본 검증 시나리오는 BillingPlugin native 응답까지라 로그인 상태와 무관.
- `mobile/android/app/build/` 산출물을 git 에 commit 하지 마라. **이유**: `.gitignore` 처리됨, 자동 제외.

## 커밋

이 step 자체는 코드 변경이 없으므로 step status JSON 갱신 1줄만 commit:

```powershell
git add phases/android-emulator-setup/index.json phases/index.json
git commit -m "chore(android-emulator-setup): step 3 + phase completed

Layer 1 검증 결과:
- 에뮬레이터 부팅 OK (adb devices: emulator-5554)
- debug APK 빌드/설치 OK (com.sqldpass.app)
- chrome://inspect 로 WebView DevTools 접근 OK
- Capacitor.Plugins.Billing 등록 확인 OK
- Billing.purchase native 호출 응답: <BILLING_UNAVAILABLE | ITEM_UNAVAILABLE>
  (SKU 미등록이라 거절이 정상)

Layer 2 (실제 결제창 + 토큰 + backend verify e2e) 는 별도 phase.
"
```

## Status 규칙

- 성공: step 3 status `completed`, summary 에 위 7개 acceptance 결과 요약.
- 실패: 3회 재시도 후에도 임의 단계 실패 시 `error` + `error_message`. 흔한 원인: WHPX 미활성 (emulator 안 뜸), Gradle SDK 경로 미인식, BillingPlugin 컴파일 에러.
- blocked: `chrome://inspect` 에 WebView 가 안 보이면 Step 2 의 `webContentsDebuggingEnabled` 가 false 로 빌드된 것 — Step 2 재확인 + cap sync 재실행 + 재빌드 + 재설치 후 시도. 그래도 안 보이면 `blocked` + `blocked_reason`.
