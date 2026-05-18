import GoogleSignIn
import StoreKit
import SwiftUI

@main
struct SqldpassApp: App {
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
                        }
                    }
                }
        }
    }
}
