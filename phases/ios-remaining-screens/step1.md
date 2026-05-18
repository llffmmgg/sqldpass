# Step 1 — WrongAnswerService + Models 정정 + MemberService 확장

## Background

남은 5개 화면을 만들기 전에 백엔드 DTO 와 일치하지 않는 모델을 정정하고, 새 Service 를 추가한다.

### 발견된 모델 불일치

기존 `ios/Sqldpass/Models/WrongAnswer.swift` 의 `WrongAnswer` 구조체는 **실제 백엔드 DTO 와 다름**:

- 기존: `id, questionId, chosenAnswer, correctAnswer, retryCount, lastSolvedAt`
- 실제 (`backend/.../wronganswer/dto/WrongAnswerResponse.java`): `questionId, questionContent, subjectId, subjectName, wrongCount, lastWrongAt`

`WrongAnswerStats` 도 다름. 본 step 에서 정정한다.

### 추가 서비스

- `WrongAnswerService` — `/api/wrong-answers` 시리즈 호출
- `MemberService.updateNickname` — 닉네임 편집용 PATCH (Step 6 에서 사용)

## Workdir

```bash
ios/
```

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Models/WrongAnswer.swift` | 재작성 — 실 DTO 일치 + 추가 모델(Preview/Retry) |
| `ios/Sqldpass/Services/WrongAnswerService.swift` | 신규 — list/stats/preview/retry |
| `ios/Sqldpass/Services/MemberService.swift` | 수정 — updateNickname 추가 |

## Implementation

### `Models/WrongAnswer.swift` (재작성)

기존 `WrongAnswer/WrongAnswerStats/SubjectCount` 정의를 통째로 삭제하고 아래로 교체.

```swift
import Foundation

/// 백엔드 응답: GET /api/wrong-answers
struct WrongAnswer: Codable, Equatable, Hashable, Identifiable {
    let questionId: Int64
    let questionContent: String
    let subjectId: Int64
    let subjectName: String
    let wrongCount: Int
    let lastWrongAt: String

    var id: Int64 { questionId }
}

/// 백엔드 응답: GET /api/wrong-answers/stats
struct WrongAnswerStats: Codable, Equatable, Identifiable {
    let subjectId: Int64
    let subjectName: String
    let totalSolved: Int
    let wrongCount: Int
    /// 0~100 정수 (백엔드 계산)
    let wrongRate: Int

    var id: Int64 { subjectId }
}

/// 백엔드 응답: GET /api/wrong-answers/preview — 잠금 화면용 미리보기
struct WrongAnswerPreview: Codable, Equatable, Identifiable {
    let questionId: Int64
    let questionContent: String
    let subjectName: String

    var id: Int64 { questionId }
}

/// 백엔드 응답: POST /api/wrong-answers/{questionId}/retry
struct WrongAnswerRetryResult: Codable, Equatable {
    let correct: Bool
    /// MCQ 정답 번호 (1~4). 단답형은 nil
    let correctOption: Int?
    /// 단답/약술형 모범답안. MCQ는 nil
    let correctAnswer: String?
    let explanation: String?
}
```

### `Services/WrongAnswerService.swift` (신규)

```swift
import Foundation

enum WrongAnswerService {
    /// GET /api/wrong-answers — 오답노트 목록
    static func list() async throws -> [WrongAnswer] {
        try await APIClient.shared.get("/api/wrong-answers")
    }

    /// GET /api/wrong-answers/stats — 과목별 오답률 통계
    static func stats() async throws -> [WrongAnswerStats] {
        try await APIClient.shared.get("/api/wrong-answers/stats")
    }

    /// GET /api/wrong-answers/preview — 잠금 화면용 미리보기 (구독 없이도 호출)
    static func preview() async throws -> [WrongAnswerPreview] {
        try await APIClient.shared.get("/api/wrong-answers/preview")
    }

    /// POST /api/wrong-answers/{questionId}/retry — 다시 풀기
    /// chosenOption: MCQ 선택 번호 ("1"~"4") 또는 단답형 답안 텍스트
    static func retry(questionId: Int64, chosen: String) async throws -> WrongAnswerRetryResult {
        try await APIClient.shared.post(
            "/api/wrong-answers/\(questionId)/retry",
            body: RetryRequest(chosenAnswer: chosen)
        )
    }

    private struct RetryRequest: Encodable {
        let chosenAnswer: String
    }
}
```

### `Services/MemberService.swift` (수정)

기존 구현에서 `me()`, `deleteAccount()` 는 그대로 유지하고 `updateNickname` 만 추가한다.

기존 파일:
```swift
enum MemberService {
    /// GET /api/members/me — 현재 로그인 사용자 정보
    static func me() async throws -> MemberMe {
        try await APIClient.shared.get("/api/members/me")
    }

    /// DELETE /api/members/me — 계정 삭제 (App Store 필수)
    static func deleteAccount() async throws {
        try await APIClient.shared.delete("/api/members/me")
    }
}
```

→ 아래로 교체 (`updateNickname` 추가):

```swift
import Foundation

enum MemberService {
    /// GET /api/members/me — 현재 로그인 사용자 정보
    static func me() async throws -> MemberMe {
        try await APIClient.shared.get("/api/members/me")
    }

    /// PATCH /api/members/me/nickname — 닉네임 변경
    static func updateNickname(_ nickname: String) async throws -> MemberMe {
        try await APIClient.shared.send(
            path: "/api/members/me/nickname",
            method: "PATCH",
            body: UpdateNicknameRequest(nickname: nickname)
        )
    }

    /// DELETE /api/members/me — 계정 삭제 (App Store 필수)
    static func deleteAccount() async throws {
        try await APIClient.shared.delete("/api/members/me")
    }

    private struct UpdateNicknameRequest: Encodable {
        let nickname: String
    }
}
```

### `Core/Networking/APIClient.swift` (보조 — PATCH 지원)

`APIClient` 의 high-level helper 가 현재 `get/post/postVoid/delete` 만 있어서 `PATCH` 가 없다. 아래 메서드를 `APIClient` 클래스 안 (delete 메서드 아래) 에 추가한다.

```swift
func patch<R: Decodable, B: Encodable>(_ path: String, body: B) async throws -> R {
    try await send(path: path, method: .patch, body: body)
}
```

위 `MemberService.updateNickname` 의 `send(path:method:body:)` 호출을 다음으로 교체한다:

```swift
static func updateNickname(_ nickname: String) async throws -> MemberMe {
    try await APIClient.shared.patch(
        "/api/members/me/nickname",
        body: UpdateNicknameRequest(nickname: nickname)
    )
}
```

## Validation

### 빌드 검증 (필수)

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

기대: `** BUILD SUCCEEDED **`

### 정합성 검증

- `Models/WrongAnswer.swift` 의 `WrongAnswer` 가 `questionId/questionContent/subjectId/subjectName/wrongCount/lastWrongAt` 필드만 가지는지.
- `Services/WrongAnswerService.swift` 의 4 메서드 모두 정의됐는지.
- `APIClient` 에 `patch(_:body:)` 메서드가 추가됐는지.

## 금지사항

- 기존 `WrongAnswer` 구조체의 옛 필드(`chosenAnswer`, `correctAnswer`, `retryCount`, `lastSolvedAt`)를 호환 위해 남기지 마라. 이유: 실제 백엔드 응답과 다른 필드를 메모리에 두면 decoding 실패 + 혼동.
- `WrongAnswerStats` 의 `SubjectCount`(이전 잘못된 정의)를 어디서도 import 하지 마라 — 새 `WrongAnswerStats` 가 그 역할을 통합 대체.
- `APIClient.send(path:method:body:)` 를 외부에 노출하지 마라(`private` 유지). 이유: high-level helper(`get/post/patch/delete`) 만 외부 사용. 라이브러리 캡슐화.
- `destination` 을 `iPhone 15 Pro` 같이 구체적 이름으로 지정하지 마라. 이유: 현재 머신은 iOS 18.2 시뮬레이터 미설치라 구체 destination 매칭 실패. `generic/platform=iOS Simulator` 만 사용.
