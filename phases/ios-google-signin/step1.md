# Step 1 — iOS Google Sign-In Integration

## Background

`Core/Auth/GoogleAuthService.swift` 가 현재 placeholder 상태(에러 던짐). 실제 Google 로그인을 구현:

1. **SPM 의존성**: `GoogleSignIn-iOS` 8.x 추가 (`project.yml` packages)
2. **Info.plist**: URL types (REVERSED_CLIENT_ID 매핑) + `GIDClientID` 키
3. **GoogleAuthService 재작성**: `GIDSignIn.sharedInstance.signIn(withPresenting:)` → idToken 추출 → `POST /api/auth/login/google/idtoken` → AuthStore.signIn
4. **SqldpassApp 수정**: `.onOpenURL { GIDSignIn.sharedInstance.handle($0) }` 핸들러 추가
5. **xcodegen 재생성** + 첫 빌드 시 SPM resolve (수십 초 소요)

### Client ID (사용자 발급)

- iOS Client ID: `1026495161962-jtgphtjb90025fsmvb8e13tpnokv3edp.apps.googleusercontent.com`
- REVERSED_CLIENT_ID: `com.googleusercontent.apps.1026495161962-jtgphtjb90025fsmvb8e13tpnokv3edp`

## Workdir

```bash
ios/
```

## Scope

| File | Change |
| --- | --- |
| `ios/project.yml` | SPM 의존성(GoogleSignIn) 추가 + `info.path/properties` 도입 (기존 INFOPLIST_KEY_* 이관) |
| `ios/Sqldpass/Core/Auth/GoogleAuthService.swift` | 재작성 — placeholder 제거, 실 구현 |
| `ios/Sqldpass/App/SqldpassApp.swift` | `.onOpenURL { GIDSignIn.sharedInstance.handle($0) }` 핸들러 추가 |

## Implementation

### `ios/project.yml` (전체 교체)

기존 `INFOPLIST_KEY_*` 빌드 설정은 단순 key/value 만 지원해 `CFBundleURLTypes`(array of dict) 를 표현 못 한다. 별도 Info.plist 를 명시(`info.path`) 하고 모든 키를 `info.properties` 로 옮긴다.

```yaml
name: Sqldpass
options:
  bundleIdPrefix: com.sqldpass
  deploymentTarget:
    iOS: '17.0'
  developmentLanguage: ko
  generateEmptyDirectories: true
  createIntermediateGroups: true
  xcodeVersion: '15.4'

configs:
  Debug: debug
  Release: release

settings:
  base:
    SWIFT_VERSION: '5.0'
    ALWAYS_SEARCH_USER_PATHS: NO
    DEVELOPMENT_TEAM: ''
    CODE_SIGN_STYLE: Automatic
    ENABLE_USER_SCRIPT_SANDBOXING: YES

packages:
  GoogleSignIn:
    url: https://github.com/google/GoogleSignIn-iOS
    from: 8.0.0

targets:
  Sqldpass:
    type: application
    platform: iOS
    deploymentTarget: '17.0'
    sources:
      - path: Sqldpass
    dependencies:
      - package: GoogleSignIn
        product: GoogleSignIn
      - package: GoogleSignIn
        product: GoogleSignInSwift
    info:
      path: Sqldpass/Info.plist
      properties:
        CFBundleDisplayName: 문어CBT
        UILaunchScreen: {}
        UISupportedInterfaceOrientations:
          - UIInterfaceOrientationPortrait
        UISupportedInterfaceOrientations~ipad:
          - UIInterfaceOrientationPortrait
          - UIInterfaceOrientationPortraitUpsideDown
          - UIInterfaceOrientationLandscapeLeft
          - UIInterfaceOrientationLandscapeRight
        CFBundleURLTypes:
          - CFBundleTypeRole: Editor
            CFBundleURLSchemes:
              - com.googleusercontent.apps.1026495161962-jtgphtjb90025fsmvb8e13tpnokv3edp
        GIDClientID: 1026495161962-jtgphtjb90025fsmvb8e13tpnokv3edp.apps.googleusercontent.com
    settings:
      base:
        PRODUCT_NAME: Sqldpass
        PRODUCT_BUNDLE_IDENTIFIER: com.sqldpass.app
        ENABLE_PREVIEWS: YES
        MARKETING_VERSION: '1.0.0'
        CURRENT_PROJECT_VERSION: '1'
        TARGETED_DEVICE_FAMILY: '1'
```

변경 핵심:
- `packages.GoogleSignIn` 추가 (URL + from 8.0.0)
- `targets.Sqldpass.dependencies` 에 `GoogleSignIn`, `GoogleSignInSwift` product 두 개
- `info.path/properties` 도입 — `GENERATE_INFOPLIST_FILE: YES` 와 `INFOPLIST_KEY_*` 제거 (info.properties 가 대체)
- `CFBundleURLTypes` 와 `GIDClientID` 추가

### `ios/Sqldpass/Core/Auth/GoogleAuthService.swift` (재작성)

placeholder 통째로 교체.

```swift
import Foundation
import GoogleSignIn
import UIKit

/// Google Sign-In → 백엔드 `POST /api/auth/login/google/idtoken` 교환 후 AuthStore 갱신.
///
/// 호출자(AuthView)가 `await GoogleAuthService.signIn()` 으로 진입.
/// 백엔드는 idToken 의 audience 가 iOS Client ID 와 일치하는지 검증한다.
enum GoogleAuthService {
    static func signIn() async throws {
        let presenter = try await topViewController()
        let signInResult: GIDSignInResult
        do {
            signInResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
        } catch {
            // 사용자 취소는 별도 에러 코드 — 호출자가 UI 알림 안 띄우도록 그대로 throw.
            throw error
        }

        guard let idToken = signInResult.user.idToken?.tokenString else {
            throw APIError.unknown(message: "Google ID Token 을 받지 못했습니다.")
        }

        let request = GoogleIdTokenLoginRequest(idToken: idToken)
        let response: OAuthLoginResponse = try await APIClient.shared.post(
            "/api/auth/login/google/idtoken",
            body: request
        )

        await MainActor.run {
            AuthStore.shared.signIn(token: response.token, nickname: response.nickname)
        }
    }

    /// 현재 키 윈도우의 최상위 ViewController. presenting controller 로 사용.
    @MainActor
    private static func topViewController() throws -> UIViewController {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
            ?? UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        guard let window = scene?.windows.first(where: { $0.isKeyWindow }) ?? scene?.windows.first,
              var top = window.rootViewController else {
            throw APIError.unknown(message: "최상위 화면을 찾지 못했습니다.")
        }
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }
}
```

### `ios/Sqldpass/App/SqldpassApp.swift` (수정)

기존:

```swift
import SwiftUI

@main
struct SqldpassApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
```

→ Google Sign-In URL 핸들러 추가:

```swift
import GoogleSignIn
import SwiftUI

@main
struct SqldpassApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
```

## Validation

### 빌드 검증 (필수)

```bash
cd ios
~/bin/xcodegen generate
# objectVersion 56 패치 더이상 불필요 — Xcode 16.2 가 77 읽음. project.pbxproj 그대로 사용.
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

**첫 빌드는 SPM resolve 로 30 초~2 분 추가 소요**. `Sources/AppleProductTypes`, `Sources/GoogleSignIn` 등 GoogleSignIn 의 의존성(AppAuth, GTMSessionFetcher, GTMAppAuth) 도 함께 fetch.

기대: `** BUILD SUCCEEDED **`

### 검증 추가

- `Sqldpass.xcodeproj/project.pbxproj` 에 `GoogleSignIn` 패키지 참조가 포함됐는지 (`grep -c "GoogleSignIn" Sqldpass.xcodeproj/project.pbxproj` ≥ 1)
- 시뮬레이터 실행 → Google 버튼 탭 → SDK가 띄우는 Safari 브라우저 화면이 뜨면 통합 성공 (실제 로그인 검증은 Step 2 의 백엔드 audience 추가 이후)

## 금지사항

- `Info.plist` 를 손으로 직접 만들지 마라. 이유: `info.properties` 가 xcodegen 으로 자동 생성. 손으로 만들면 xcodegen 이 다음 generate 에서 덮어쓴다.
- `INFOPLIST_KEY_*` 빌드 설정을 `settings.base` 에 남기지 마라. 이유: `info.path` 가 명시되면 `GENERATE_INFOPLIST_FILE` 가 무시되고 `info.properties` 만 사용. `INFOPLIST_KEY_*` 는 dead code.
- iOS Client ID 를 코드 안에 하드코딩 하지 마라(`GIDConfiguration(clientID: "...")` 직접 호출 금지). 이유: `GIDClientID` Info.plist 키가 있으면 SDK 가 자동 로드. 두 곳에 두면 동기화 사고.
- `UIApplication.shared.windows` 의 `windows` property 는 iOS 15+ deprecated. 위 구현처럼 `connectedScenes` 경유. 이유: 멀티 윈도우 환경 대응.
- 사용자 취소(`GIDSignInError.canceled`) 를 일반 에러로 표시하지 마라. AuthView 가 이미 ASAuthorization 의 `.canceled` 와 동일하게 메시지 안 띄우도록 처리 — 호출자가 에러를 분류해 표시.
