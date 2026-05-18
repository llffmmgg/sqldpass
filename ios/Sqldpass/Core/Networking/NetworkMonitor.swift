import Foundation
import Network
import Observation

/// NWPathMonitor 래퍼 — `isOnline` 을 SwiftUI 가 관찰.
///
/// 앱 진입 시 `start()`, 종료 시 `cancel()`. 변화 시 `SolveSyncManager.tryDrain()` 트리거에 사용.
@MainActor
@Observable
final class NetworkMonitor {
    static let shared = NetworkMonitor()

    private(set) var isOnline: Bool = true
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.sqldpass.network-monitor")
    private var started = false

    private init() {}

    func start() {
        guard !started else { return }
        started = true
        monitor.pathUpdateHandler = { [weak self] path in
            let online = path.status == .satisfied
            Task { @MainActor [weak self] in
                self?.isOnline = online
            }
        }
        monitor.start(queue: queue)
    }

    func cancel() {
        guard started else { return }
        started = false
        monitor.cancel()
    }
}
