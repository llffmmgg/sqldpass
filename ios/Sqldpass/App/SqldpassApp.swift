import GoogleSignIn
import os
import StoreKit
import SwiftData
import SwiftUI

@main
struct SqldpassApp: App {
    private static let storeKitLogger = Logger(subsystem: "com.sqldpass.app", category: "storekit")

    /// 오프라인 풀이 큐의 네트워크 복귀 drain 을 위해 NetworkMonitor.isOnline 관찰.
    private let networkMonitor = NetworkMonitor.shared

    var body: some Scene {
        WindowGroup {
            RootView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
                .task {
                    networkMonitor.start()
                    // 부팅 즉시 한 번 시도 (앱 재시작 후 큐 남았을 수 있음).
                    await SolveSyncManager.shared.tryDrain()
                }
                .onChange(of: networkMonitor.isOnline) { _, isOnline in
                    if isOnline {
                        Task { await SolveSyncManager.shared.tryDrain() }
                    }
                }
                .task {
                    StoreKitService.shared.startListening { event in
                        do {
                            _ = try await PaymentService.verifyApple(
                                signedTransaction: event.jws,
                                productId: event.productId
                            )
                            await event.transaction.finish()
                        } catch {
                            // 영수증 검증 실패는 백엔드/네트워크 이슈일 가능성. transaction 은
                            // finish 하지 않고 두면 다음 Transaction.updates 에서 다시 들어온다.
                            Self.storeKitLogger.error(
                                "verifyApple failed for productId=\(event.productId, privacy: .public) txn=\(event.transaction.id, privacy: .public) error=\(String(describing: error), privacy: .public)"
                            )
                        }
                    }
                }
        }
        .modelContainer(SqldpassModelContainer.shared)
    }
}
