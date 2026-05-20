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

    /// 한국식 "9,900원" 포맷 강제 표시. App Store Connect 의 KRW 가격이 base 라는 전제.
    /// 시뮬레이터 region 무관 항상 한국 자연체 표기로 노출 — ₩ 기호 대신 "원" 글자가
    /// 한국어 사용자에게 직관적.
    var displayPriceKRW: String {
        let intValue = (underlying.price as NSDecimalNumber).intValue
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.locale = Locale(identifier: "ko_KR")
        let formatted = formatter.string(from: NSNumber(value: intValue)) ?? "\(intValue)"
        return "\(formatted)원"
    }

    static func == (lhs: PaymentProduct, rhs: PaymentProduct) -> Bool {
        lhs.id == rhs.id
    }
}
