import Foundation
import StoreKit

/// StoreKit 2 Product 의 View-friendly wrapper.
struct PaymentProduct: Identifiable, Equatable {
    let id: String              // productId (예: iap_one_month)
    let displayName: String
    let description: String
    let displayPrice: String    // StoreKit 기본 — 사용자 지역 통화 (예: 시뮬레이터 US 이면 $9.99)
    let underlying: Product

    init(_ product: Product) {
        self.id = product.id
        self.displayName = product.displayName
        self.description = product.description
        self.displayPrice = product.displayPrice
        self.underlying = product
    }

    /// 한국식 ₩ 포맷 강제 표시. App Store Connect 의 KRW 가격이 base 라는 전제.
    /// 시뮬레이터 region 이 한국이 아니어도 항상 ₩9,900 같은 한국식 표기로 노출한다.
    var displayPriceKRW: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.currencyCode = "KRW"
        formatter.maximumFractionDigits = 0
        return formatter.string(from: underlying.price as NSDecimalNumber) ?? displayPrice
    }

    static func == (lhs: PaymentProduct, rhs: PaymentProduct) -> Bool {
        lhs.id == rhs.id
    }
}
