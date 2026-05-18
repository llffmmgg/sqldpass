import Foundation

/// 백엔드 응답: GET /api/members/me
struct MemberMe: Codable, Equatable, Identifiable {
    let id: Int64
    let nickname: String
    let provider: String          // GOOGLE | APPLE
    /// ISO 8601 문자열. UI 표시 시 ISO8601DateFormatter 로 Date 변환.
    let createdAt: String
}
