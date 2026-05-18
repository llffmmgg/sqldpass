# Step 1 — APIEnvironment 운영 URL 일원화

## Background

현재 `ios/Sqldpass/Core/Networking/APIEnvironment.swift` 의 Debug 분기는 `http://localhost:8080` 을 가리킨다. 호스트 맥에 백엔드가 안 떠있으면 connection refused. 사용자 결정에 따라 Debug/Release 모두 운영 `https://sqldpass.com` 으로 일원화한다.

로컬 dev 서버 사용은 `SQLDPASS_BACKEND_URL` 환경변수 override 로 유지 (SIMCTL_CHILD_SQLDPASS_BACKEND_URL=http://192.168.x.x:8080 식).

## Workdir

```bash
ios/
```

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Core/Networking/APIEnvironment.swift` | Debug/Release 분기 통합 — 둘 다 `https://sqldpass.com` 기본 |

## Implementation

기존:

```swift
enum APIEnvironment {
    static var current: URL {
        if let override = localBackendOverride, let url = URL(string: override) {
            return url
        }
        #if DEBUG
        return URL(string: "http://localhost:8080")!
        #else
        return URL(string: "https://sqldpass.com")!
        #endif
    }

    static var localBackendOverride: String? {
        ProcessInfo.processInfo.environment["SQLDPASS_BACKEND_URL"]
    }
}
```

→ 아래로 교체:

```swift
import Foundation

/// 백엔드 베이스 URL.
///
/// 기본값은 운영 `https://sqldpass.com` — Debug 빌드도 동일. 로컬 dev 서버를
/// 가리키려면 `SQLDPASS_BACKEND_URL` 환경변수에 URL 지정.
/// (예: `SIMCTL_CHILD_SQLDPASS_BACKEND_URL=http://192.168.0.42:8080`)
enum APIEnvironment {
    static var current: URL {
        if let override = localBackendOverride, let url = URL(string: override) {
            return url
        }
        return URL(string: "https://sqldpass.com")!
    }

    /// 로컬 dev 서버 override (시뮬레이터 launch 환경변수)
    static var localBackendOverride: String? {
        ProcessInfo.processInfo.environment["SQLDPASS_BACKEND_URL"]
    }
}
```

## Validation

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -5
```

기대: `** BUILD SUCCEEDED **`

## 금지사항

- HTTP 평문 URL 을 기본값으로 두지 마라. 이유: iOS ATS(App Transport Security)가 평문 HTTP 차단. 기본은 HTTPS, 로컬 override 시 사용자가 명시적으로 설정.
- localhost fallback 을 코드에 남기지 마라. 이유: 운영 일원화. 필요 시 env override.
