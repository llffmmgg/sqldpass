# iOS Release — 최초 Secret 셋업 가이드

`ios-release.yml` workflow 와 `scripts/archive_app_store.sh` 가 동작하려면 GitHub Secrets 9개가 등록되어 있어야 한다. 본 문서는 그 secret 들을 어떻게 생성·변환·등록하는지 단계별로 안내한다.

> ⚠️ **새 Mac 또는 새 Apple ID 로 시작하는 1회성 작업.** 한 번 등록하면 인증서 / 프로파일 만료(보통 1년) 전까지 재실행 불필요.

## 필요 Secret 9개 요약

| Secret | 용도 | 작업 환경 |
|---|---|---|
| `APPLE_TEAM_ID` | Apple Developer 팀 ID (10자) | Windows 가능 |
| `IOS_CERTIFICATE_P12_BASE64` | iOS Distribution 인증서 .p12 | **Mac 필요** |
| `IOS_CERTIFICATE_PASSWORD` | 위 .p12 의 export 비밀번호 | **Mac 필요** |
| `IOS_PROVISIONING_PROFILE_BASE64` | App Store Distribution provisioning profile | Mac 권장 |
| `IOS_PROVISIONING_PROFILE_NAME` | profile 이름 | 본인 결정 |
| `IOS_KEYCHAIN_PASSWORD` (선택) | 임시 keychain 비밀번호 | 본인 결정 |
| `APP_STORE_CONNECT_KEY_ID` | App Store Connect API Key ID | Windows 가능 |
| `APP_STORE_CONNECT_ISSUER_ID` | API Issuer ID | Windows 가능 |
| `APP_STORE_CONNECT_API_KEY_BASE64` | .p8 키 파일 | Windows 가능 |

→ Mac 필요한 단계는 **STEP 2 + STEP 3**. 나머지는 Windows 브라우저만으로 가능.

---

## STEP 1 — APPLE_TEAM_ID (Windows 가능)

Apple Developer 팀 ID — 10자 영숫자.

### 확인 위치

- https://developer.apple.com/account → 좌측 사이드바 **Membership Details**
- 또는 https://developer.apple.com/account → 상단 우측 본인 이름 옆에 표시

### 값 (sqldpass)

```
APPLE_TEAM_ID = Y9439S9RWP
```

### 등록

```powershell
gh secret set APPLE_TEAM_ID --repo llffmmgg/sqldpass --body "Y9439S9RWP"
```

또는 https://github.com/llffmmgg/sqldpass/settings/secrets/actions → "New repository secret"

> ✅ 이미 등록됨 (2026-05-19).

---

## STEP 2 — iOS Distribution 인증서 (.p12) (Mac 필수)

### 2-1. Mac 에서 CSR 파일 생성

1. **Spotlight (⌘+Space)** → `Keychain Access` 검색 → 실행
2. 상단 메뉴바 → **Keychain Access** → **Certificate Assistant** → **"Request a Certificate From a Certificate Authority..."**
3. 입력:
   - **User Email Address**: 본인 Apple ID 이메일 (예: `heehun3658@gmail.com`)
   - **Common Name**: `Sqldpass Distribution`
   - **CA Email Address**: 비움
   - **Request is**: ⚫ **Saved to disk** (Continue 선택)
4. 데스크톱에 `CertificateSigningRequest.certSigningRequest` 저장됨

### 2-2. Apple Developer Portal 에서 인증서 발급

1. https://developer.apple.com/account/resources/certificates/list
2. 우측 상단 **+** 버튼
3. **Software** 섹션 → ⚫ **Apple Distribution** 선택 → Continue
4. **Choose File** → 위 2-1 의 `.certSigningRequest` 업로드 → Continue
5. 인증서 생성됨 → **Download** 클릭
6. `~/Downloads/distribution.cer` 같은 파일 받음

### 2-3. .p12 로 export

1. 다운받은 `distribution.cer` 더블클릭 → Keychain Access 의 "login" 키체인에 자동 등록
2. Keychain Access 좌측 사이드바 → **My Certificates** 카테고리 클릭 (⚠️ "Certificates" 카테고리 아님, private key 포함된 곳)
3. 방금 추가된 `Apple Distribution: HEEHUN JUNG (Y9439S9RWP)` 찾기
4. **우클릭 → Export...**
5. 파일 형식: ⚫ **Personal Information Exchange (.p12)** → 저장 (예: `~/Desktop/distribution.p12`)
6. **비밀번호 설정** — 메모 필수 (`IOS_CERTIFICATE_PASSWORD` 값)
7. 시스템 비밀번호 입력 (Mac 로그인 비밀번호)

### 2-4. base64 변환 (Mac Terminal)

```bash
base64 -i ~/Desktop/distribution.p12 | pbcopy
```

→ 클립보드에 매우 긴 base64 문자열 복사됨.

### 2-5. Secret 등록 (2개)

방법 A — 에이전트에게 전달:
```
"IOS_CERTIFICATE_P12_BASE64 등록해줘: <base64 전체>"
"IOS_CERTIFICATE_PASSWORD 등록해줘: <2-3 의 비밀번호>"
```

방법 B — 본인이 직접 GitHub UI:
- https://github.com/llffmmgg/sqldpass/settings/secrets/actions
- `IOS_CERTIFICATE_P12_BASE64` + base64 값
- `IOS_CERTIFICATE_PASSWORD` + 비밀번호

### 막힐 만한 곳
- **"Request a Certificate" 메뉴 안 보임**: macOS Sequoia 부터 Keychain Access 가 별도 앱. Spotlight 검색 또는 `/Applications/Utilities/Keychain Access.app`
- **".p12 옵션 회색"**: My Certificates 카테고리에서 우클릭해야 함. Certificates 카테고리에선 .cer 만 export 가능
- **private key 없음**: CSR 생성한 동일 Mac 에서 export 해야 함. 다른 Mac 의 .cer 를 가져오면 private key 없어 .p12 불가

---

## STEP 3 — App Store Distribution Provisioning Profile (Mac 권장)

> Apple Developer Portal 의 profile 발급은 Windows 브라우저로도 가능. 단 인증서 (STEP 2) 가 먼저 등록되어 있어야 발급 가능.

### 3-1. Apple Developer Portal 에서 profile 생성

1. https://developer.apple.com/account/resources/profiles/list
2. **+** 버튼 → **Distribution** 섹션의 ⚫ **App Store** 선택 → Continue
3. **App ID** 선택: `com.sqldpass.app` (이미 등록됨) → Continue
4. **Certificate** 선택: 방금 만든 `Apple Distribution: HEEHUN JUNG` → Continue
5. **Provisioning Profile Name** 입력: `sqldpass App Store` (이게 `IOS_PROVISIONING_PROFILE_NAME` 값)
6. **Generate** → **Download** → `sqldpass_App_Store.mobileprovision` 받음

### 3-2. .mobileprovision → base64

**Mac Terminal:**
```bash
base64 -i ~/Downloads/sqldpass_App_Store.mobileprovision | pbcopy
```

**Windows PowerShell** (Mac 없이 가능):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\Downloads\sqldpass_App_Store.mobileprovision")) | Set-Clipboard
```

### 3-3. Secret 등록 (2개)

```
"IOS_PROVISIONING_PROFILE_BASE64 등록해줘: <base64 전체>"
"IOS_PROVISIONING_PROFILE_NAME 등록해줘: sqldpass App Store"
```

또는 GitHub UI 직접 등록.

---

## STEP 4 — App Store Connect API Key (Windows 가능)

> 브라우저만 필요. Mac 없이 가능.

### 4-1. Apple Developer Portal 에서 API Key 발급

1. https://appstoreconnect.apple.com/access/api → **App Store Connect API** 탭 또는 **Integrations** 탭
2. 좌측 사이드바 **Keys** → **+** 버튼 (또는 "Generate API Key")
3. 입력:
   - **Name**: `sqldpass-testflight-upload`
   - **Access**: ⚫ **App Manager** (앱 빌드 업로드 권한)
4. **Generate** → 한 번만 표시되는 `.p8` 파일 **즉시 다운로드** (재발급 불가, 분실 시 새 키 발급해야)
   - 파일명: `AuthKey_XXXXXXXXXX.p8`
   - 표에서 **Key ID** (10자, 예: `ABC1234DEF`) 메모
5. 페이지 상단 **Issuer ID** (UUID 형식, 예: `12345678-1234-1234-1234-123456789012`) 메모

### 4-2. .p8 → base64

**Windows PowerShell:**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\Downloads\AuthKey_XXXXXXXXXX.p8")) | Set-Clipboard
```

**Mac Terminal:**
```bash
base64 -i ~/Downloads/AuthKey_XXXXXXXXXX.p8 | pbcopy
```

### 4-3. Secret 등록 (3개)

```
"APP_STORE_CONNECT_KEY_ID 등록해줘: <Key ID 10자>"
"APP_STORE_CONNECT_ISSUER_ID 등록해줘: <Issuer ID UUID>"
"APP_STORE_CONNECT_API_KEY_BASE64 등록해줘: <base64 전체>"
```

또는 GitHub UI.

### 막힐 만한 곳
- **App Store Connect API 탭 안 보임**: 본인 계정이 Apple Developer Program 회원이어야 함. 단순 Apple ID 만으로는 안 됨.
- **".p8 파일 사라짐"**: 한 번만 표시되고 재다운로드 불가. 분실 시 키 revoke 후 새로 발급
- **권한 부족**: API Key 발급은 **Account Holder** 권한 필요. App Manager 계정으로는 API Key 발급 불가 (Key 사용은 가능)

---

## STEP 5 — IOS_KEYCHAIN_PASSWORD (선택)

CI 의 임시 keychain 비밀번호. **미설정이어도 동작** (스크립트 기본값 `temporary-ios-build-keychain` 사용). 보안 강화 원하면 임의의 강한 문자열 등록:

```
"IOS_KEYCHAIN_PASSWORD 등록해줘: <임의 16자 이상>"
```

---

## STEP 6 — 검증

모든 secret 등록 후 GitHub Actions 의 **iOS Release** workflow 수동 실행:

1. https://github.com/llffmmgg/sqldpass/actions → 좌측 **iOS Release**
2. 우측 **Run workflow** 버튼
3. **upload_to_testflight**: ⚫ false (처음엔 false 로 archive 만 검증) → **Run workflow**
4. 약 10~15분 소요 — archive + IPA export 통과 확인
5. 통과하면 다음 시도 시 **upload_to_testflight = true** 로 실행 → TestFlight 자동 업로드

### 첫 시도 실패 시 흔한 원인
- `IOS_CERTIFICATE_PASSWORD` 잘못 입력 — `security import` 단계에서 실패
- `IOS_PROVISIONING_PROFILE_NAME` 이 profile 의 정확한 이름과 다름 — export 단계에서 실패
- profile 의 App ID 가 `com.sqldpass.app` 와 다름 — Distribution profile 다시 생성

---

## Secret 만료 / 재발급

| 항목 | 만료 |
|---|---|
| iOS Distribution 인증서 | 1년 |
| Provisioning Profile | 1년 (인증서 만료와 동기화) |
| API Key (.p8) | 영구 (revoke 안 하면) |
| APPLE_TEAM_ID | 영구 |

1년 후 STEP 2 + STEP 3 만 다시 진행. 단 새 .p12 + 새 profile 만들면 기존 secret 두 개 (`IOS_CERTIFICATE_P12_BASE64`, `IOS_PROVISIONING_PROFILE_BASE64`) 갱신만 하면 됨.
