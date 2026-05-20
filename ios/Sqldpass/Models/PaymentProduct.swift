import Foundation
import StoreKit

/// StoreKit 2 Product 의 View-friendly wrapper.
struct PaymentProduct: Identifiable, Equatable {
    let id: String              // productId (예: iap_one_month)
    let displayName: String
    let description: String
    let displayPrice: String    // StoreKit priceFormatStyle 결과 — 스토어프론트 통화로 자동 포맷 (KR: "₩13,900", US: "$9.99")
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
