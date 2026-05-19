import Foundation

/// 백엔드 `POST /api/feedback` 호출 래퍼.
///
/// 백엔드 `CreateFeedbackRequest { type, questionId?, content, pageUrl? }` 와 1:1.
/// 응답 본문(`FeedbackResponse`)은 iOS 사용자 화면에서 즉시 필요한 정보가 없어
/// `EmptyResponse` 로 받지 않고 디코드하되 호출자에서 무시한다.
enum FeedbackService {
    enum FeedbackType: String, Codable, CaseIterable, Identifiable {
        case questionError = "QUESTION_ERROR"
        case bug = "BUG"
        case feature = "FEATURE"
        case other = "OTHER"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .questionError: return "문제 오류"
            case .bug: return "버그 신고"
            case .feature: return "기능 제안"
            case .other: return "기타 의견"
            }
        }
    }

    struct CreateRequest: Encodable {
        let type: FeedbackType
        let questionId: Int64?
        let content: String
        let pageUrl: String?
    }

    /// 백엔드 응답은 풍부하지만 모바일에서는 본문이 필요 없다 — 디코드 후 폐기.
    struct Created: Decodable {
        let id: Int64?
    }

    @discardableResult
    static func create(type: FeedbackType, questionId: Int64?, content: String, pageUrl: String? = nil) async throws -> Created {
        let body = CreateRequest(
            type: type,
            questionId: questionId,
            content: content,
            pageUrl: pageUrl
        )
        return try await APIClient.shared.post("/api/feedback", body: body)
    }
}
