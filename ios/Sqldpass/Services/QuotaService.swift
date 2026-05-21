import Foundation

/// 무료 회원 일일 한도 진행 상태 조회.
///
/// - 진입 시 호출해 헤더 배지를 미리 표시한다.
/// - 자체 카운팅 절대 금지 — 서버 단일 진실 소스. 갱신은 reload 로만.
enum QuotaService {
    /// GET /api/quota — 활성 구독자는 `questionLimit/mockLimit = nil`.
    static func fetch() async throws -> Quota {
        try await APIClient.shared.get("/api/quota")
    }
}
