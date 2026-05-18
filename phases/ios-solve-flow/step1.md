# Step 1 — iOS AGENTS.md + Question/Bookmark Service + Solve 입력 모델

## Background

본 phase는 sqldpass 모노레포에 신규 도입된 `ios/` 디렉토리의 첫 번째 실 기능 작업이다. 풀이 화면을 만들기 전에:

1. `ios/AGENTS.md` 가 없어서 다른 step 에서 작업 디렉토리·검증 명령을 일관되게 참조할 기준이 없다. **본 step 에서 신설**.
2. Solve 화면이 `GET /api/questions/{id}` 으로 문항 상세를 가져오고, 풀이 중 북마크 토글(`POST/DELETE /api/bookmarks/{questionId}`)을 호출해야 한다. **QuestionService, BookmarkService** 추가.
3. 풀이 제출 입력 모델(`SolveService.SubmitRequest`)은 이미 존재하지만, ViewModel 이 유저 답안을 보관할 **메모리용 구조체**(`SolveAnswerEntry`)가 따로 필요하다.

이 step 산출물 위에 Step 2(ViewModel) 이후가 쌓인다.

## Workdir

```bash
ios/
```

(macOS 셸에서. Windows 에서 본 step 을 실행하려고 시도하지 말 것 — `xcodebuild` 가 없다.)

## Scope

| File | Change |
| --- | --- |
| `ios/AGENTS.md` | 신규 — iOS 작업 디렉토리/검증 명령/금지사항 정의 |
| `ios/Sqldpass/Services/QuestionService.swift` | 신규 — `GET /api/questions/{id}` 호출 래퍼 |
| `ios/Sqldpass/Services/BookmarkService.swift` | 신규 — `GET /api/bookmarks`, `POST/DELETE /api/bookmarks/{questionId}`, `GET /api/bookmarks/exists/{questionId}` |
| `ios/Sqldpass/Models/SolveAnswerEntry.swift` | 신규 — 풀이 ViewModel 메모리용 답안 구조체 |

## Implementation

### `ios/AGENTS.md`

```markdown
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
```

### `ios/Sqldpass/Services/QuestionService.swift`

```swift
import Foundation

enum QuestionService {
    /// GET /api/questions/{id} — 단일 문제 상세
    static func detail(id: Int64) async throws -> Question {
        try await APIClient.shared.get("/api/questions/\(id)")
    }

    /// GET /api/questions?subjectId=...&limit=... — 과목별 문제 목록 (옵션)
    static func list(subjectId: Int64? = nil, limit: Int? = nil) async throws -> [Question] {
        var query: [URLQueryItem] = []
        if let subjectId { query.append(.init(name: "subjectId", value: String(subjectId))) }
        if let limit { query.append(.init(name: "limit", value: String(limit))) }
        return try await APIClient.shared.get("/api/questions", query: query)
    }
}
```

### `ios/Sqldpass/Services/BookmarkService.swift`

```swift
import Foundation

enum BookmarkService {
    /// GET /api/bookmarks — 내 북마크 목록
    static func list() async throws -> [Bookmark] {
        try await APIClient.shared.get("/api/bookmarks")
    }

    /// GET /api/bookmarks/exists/{questionId} — 특정 문제 북마크 여부
    static func exists(questionId: Int64) async throws -> Bool {
        let response: BookmarkExists = try await APIClient.shared.get("/api/bookmarks/exists/\(questionId)")
        return response.exists
    }

    /// POST /api/bookmarks/{questionId} — 북마크 추가
    static func add(questionId: Int64) async throws {
        let _: Bookmark = try await APIClient.shared.post("/api/bookmarks/\(questionId)", body: EmptyBookmarkBody())
    }

    /// DELETE /api/bookmarks/{questionId} — 북마크 제거
    static func remove(questionId: Int64) async throws {
        try await APIClient.shared.delete("/api/bookmarks/\(questionId)")
    }
}

private struct EmptyBookmarkBody: Encodable {}
```

### `ios/Sqldpass/Models/SolveAnswerEntry.swift`

```swift
import Foundation

/// SolveViewModel 메모리 전용 답안 보관 구조체.
/// 제출 시 SolveService.SubmitRequest.Answer 로 매핑.
struct SolveAnswerEntry: Equatable {
    let questionId: Int64
    var chosenAnswer: String?
    var markedForReview: Bool

    init(questionId: Int64, chosenAnswer: String? = nil, markedForReview: Bool = false) {
        self.questionId = questionId
        self.chosenAnswer = chosenAnswer
        self.markedForReview = markedForReview
    }

    var toSubmitAnswer: SolveService.SubmitRequest.Answer {
        .init(questionId: questionId, chosenAnswer: chosenAnswer)
    }
}
```

## Validation

### 빌드 검증 (필수)

```bash
cd ios
~/bin/xcodegen generate
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

기대 출력: `** BUILD SUCCEEDED **`

빌드 실패 시 컴파일 에러 원인 (대부분 import 누락, 타입 불일치) 수정 후 재시도.

### 추가 검증

- `ios/AGENTS.md` 가 존재하고 내용이 본 step Implementation 과 동일한지 확인.
- `Services/` 폴더에 `QuestionService.swift`, `BookmarkService.swift` 가 추가됐는지.
- `Models/SolveAnswerEntry.swift` 가 추가됐는지.

## 금지사항

- `Models/SolveAnswerEntry.swift` 를 `Codable` 로 만들지 마라. 이유: 메모리 전용 ViewModel state 구조체이며 API 직렬화 대상 아님.
- BookmarkService.add 의 응답 타입을 Void 로 처리하지 마라. 이유: 백엔드가 `Bookmark` 객체 반환. 응답을 무시하더라도 디코딩은 통과해야 함.
- `~/Desktop/sqldpass/ios/Sqldpass/Models/Bookmark.swift` 의 `BookmarkExists` 구조체를 수정/이동하지 마라. 이미 정의돼있다 — BookmarkService 에서 그대로 import 해 사용.
