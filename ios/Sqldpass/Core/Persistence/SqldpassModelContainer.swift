import Foundation
import SwiftData

/// 앱 전역 SwiftData ModelContainer.
///
/// `SqldpassApp` 의 `WindowGroup` 에 `.modelContainer(SqldpassModelContainer.shared)` 로 attach.
/// `SolveQueue.shared` 와 `SolveSyncManager.shared` 가 이 컨테이너의 mainContext 를 사용.
@MainActor
enum SqldpassModelContainer {
    static let shared: ModelContainer = {
        do {
            return try ModelContainer(for: PendingSolve.self)
        } catch {
            fatalError("SwiftData ModelContainer 생성 실패: \(error)")
        }
    }()
}
