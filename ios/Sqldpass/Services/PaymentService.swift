import Foundation

enum PaymentService {
    /// GET /api/payment/subscription — 통합 entitlement 확인 (다른 플랫폼 결제 포함)
    static func subscription() async throws -> SubscriptionInfo {
        try await APIClient.shared.get("/api/payment/subscription")
    }

    /// GET /api/payment/eligibility — 결제 페이지 접근 가능 여부.
    /// 베타/리뷰어 화이트리스트 모드에서 결제 진입 전 사용자에게 즉시 차단을 안내하기 위해 사용.
    /// 정식 오픈 모드(`reviewer-nicknames=""`)에서는 항상 `eligible=true` 를 반환한다.
    static func eligibility() async throws -> PaymentEligibility {
        try await APIClient.shared.get("/api/payment/eligibility")
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
