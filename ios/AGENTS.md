# iOS Agent Guidelines

## Scope

`ios/` 디렉토리는 sqldpass 의 **SwiftUI 네이티브 iOS 앱**이다. 기존 `mobile/` (Capacitor 안드로이드) 와 별개로 동작한다.

- 언어: Swift 5.x, SwiftUI
- 최소 iOS: 17.0
- 프로젝트 생성: xcodegen (`ios/project.yml` → `ios/Sqldpass.xcodeproj`)
- 빌드 도구: Xcode 15.4+ (현재 16.2)
- 백엔드 API 베이스: Debug `http://localhost:8080`, Release `https://sqldpass.com`

## Working Directory

```bash
cd ios
```

iOS 작업은 macOS 셸에서만 가능하다. Windows/Linux 에서 본 디렉토리의 검증을 시도하지 말 것.

## Build & Verify

### 프로젝트 재생성 (project.yml 변경 시)

```bash
~/bin/xcodegen generate
sed -i '' 's/objectVersion = 77;/objectVersion = 56;/' Sqldpass.xcodeproj/project.pbxproj
```

`objectVersion` 패치는 Xcode 15.4 호환을 위해 필요 (xcodegen 기본은 77 = Xcode 16+ 포맷). Xcode 16.2 이상에서는 패치 안 해도 무방.

### 빌드 (필수 검증)

```bash
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build
```

성공 시 `** BUILD SUCCEEDED **` 출력.

### 시뮬레이터 실행 + 스크린샷

```bash
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/Sqldpass-* -name "Sqldpass.app" -type d | head -1)
xcrun simctl uninstall booted com.sqldpass.app
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted com.sqldpass.app
sleep 3
xcrun simctl io booted screenshot /tmp/sqldpass-<step>.png
```

## Layer Rules

```
App/          앱 엔트리, SessionGate, MainTabView
Core/
  Auth/       AuthStore(Keychain), Apple/Google AuthService, SessionGate UI
  Networking/ APIClient, APIError, APIEnvironment
  DesignSystem/ Color tokens, Spacing, Radius, Typography
Features/<X>/ 화면 단위 (View + ViewModel + 부속 컴포넌트)
Models/       백엔드 DTO 미러 Codable 구조체
Services/     API 호출 래퍼 (enum 정적 메서드)
Resources/    PrivacyInfo.xcprivacy 등
```

- 새 화면은 `Features/<도메인>/` 하위 폴더에 둔다.
- API 호출은 반드시 `Services/<X>Service.swift` 의 `enum` 정적 메서드를 거쳐 `APIClient.shared` 를 호출. ViewModel/View 가 `APIClient` 를 직접 호출하지 않는다.
- Codable 모델은 `Models/` 에만 둔다. View 안에서 Codable 구조체를 새로 선언하지 말 것.

## Design Tokens

`ios/Sqldpass/Core/DesignSystem/Color+Tokens.swift` 의 토큰만 사용. 직접 `Color(hex:)` 또는 `Color.green` 같은 시스템 색을 박지 말 것 — 라이트/다크 자동 적응을 깬다.

- 본문 컬러: `Color.appTextPrimary`, `Color.appTextMuted`, `Color.appTextSubtle`
- 표면: `Color.appPage`, `Color.appSurface`, `Color.appBorder`
- 브랜드: `Color.brandPrimary`
- 시맨틱: `Color.semanticDanger`, `Color.semanticSuccess`, `Color.semanticWarning`, `Color.semanticInfo`
- 시험 액센트: `Color.certSQLD`, `Color.certEngineerPractical`, ...

## 금지사항

- WKWebView, Capacitor, React Native 같은 하이브리드 도입 금지. 이유: 본 앱은 네이티브 SwiftUI 단일 스택.
- Apple 네이티브 외의 인증 SDK 추가 금지(현재 단계). 이유: Google Sign-In SDK 는 별도 phase 에서 도입 예정.
- `Color(hex:)` 같은 hex 직접 호출 금지(`UIColor+Hex.swift` 의 헬퍼는 디자인 토큰 정의에서만 사용). 이유: 라이트/다크 모드 분기와 디자인 토큰 통일을 위해.
- `print()` 디버그 로그 남기지 말 것. 이유: 로그 정책 미정 단계라 산출물 오염.

## Cross-cutting

API 계약을 신설/변경할 때는 백엔드(`backend/`) step 과 동일 phase 에 함께 묶고, iOS step 은 백엔드 step 완료 후 실행한다. iOS 단독 화면 작업(본 phase 같이)은 백엔드 변경 없이 기존 엔드포인트만 사용한다.
