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

    /// 백엔드 `VerifyPaymentResult` record 와 1:1.
    /// `{ paymentId: String, amount: Int, productName: String, plan: String, expiresAt: String }`
    /// — `paymentId` 는 PG/Apple transactionId 기반 문자열. iOS 클라이언트는 본 응답을
    /// 직접 사용하지 않고 직후 `/api/payment/subscription` 을 재조회해 활성 상태를 반영한다.
    struct VerifyResult: Decodable {
        let paymentId: String
        let amount: Int?
        let productName: String?
        let plan: String?
        let expiresAt: String?
    }

    static func verifyApple(signedTransaction: String, productId: String) async throws -> VerifyResult {
        try await APIClient.shared.post(
            "/api/payment/apple/verify",
            body: AppleVerifyRequest(signedTransaction: signedTransaction, productId: productId)
        )
    }
}
