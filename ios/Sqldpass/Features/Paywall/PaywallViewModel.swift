import Foundation
import Observation
import StoreKit

@Observable
@MainActor
final class PaywallViewModel {
    private(set) var products: [PaymentProduct] = []
    private(set) var isLoading = false
    private(set) var isPurchasing = false
    private(set) var errorMessage: String?
    private(set) var purchaseSuccess = false

    private let subscriptionStore = SubscriptionStore.shared

    /// 구독 정보는 더 이상 VM 이 직접 보관하지 않는다 — 전역 SubscriptionStore 의 single source of truth.
    /// computed 로 노출하므로 기존 호출부(`viewModel.subscription`) 는 그대로 동작한다.
    var subscription: SubscriptionInfo? {
        subscriptionStore.info
    }

    var hasActiveSubscription: Bool {
        subscriptionStore.isActive
    }

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let productsTask = StoreKitService.shared.loadProducts()
            async let storeRefresh: Void = subscriptionStore.refresh()
            let p = try await productsTask
            _ = await storeRefresh
            products = p
            errorMessage = nil
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func purchase(_ product: PaymentProduct) async {
        guard !isPurchasing else { return }
        isPurchasing = true
        defer { isPurchasing = false }
        errorMessage = nil

        do {
            let result = try await StoreKitService.shared.purchase(product)
            switch result {
            case .success(let jws, let productId, _, let transaction):
                _ = try await PaymentService.verifyApple(
                    signedTransaction: jws,
                    productId: productId
                )
                await transaction.finish()
                purchaseSuccess = true
                await subscriptionStore.refresh()
            case .userCancelled:
                break
            case .pending:
                errorMessage = "결제 승인 대기 중입니다."
            }
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func restore() async {
        guard !isPurchasing else { return }
        isPurchasing = true
        defer { isPurchasing = false }
        errorMessage = nil

        do {
            try await StoreKitService.shared.restore()
            try await syncCurrentEntitlements()
            await subscriptionStore.refresh()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func dismissPurchaseSuccess() {
        purchaseSuccess = false
    }

    private func syncCurrentEntitlements() async throws {
        let entitlements = await StoreKitService.shared.currentEntitlements()
        for entitlement in entitlements {
            _ = try await PaymentService.verifyApple(
                signedTransaction: entitlement.jws,
                productId: entitlement.productId
            )
            await entitlement.transaction.finish()
        }
    }
}
