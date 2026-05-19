import Foundation

/// 백엔드 응답: GET /api/payment/subscription
/// 백엔드 record(`SubscriptionResponse`) 와 1:1.
///
/// 활성 구독이 없으면 `inactive()` → `{ active: false, plan: nil, expiresAt: nil, ... false }`.
/// 활성이면 `plan` (예: THUNDER/FOCUS/PRO/UNLIMITED) 과 entitlement 플래그가 채워진다.
///
/// 백엔드는 `LocalDateTime` 을 ISO 문자열(타임존 없음 / KST)로 직렬화 — `String?` 로 받는다.
struct SubscriptionInfo: Codable, Equatable {
    let active: Bool
    let plan: String?
    let expiresAt: String?
    let removesAds: Bool?
    let allowsPdf: Bool?
    let hasLibraryAccess: Bool?
    let allowsPremium: Bool?
}

/// 결제 페이지 접근 가능 여부 (백엔드 화이트리스트 기반 토글)
struct PaymentEligibility: Codable, Equatable {
    let eligible: Bool
    let reason: String?
}

extension SubscriptionInfo {
    /// 상단 배지/카드에 노출할 라벨 — 활성이면 `plan` 또는 "PRO", 아니면 "FREE".
    var displayBadgeLabel: String {
        guard active else { return "FREE" }
        if let plan, !plan.isEmpty { return plan }
        return "PRO"
    }
}
