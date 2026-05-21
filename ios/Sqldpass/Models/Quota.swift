import Foundation

/// 백엔드 응답: GET /api/quota
///
/// 무료 회원 일일 한도 진행 상태. 활성 구독자는 `questionLimit` / `mockLimit` 가
/// `nil` 로 응답되며 UI 는 배지를 숨긴다.
///
/// `resetAt` 은 백엔드의 KST naive `LocalDateTime` 직렬화 (`"2026-05-22T00:00:00"`).
/// 표시 시 `+09:00` 가정.
struct Quota: Codable, Equatable {
    let questionUsed: Int
    let questionLimit: Int?
    let mockUsed: Int
    let mockLimit: Int?
    let resetAt: String
}
