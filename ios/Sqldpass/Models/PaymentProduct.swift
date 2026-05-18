import Foundation
import StoreKit

/// StoreKit 2 Product 의 View-friendly wrapper.
struct PaymentProduct: Identifiable, Equatable {
    let id: String              // productId (예: iap_one_month)
    let displayName: String
    let description: String
    let displayPrice: String    // 로케일 통화 포함 (예: ₩9,900)
    let underlying: Product

    init(_ product: Product) {
        self.id = product.id
        self.displayName = product.displayName
        self.description = product.description
        self.displayPrice = product.displayPrice
        self.underlying = product
    }

    static func == (lhs: PaymentProduct, rhs: PaymentProduct) -> Bool {
        lhs.id == rhs.id
    }
}
