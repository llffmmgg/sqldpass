# Step 1 — iOS APIClient timeout

## 작업 디렉터리
`ios/`

## 배경 / Why
- 현재 `ios/Sqldpass/Core/Networking/APIClient.swift` 는 `URLSession.shared` 를 사용 (간접). URLSession.shared 의 기본 timeout 은 request 60s / resource 7 days — 모바일 네트워크 변동 상황에서 사용자가 1분 가까이 멈춰있는 듯 체감.
- 결제·인증 흐름은 특히 빠른 실패 후 retry/사용자 안내가 필요.

## 변경 대상

### `ios/Sqldpass/Core/Networking/APIClient.swift`
- `URLSession.shared` 직접 사용 부분을 다음으로 교체:
  ```swift
  private let session: URLSession = {
      let config = URLSessionConfiguration.default
      config.timeoutIntervalForRequest = 15        // connect + 첫 응답 대기
      config.timeoutIntervalForResource = 30       // 전체 다운로드 한도
      config.waitsForConnectivity = false          // 즉시 실패해 사용자에게 빠른 피드백
      return URLSession(configuration: config)
  }()
  ```
- 기존 `URLSession.shared.data(for:)` 호출들을 `session.data(for:)` 로 변경.

### `ios/Sqldpass/Core/Auth/AuthStore.swift`
- `refresh()` 메서드가 직접 URLSession 을 사용 중이라면 동일하게 timeout 설정 (단 timeout 을 살짝 더 길게 — 예: 20s — refresh 가 모바일 인증 흐름에서 중요해서). 또는 그대로 두고 사용자 노출 안 되는 path 라 60s 도 허용.
- 확인 후 결정.

## 작업 절차
1. APIClient.swift 전체 읽고 URLSession.shared 호출 위치 모두 찾기.
2. 위 패턴으로 교체.
3. AuthStore.swift 의 refresh 도 점검.
4. macOS 빌드 검증은 본 step 에서 미수행 (Windows). 사용자가 별도 검증 필요.

## 검증
- Windows 에서 빌드 불가. 사용자가 macOS 에서:
  ```bash
  cd ios
  xcodebuild -project Sqldpass.xcodeproj -scheme Sqldpass \\
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \\
    -configuration Debug build
  ```

## 금지사항
- 비즈니스 로직 변경 금지. 이유: 본 step 은 transport timeout 만.
- 전역 `URLSession.shared` 동작을 바꾸지 말 것. 이유: 다른 코드(SwiftUI AsyncImage 등) 가 사용 중일 수 있음 — 우리 APIClient 만 자체 session 사용.

## 산출물
- 수정 파일 목록 + 한 줄 요약.
- macOS 빌드 미수행 명시.
