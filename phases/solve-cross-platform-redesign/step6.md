# Step 6 — iOS 오프라인 답안 큐잉 신규 구현 (ios-offline-solve-queue)

## 배경

iOS 는 현재 오프라인 답안 큐가 **전혀 없다** — 네트워크 끊긴 상태로 옵션 선택 + "정답 확인" 시 `POST /api/solves` 가 throw 하면 사용자 답안이 그냥 사라진다(또는 alert 가 뜨고 사용자가 풀이를 멈추게 된다).

Android 와 동등한 큐잉을 iOS 에 신규 구현한다. 본 step 으로 양 플랫폼 풀이 화면이 **네트워크 끊김에 강한 동등 UX** 가 된다.

기술 선택: **SwiftData (`@Model`)**. iOS 17+ 이고 Core Data 보다 보일러플레이트가 훨씬 적음. Android Room 의 entity 와 1:1 대응.

## 작업 디렉터리

```bash
cd ios
```

macOS 셸에서만. Windows 에서 빌드 검증을 시도하지 말 것.

## 변경 대상

| 파일 | 변경 |
|---|---|
| `ios/Sqldpass/Core/Persistence/SqldpassModelContainer.swift` | 신규 — SwiftData `ModelContainer` 싱글톤. App 진입 시 1회 생성, `.modelContainer(...)` 로 환경에 주입 |
| `ios/Sqldpass/Core/Persistence/PendingSolve.swift` | 신규 — `@Model` 클래스. 필드 (Android PendingSolveEntity 와 동치) — `localId: Int64` (음수 -Date timestamp), `subjectId: Int64`, `mockExamId: Int64?`, `totalCount: Int`, `correctCount: Int`, `answersJSON: String` (Codable 배열을 String 으로), `clientSubmissionId: String` (UUID), `createdAt: Date`, `synced: Bool`, `serverSolveId: Int64?`, `syncedAt: Date?` |
| `ios/Sqldpass/Core/Persistence/SolveQueue.swift` | 신규 — actor. `enqueue(answer:)`, `pendingCount: Int`, `pendingCountAsync: AsyncSequence` (혹은 NotificationCenter post), `markSynced(localId:serverSolveId:)`, `listUnsynced() -> [PendingSolve]` |
| `ios/Sqldpass/Core/Networking/NetworkMonitor.swift` | 신규 — `NWPathMonitor` 래퍼. `@Observable`. `var isOnline: Bool` 노출. App 진입 시 start, 종료 시 cancel |
| `ios/Sqldpass/Core/Persistence/SolveSyncManager.swift` | 신규 — actor. `tryDrain()` async 메서드. 미동기화 row 를 createdAt asc 로 순회하며 `POST /api/solves` 재전송. 멱등키 `clientSubmissionId` 헤더 또는 body 필드로 (백엔드와 합의된 키 사용 — Android 측 확인 필요) |
| `ios/Sqldpass/Services/SolveService.swift` | `submit(_:)` 시그니처에 `clientSubmissionId: String` 인자 추가. 기본값은 `UUID().uuidString` |
| `ios/Sqldpass/Features/Solo/SoloSolveViewModel.swift` (step 4 산출물) | submit() 내부에서 `SolveQueue.shared.enqueue(...)` → `SolveSyncManager.shared.tryDrain()` 비차단 호출. 응답 실패와 무관하게 다음 문제로 진행 가능 |
| `ios/Sqldpass/Features/Solo/Components/OfflineQueueChip.swift` | 신규 — `SolveQueue.shared.pendingCount > 0` 일 때 노출. "오프라인 — N개 보관 중" + `cloud.slash` SF Symbol |
| `ios/Sqldpass/Features/Solo/SoloSolveView.swift` (step 4 산출물) | OfflineQueueChip 을 진행 바 바로 아래 좌측 정렬로 배치 |
| `ios/Sqldpass/App/SqldpassApp.swift` | App 진입 시 `NetworkMonitor.start()`. `isOnline` 변경 감지 → `Task { await SolveSyncManager.shared.tryDrain() }` |
| `ios/project.yml` | Core/Persistence 폴더 등록. SwiftData 는 표준 프레임워크라 추가 SPM 불필요 |

## 핵심 코드 스케치

### `PendingSolve.swift`

```swift
import SwiftData

@Model
final class PendingSolve {
    @Attribute(.unique) var localId: Int64
    var subjectId: Int64
    var mockExamId: Int64?
    var totalCount: Int
    var correctCount: Int
    /// Encoded JSON of [SubmitAnswerInput] (questionId, selectedOption, answerText)
    var answersJSON: String
    var clientSubmissionId: String
    var createdAt: Date
    var synced: Bool
    var serverSolveId: Int64?
    var syncedAt: Date?

    init(
        subjectId: Int64,
        mockExamId: Int64? = nil,
        totalCount: Int,
        correctCount: Int,
        answersJSON: String,
        clientSubmissionId: String = UUID().uuidString
    ) {
        self.localId = -Int64(Date().timeIntervalSince1970 * 1000)
        self.subjectId = subjectId
        self.mockExamId = mockExamId
        self.totalCount = totalCount
        self.correctCount = correctCount
        self.answersJSON = answersJSON
        self.clientSubmissionId = clientSubmissionId
        self.createdAt = Date()
        self.synced = false
        self.serverSolveId = nil
        self.syncedAt = nil
    }
}
```

### `SolveQueue.swift`

```swift
@MainActor
final class SolveQueue: ObservableObject {
    static let shared = SolveQueue(modelContext: SqldpassModelContainer.shared.mainContext)
    private let modelContext: ModelContext
    @Published private(set) var pendingCount: Int = 0

    init(modelContext: ModelContext) {
        self.modelContext = modelContext
        refreshCount()
    }

    func enqueue(_ answer: SubmitAnswerInput, subjectId: Int64) async throws -> PendingSolve {
        let entity = PendingSolve(...)
        modelContext.insert(entity)
        try modelContext.save()
        refreshCount()
        return entity
    }

    func listUnsynced() throws -> [PendingSolve] {
        let descriptor = FetchDescriptor<PendingSolve>(
            predicate: #Predicate { !$0.synced },
            sortBy: [SortDescriptor(\.createdAt, order: .forward)]
        )
        return try modelContext.fetch(descriptor)
    }

    func markSynced(_ entity: PendingSolve, serverSolveId: Int64) throws {
        entity.synced = true
        entity.serverSolveId = serverSolveId
        entity.syncedAt = Date()
        try modelContext.save()
        refreshCount()
    }

    private func refreshCount() { ... }
}
```

### `NetworkMonitor.swift`

```swift
import Network

@Observable
final class NetworkMonitor {
    static let shared = NetworkMonitor()
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "sqldpass.network-monitor")
    private(set) var isOnline: Bool = true

    func start() {
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in
                self?.isOnline = (path.status == .satisfied)
            }
        }
        monitor.start(queue: queue)
    }

    func cancel() { monitor.cancel() }
}
```

`SqldpassApp.swift` 의 `.onChange(of: networkMonitor.isOnline)` 에서 true 로 바뀔 때 `Task { await SolveSyncManager.shared.tryDrain() }`.

### `SolveSyncManager.swift`

```swift
actor SolveSyncManager {
    static let shared = SolveSyncManager()

    func tryDrain() async {
        let pending: [PendingSolve]
        do {
            pending = try await MainActor.run { try SolveQueue.shared.listUnsynced() }
        } catch { return }

        for entity in pending {
            do {
                let answers = try JSONDecoder().decode([SolveService.SubmitRequest.Answer].self, from: Data(entity.answersJSON.utf8))
                let request = SolveService.SubmitRequest(
                    subjectId: entity.subjectId,
                    mockExamId: entity.mockExamId,
                    answers: answers,
                    clientSubmissionId: entity.clientSubmissionId
                )
                let response = try await SolveService.submit(request)
                try await MainActor.run { try SolveQueue.shared.markSynced(entity, serverSolveId: response.id) }
            } catch {
                // 다음 row 로 — 영구 실패 처리는 별도 phase
                break
            }
        }
    }
}
```

## OfflineQueueChip 시각

step 5 의 Android 와 동일 컨벤션:

```swift
struct OfflineQueueChip: View {
    let count: Int
    var body: some View {
        if count > 0 {
            HStack(spacing: Spacing.xs) {
                Image(systemName: "cloud.slash")
                    .font(.system(size: 12, weight: .semibold))
                Text("오프라인 — \(count)개 보관 중")
                    .font(AppType.caption.weight(.semibold))
            }
            .foregroundStyle(Color.semanticWarning)
            .padding(.horizontal, Spacing.sm)
            .padding(.vertical, Spacing.xs)
            .background(Color.semanticWarning.opacity(0.12))
            .clipShape(Capsule())
        }
    }
}
```

## Acceptance Criteria

1. `xcodebuild ... build` → `** BUILD SUCCEEDED **`
2. SwiftData `ModelContainer` 가 `SqldpassApp` 진입 시 생성되고 `PendingSolve` 가 스키마에 등록됨.
3. 비행기 모드 ON → SoloSolveView 에서 옵션 선택 + "정답 확인" → 정답 공개 + "오프라인 — 1개 보관 중" 칩 노출 → 다음 문제 진행 가능 → 비행기 모드 OFF → 약 1~3초 안에 칩 자동 사라짐.
4. 백엔드 풀이 기록에 동일 `clientSubmissionId` 로 중복 row 없이 정확히 N개만 생성 (Android 측과 동일 멱등 동작).
5. 시뮬레이터 스크린샷 2장: 오프라인 상태(칩 표시) + 동기화 완료(칩 사라짐).

## 검증

```bash
cd ios
~/bin/xcodegen generate
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

비행기 모드 시뮬레이션:

```bash
# 시뮬레이터에서 직접 Settings → Airplane Mode 토글
# 또는 xcrun simctl status_bar 로 status 만 변경 후 실제 네트워크 차단을 위해
# 시뮬레이터 host 의 네트워크 인터페이스를 임시로 down
```

**Windows 환경에서 본 step 빌드 불가** — 코드 변경만 본 step 에서 수행하고, 빌드/스크린샷 검증은 macOS 에서 사용자가 실행. 결과를 step summary 에 기록.

## 금지 사항

- Core Data 또는 Realm 을 본 step 에서 도입하지 마라. 이유: SwiftData 가 iOS 17+ native 이며 충분. 외부 의존성 최소화.
- 백엔드 API 계약을 본 step 에서 변경하지 마라. 이유: 본 phase 는 클라이언트만. `clientSubmissionId` 가 백엔드에 이미 정의된 필드 이름인지 Android 측 `SolveService.submit` body 와 비교해서 동일한 키 사용.
- SolveSyncManager 가 백그라운드 drain 도중 실패한 row 를 무한 재시도하지 마라. 이유: 영구 실패(예: 410 GONE) 처리가 본 step 범위 아님. 현재 row 가 실패하면 그 시점 drain 중단, 다음 네트워크 이벤트 또는 앱 재시작 시 재시도.
- OfflineQueueChip 을 danger 색으로 표시하지 마라. 이유: step 5 와 동일 — "보관 중" 상태이지 "실패" 가 아님.
- `URLSession.shared` 의 background configuration 으로 sync 를 위임하지 마라. 이유: 본 phase 범위 초과. 단순 in-process drain 으로 시작.
- `@MainActor` 격리 없이 SwiftData ModelContext 를 actor 안에서 직접 변경하지 마라. 이유: SwiftData ModelContext 는 main thread 안전성이 요구된다 — `await MainActor.run { ... }` 로 감싸야 함.

## Status 규칙

- 성공: index.json step 6 `completed`, summary 에 빌드 결과 + 멱등성 검증 결과 + 스크린샷 경로.
- 실패: 3회 시도 후 `error` + `error_message`.
- Windows 환경에서 코드만 작성: `blocked` + `blocked_reason: "iOS 빌드/스크린샷 검증 및 비행기 모드 시나리오 검증을 위해 macOS 환경 필요"`.
