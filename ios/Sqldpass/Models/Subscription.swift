import Foundation
import Observation

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

/// 활성 구독 상태의 single source of truth.
///
/// - 앱 부팅 직후(`SqldpassApp.task`) 1회 `refresh()` 로 채워진다 (로그인 상태일 때만).
/// - Paywall 결제/복원 직후 `refresh()` 가 다시 호출돼 전 화면이 자동으로 권한 변화를 반영.
/// - 로그아웃 시 `clear()` 로 메모리 상태 폐기.
///
/// 화면 단(VM)은 본 store 를 단일 진실 원천으로 참조한다 — 화면별 독립 fetch 금지.
@Observable
@MainActor
final class SubscriptionStore {
    static let shared = SubscriptionStore()

    private(set) var info: SubscriptionInfo?
    private(set) var isLoading: Bool = false
    private(set) var lastLoadedAt: Date?

    /// 활성 구독 여부 — 화면에서 권한 게이팅 분기에 사용.
    var isActive: Bool { info?.active ?? false }

    private init() {}

    /// 백엔드에서 활성 구독 정보를 가져와 store 에 채운다. 실패하면 info 를 nil 로 떨어뜨려
    /// "활성 상태가 모호한 채로 다른 화면에 권한 부여" 사고를 막는다.
    func refresh() async {
        isLoading = true
        defer { isLoading = false }
        do {
            info = try await PaymentService.subscription()
            lastLoadedAt = Date()
        } catch {
            info = nil
            lastLoadedAt = Date()
        }
    }

    /// 로그아웃·계정 삭제 시 호출. 다음 로그인 시 다시 refresh.
    func clear() {
        info = nil
        lastLoadedAt = nil
    }
}
