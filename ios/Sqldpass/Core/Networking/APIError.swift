import Foundation

/// 모든 네트워크/디코딩 에러의 통합 타입.
enum APIError: Error, LocalizedError, Equatable {
    /// URL 구성 실패 (잘못된 path / 쿼리)
    case invalidURL
    /// 네트워크 단절 / 타임아웃
    case transport(message: String)
    /// URLSession 측에서 동일 endpoint 중복 또는 명시적 cancel — 화면 에러로 표시하지 않는다.
    case cancelled
    /// 401 — 호출자는 토큰 폐기 후 로그인 화면으로 라우팅한다.
    case unauthorized
    /// 403 — 권한 부족 (PREMIUM 미구독 등)
    case forbidden
    /// 404 — 자원 없음
    case notFound
    /// 4xx (위 외)
    case clientError(status: Int, message: String?)
    /// 5xx — 서버 측 일시 장애
    case serverError(status: Int)
    /// JSON 디코딩 실패
    case decoding(message: String)
    /// 알 수 없음
    case unknown(message: String)

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "잘못된 요청 주소입니다."
        case .transport(let m): return "네트워크 연결을 확인해주세요. (\(m))"
        case .cancelled: return "요청이 취소되었습니다."
        case .unauthorized: return "로그인이 필요합니다."
        case .forbidden: return "이용 권한이 없습니다."
        case .notFound: return "요청한 정보를 찾을 수 없습니다."
        case .clientError(let s, let m): return m ?? "요청을 처리할 수 없습니다. (\(s))"
        case .serverError(let s): return "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요. (\(s))"
        case .decoding: return "응답 형식이 올바르지 않습니다."
        case .unknown(let m): return m
        }
    }
}
