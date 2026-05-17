import Foundation

/// 백엔드 응답: GET /api/payment/subscription
struct SubscriptionInfo: Codable, Equatable {
    let active: Bool
    let provider: String?     // PORTONE | PLAY_BILLING | APP_STORE
    let productId: String?
    let expiresAt: String?
    let autoRenewing: Bool?
}

struct PaymentEligibility: Codable, Equatable {
    let eligible: Bool
    let reason: String?
}
