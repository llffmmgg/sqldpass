import Foundation

enum PaymentService {
    /// GET /api/payment/subscription — 통합 entitlement 확인 (다른 플랫폼 결제 포함)
    static func subscription() async throws -> SubscriptionInfo {
        try await APIClient.shared.get("/api/payment/subscription")
    }

    /// POST /api/payment/apple/verify — 영수증 검증 + Subscription 발급
    struct AppleVerifyRequest: Encodable {
        let signedTransaction: String
        let productId: String
    }

    struct VerifyResult: Decodable {
        let paymentId: Int64
        let status: String
    }

    static func verifyApple(signedTransaction: String, productId: String) async throws -> VerifyResult {
        try await APIClient.shared.post(
            "/api/payment/apple/verify",
            body: AppleVerifyRequest(signedTransaction: signedTransaction, productId: productId)
        )
    }
}
