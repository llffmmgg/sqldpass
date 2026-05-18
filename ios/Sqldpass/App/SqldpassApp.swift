import GoogleSignIn
import os
import StoreKit
import SwiftUI

@main
struct SqldpassApp: App {
    private static let storeKitLogger = Logger(subsystem: "com.sqldpass.app", category: "storekit")

    var body: some Scene {
        WindowGroup {
            RootView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
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
    }
}
