# Step 1 — cmdline-tools 다운 + SDK 패키지 + AVD 셋업

## 배경

`docs/ANDROID_LAUNCH.md` Step 2 는 Android Studio GUI 설치를 기본 가이드로 두지만, 사용자 환경(`~/AppData/Local/Android/Sdk` 거의 비어 있음, `ANDROID_HOME` 미설정)에서 IDE 없이 에뮬레이터만 띄우는 게 목적이므로 Google cmdline-tools 패키지를 받아 sdkmanager + avdmanager 로 자동 셋업한다. 결과물(SDK + AVD) 은 영구 보존되어 이후 `emulator -avd sqldpass_test` 한 줄로 재실행 가능, 미래에 Android Studio 추가 설치 시 같은 `ANDROID_HOME` 인식.

이 step 은 **PowerShell 명령만 실행**, git 에 들어가는 파일 변경은 없다 (이 step 자체 commit 은 step status 갱신 1줄).

## 작업 디렉터리

PowerShell 어디서나 가능. 결과는 `$env:USERPROFILE\AppData\Local\Android\Sdk`.

## 사용자 사전 확인 (Claude 자동화 불가)

1. **WHPX 활성화** — admin PowerShell 에서:
   ```powershell
   Get-WindowsOptionalFeature -Online -FeatureName "HypervisorPlatform"
   # State 가 "Enabled" 아니면:
   Enable-WindowsOptionalFeature -Online -FeatureName "HypervisorPlatform" -All
   # → 재부팅 필요
   ```
2. **디스크 공간 ~5GB 여유** 확인.

## 실행 명령 (PowerShell)

### 1-A. cmdline-tools 다운로드 + 정규화

```powershell
$sdk = "$env:USERPROFILE\AppData\Local\Android\Sdk"
$tmp = "$env:TEMP\cmdline-tools.zip"
$url = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"

New-Item -ItemType Directory -Force -Path "$sdk\cmdline-tools" | Out-Null
Invoke-WebRequest -Uri $url -OutFile $tmp
Expand-Archive -Path $tmp -DestinationPath "$sdk\cmdline-tools" -Force

# zip 안의 폴더명이 "cmdline-tools" 라 cmdline-tools/cmdline-tools/ 로 풀림 → cmdline-tools/latest/ 로 이동
if (Test-Path "$sdk\cmdline-tools\cmdline-tools") {
    if (Test-Path "$sdk\cmdline-tools\latest") { Remove-Item -Recurse -Force "$sdk\cmdline-tools\latest" }
    Move-Item "$sdk\cmdline-tools\cmdline-tools" "$sdk\cmdline-tools\latest"
}
Remove-Item $tmp -ErrorAction SilentlyContinue
```

### 1-B. 환경변수 영구 설정 (User 스코프)

```powershell
$sdk = "$env:USERPROFILE\AppData\Local\Android\Sdk"

# 현재 세션 즉시 적용
$env:ANDROID_HOME = $sdk
$env:Path = "$sdk\cmdline-tools\latest\bin;$sdk\platform-tools;$sdk\emulator;$env:Path"

# 영구 (User 환경변수)
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdk, "User")
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$addPaths = "$sdk\cmdline-tools\latest\bin;$sdk\platform-tools;$sdk\emulator"
if (-not $userPath -or -not $userPath.Contains("$sdk\cmdline-tools")) {
    [Environment]::SetEnvironmentVariable("Path", "$addPaths;$userPath", "User")
}
```

### 1-C. 라이선스 수락 + SDK 패키지 설치 (~15~25분, ~4GB)

```powershell
# 라이선스 일괄 수락
"y`ny`ny`ny`ny`ny`ny`n" | & "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --licenses

# 필수 패키지
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator" "system-images;android-34;google_apis_playstore;x86_64"
```

`google_apis_playstore` 시스템 이미지를 쓰는 이유: 실제 Play Store 앱이 포함되어 BillingClient 가 정상 통신 시도 (없는 이미지는 결제 자체 불가).

### 1-D. AVD 생성

```powershell
$avdInput = "no"  # custom hardware profile 묻는 입력에 no
$avdInput | & "$env:ANDROID_HOME\cmdline-tools\latest\bin\avdmanager.bat" create avd `
  -n sqldpass_test `
  -k "system-images;android-34;google_apis_playstore;x86_64" `
  -d pixel_7 `
  --force
```

생성 위치: `~/.android/avd/sqldpass_test.avd/`.

## 검증

```powershell
# 새 PowerShell 창 열어서 (환경변수 반영 확인)
$env:ANDROID_HOME      # → C:\Users\<user>\AppData\Local\Android\Sdk
sdkmanager --version   # 버전 출력
sdkmanager --list_installed   # 위 5개 패키지 보임
avdmanager list avd    # Name: sqldpass_test 보임
```

## Acceptance Criteria

1. `$env:USERPROFILE\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat` 존재.
2. User 환경변수 `ANDROID_HOME` 설정됨 + `Path` 에 cmdline-tools, platform-tools, emulator 3개 경로 추가.
3. `sdkmanager --list_installed` 출력에 `platform-tools`, `platforms;android-34`, `build-tools;34.0.0`, `emulator`, `system-images;android-34;google_apis_playstore;x86_64` 5개 모두 포함.
4. `avdmanager list avd` 에 `sqldpass_test` 보임.
5. 변경 파일 없음 (환경 셋업이라 git 영향 X). step status JSON 1줄만 갱신.

## 금지 사항

- `system-images;android-34;google_apis;x86_64` (Play Store 없는 이미지) 를 쓰지 마라. **이유**: Play Billing 동작에 Play Store 앱 필수. 없으면 `BILLING_UNAVAILABLE` 만 나와 검증 의미가 약함.
- ARM 시스템 이미지(`armeabi-v7a`, `arm64-v8a`)를 쓰지 마라. **이유**: x86_64 호스트(Windows) 에서 WHPX 가속화는 x86_64 이미지에만 적용. ARM 은 software 에뮬레이션이라 매우 느림.
- API 34 외 버전을 쓰지 마라. **이유**: Play Store 정책 target API 34 이상 강제. 동일 SDK 로 build/test 일관성.
- 비번 들어가는 sdkmanager 명령에 `--no-https` 같은 옵션 끼우지 마라. **이유**: 패키지 무결성. HTTPS 강제.
- machine 스코프 환경변수(`SetEnvironmentVariable(..., "Machine")`) 로 설정하지 마라. **이유**: admin 권한 필요 + 다른 사용자 영향. User 스코프로 충분.

## Status 규칙

- 성공: step 1 status `completed`, summary "cmdline-tools + SDK 5개 패키지 + sqldpass_test AVD 셋업, 환경변수 User 영구 설정".
- 실패: 다운로드/sdkmanager/avdmanager 중 어느 하나라도 3회 재시도 후 실패 시 `error` + `error_message`.
- blocked: WHPX 미활성으로 향후 Step 3 의 에뮬레이터 부팅이 불가능할 게 예상되면 step 1 자체는 통과시키되 phase summary 에 명시.
