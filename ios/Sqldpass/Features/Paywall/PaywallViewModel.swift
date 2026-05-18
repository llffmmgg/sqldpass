import Foundation
import Observation
import StoreKit

@Observable
final class PaywallViewModel {
    private(set) var products: [PaymentProduct] = []
    private(set) var subscription: SubscriptionInfo?
    private(set) var isLoading = false
    private(set) var isPurchasing = false
    private(set) var errorMessage: String?
    private(set) var purchaseSuccess = false

    var hasActiveSubscription: Bool {
        subscription?.active ?? false
    }

    @MainActor
    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let productsTask = StoreKitService.shared.loadProducts()
            async let subscriptionTask = PaymentService.subscription()
            let (p, s) = try await (productsTask, subscriptionTask)
            products = p
            subscription = s
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    @MainActor
    func purchase(_ product: PaymentProduct) async {
        guard !isPurchasing else { return }
        isPurchasing = true
        errorMessage = nil
        do {
            let result = try await StoreKitService.shared.purchase(product)
            switch result {
            case .success(let jws, let productId, _):
                _ = try await PaymentService.verifyApple(
                    signedTransaction: jws,
                    productId: productId
                )
                purchaseSuccess = true
                subscription = try? await PaymentService.subscription()
            case .userCancelled:
                break
            case .pending:
                errorMessage = "결제 승인 대기 중입니다."
            }
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
        isPurchasing = false
    }

    @MainActor
    func restore() async {
        isPurchasing = true
        errorMessage = nil
        do {
            try await StoreKitService.shared.restore()
            subscription = try? await PaymentService.subscription()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
        isPurchasing = false
    }
}
