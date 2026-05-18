import Foundation
import StoreKit

/// StoreKit 2 통합. PaywallViewModel 이 호출.
///
/// - `loadProducts(ids:)` — App Store Connect 에 등록된 상품 메타 조회
/// - `purchase(_:)` — 구매 시작 → JWS 영수증 반환
/// - `restore()` — 기기에 활성 구독 있는지 동기화
/// - `Transaction.updates` 감시 → 자동 갱신 등 백그라운드 transaction
final class StoreKitService {
    static let shared = StoreKitService()

    private var updateTask: Task<Void, Never>?

    struct VerifiedTransaction: Sendable {
        let jws: String
        let productId: String
        let transaction: Transaction
    }

    /// 백엔드 등록된 productId 목록. 실제 App Store Connect SKU 와 일치해야 함.
    static let knownProductIds: [String] = [
        "iap_three_day",
        "iap_focus",
        "iap_one_month",
        "iap_unlimited"
    ]

    private init() {}

    func startListening(onNewTransaction: @escaping @Sendable (VerifiedTransaction) async -> Void) {
        updateTask?.cancel()
        updateTask = Task.detached {
            for await result in Transaction.updates {
                if case .verified(let transaction) = result {
                    await onNewTransaction(
                        VerifiedTransaction(
                            jws: result.jwsRepresentation,
                            productId: transaction.productID,
                            transaction: transaction
                        )
                    )
                }
            }
        }
    }

    func stopListening() {
        updateTask?.cancel()
        updateTask = nil
    }

    /// App Store Connect 의 상품 메타 조회.
    func loadProducts(ids: [String] = StoreKitService.knownProductIds) async throws -> [PaymentProduct] {
        let products = try await Product.products(for: ids)
        return products
            .sorted { $0.price < $1.price }
            .map(PaymentProduct.init)
    }

    /// 구매. 성공 시 JWS signedTransaction 문자열 반환.
    enum PurchaseResult {
        case success(jws: String, productId: String, transactionId: UInt64, transaction: Transaction)
        case userCancelled
        case pending
    }

    func purchase(_ product: PaymentProduct) async throws -> PurchaseResult {
        let result = try await product.underlying.purchase()
        switch result {
        case .success(let verification):
            switch verification {
            case .verified(let transaction):
                let jws = verification.jwsRepresentation
                let res = PurchaseResult.success(
                    jws: jws,
                    productId: transaction.productID,
                    transactionId: transaction.id,
                    transaction: transaction
                )
                return res
            case .unverified(_, let error):
                throw error
            }
        case .userCancelled:
            return .userCancelled
        case .pending:
            return .pending
        @unknown default:
            return .pending
        }
    }

    /// 기존 구매 복원 — 사용자가 다른 기기에서 구매했거나 앱 재설치 시.
    func restore() async throws {
        try await AppStore.sync()
    }

    /// 현재 활성 entitlement 의 JWS 반환 (있으면). 백엔드 재확인용.
    func currentEntitlementJWS() async -> String? {
        for await result in Transaction.currentEntitlements {
            if case .verified = result {
                return result.jwsRepresentation
            }
        }
        return nil
    }

    func currentEntitlements() async -> [VerifiedTransaction] {
        var entitlements: [VerifiedTransaction] = []
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result {
                entitlements.append(
                    VerifiedTransaction(
                        jws: result.jwsRepresentation,
                        productId: transaction.productID,
                        transaction: transaction
                    )
                )
            }
        }
        return entitlements
    }
}
